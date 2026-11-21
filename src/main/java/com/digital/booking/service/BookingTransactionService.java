package com.digital.booking.service;
import com.digital.booking.domain.Booking;
import com.digital.booking.domain.BookingStatus;
import com.digital.booking.domain.SeatStatus;
import com.digital.booking.domain.ShowSeat;
import com.digital.booking.dto.BookTicketRequest;
import com.digital.booking.dto.PaymentResponse;
import com.digital.booking.exception.*;
import com.digital.booking.repository.BookingRepository;
import com.digital.booking.repository.ShowSeatRepository;
import com.digital.booking.domain.*;
import com.digital.booking.dto.*;
import com.digital.booking.exception.*;
import com.digital.booking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class BookingTransactionService {

    private final BookingRepository bookingRepository;
    private final ShowSeatRepository seatRepository;
    private final OutboxService outboxService;

    @Transactional
    public Booking createBookingAndHoldSeats(String userId, BookTicketRequest request) {
        List<String> ids = request.seatIds().stream().distinct().toList();
        if (ids.size() != request.seatIds().size()) throw new SeatsUnavailableException("Duplicate seats");

        List<ShowSeat> seats = seatRepository.findAllByShowIdAndSeatIdIn(request.showId(), ids);
        if (seats.size() != ids.size() || seats.stream().anyMatch(s -> s.getStatus() != SeatStatus.AVAILABLE))
            throw new SeatsUnavailableException("Seats unavailable");

        BigDecimal amount = seats.stream().map(ShowSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        Booking booking = bookingRepository.save(Booking.builder().userId(userId).showId(request.showId())
            .idempotencyKey(request.idempotencyKey()).seatCount(ids.size()).totalAmount(amount)
            .status(BookingStatus.PENDING_PAYMENT).build());

        int count = seatRepository.holdAvailableSeats(request.showId(), ids, booking.getId(),
            LocalDateTime.now().plusMinutes(5), SeatStatus.HELD, SeatStatus.AVAILABLE);
        if (count != ids.size()) throw new SeatsUnavailableException("Concurrent seat allocation conflict");
        return booking;
    }

    @Transactional
    public Booking confirmBookingAndSaveOutbox(Long bookingId, PaymentResponse payment) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT)
            throw new InvalidBookingStateException("Booking is not pending payment");
        if (payment == null || !payment.successful() || payment.paymentId() == null)
            throw new PaymentFailedException("Payment authorization not successful");

        int count = seatRepository.confirmHeldSeats(bookingId, SeatStatus.BOOKED, SeatStatus.HELD);
        if (count != booking.getSeatCount()) throw new InventoryUpdateException("Could not confirm every held seat");

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(payment.paymentId());
        outboxService.saveBookingConfirmedEvent(booking);
        return booking;
    }

    @Transactional
    public void failBookingAndReleaseSeats(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
            .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) return;

        int count = seatRepository.releaseHeldSeats(bookingId, SeatStatus.AVAILABLE, SeatStatus.HELD);
        if (count != booking.getSeatCount()) throw new InventoryUpdateException("Could not release every held seat");
        booking.setStatus(BookingStatus.FAILED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking markRecoveryRequired(Long bookingId) {
        Booking b = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking " + bookingId));
        if (b.getStatus() == BookingStatus.PENDING_PAYMENT) {
            b.setStatus(BookingStatus.RECOVERY_REQUIRED);
        }
        return b;
    }
}
