package com.digital.booking.service;

import com.digital.booking.domain.Booking;
import com.digital.booking.domain.BookingStatus;
import com.digital.booking.domain.ShowSeat;
import com.digital.booking.dto.BookTicketRequest;
import com.digital.booking.dto.PaymentResponse;
import com.digital.booking.exception.BookingNotFoundException;
import com.digital.booking.exception.InvalidBookingStateException;
import com.digital.booking.exception.InventoryUpdateException;
import com.digital.booking.exception.PaymentFailedException;
import com.digital.booking.exception.SeatsUnavailableException;
import com.digital.booking.repository.BookingRepository;
import com.digital.booking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingTransactionService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final BookingRepository bookingRepository;
    private final ShowSeatRepository seatRepository;
    private final OutboxService outboxService;
    private final Clock clock; // expose a @Bean Clock.systemUTC()

    @Transactional
    public Booking createBookingAndHoldSeats(String userId, BookTicketRequest request) {
        List<String> ids = request.seatIds().stream().distinct().toList();
        if (ids.isEmpty()) throw new SeatsUnavailableException("No seats selected");
        if (ids.size() != request.seatIds().size()) throw new SeatsUnavailableException("Duplicate seats");

        Instant now = clock.instant();

        // Fast-fail pre-check (also gives us prices). The atomic update below is the real guard.
        // isAvailable(now) treats a lapsed hold as free, matching holdAvailableSeats.
        List<ShowSeat> seats = seatRepository.findAllByShowIdAndSeatIdIn(request.showId(), ids);
        if (seats.size() != ids.size() || seats.stream().anyMatch(s -> !s.isAvailable(now)))
            throw new SeatsUnavailableException("Seats unavailable");

        BigDecimal amount = seats.stream().map(ShowSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        Booking booking = bookingRepository.save(Booking.builder()
                .userId(userId)
                .showId(request.showId())
                .idempotencyKey(request.idempotencyKey())
                .seatCount(ids.size())
                .totalAmount(amount)
                .status(BookingStatus.PENDING_PAYMENT)
                .build());

        int count = seatRepository.holdAvailableSeats(
                request.showId(), ids, booking.getId(), now, now.plus(HOLD_DURATION));
        if (count != ids.size()) {
            // Throwing rolls back the booking insert and any partial holds.
            throw new SeatsUnavailableException("Concurrent seat allocation conflict");
        }
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

        // Only succeeds for seats still HELD by this booking with an unexpired hold.
        int count = seatRepository.confirmHeldSeats(bookingId, clock.instant());
        if (count != booking.getSeatCount()) {
            // Payment succeeded but the hold lapsed or was lost: caller should refund / mark recovery.
            throw new InventoryUpdateException("Could not confirm every held seat");
        }

        // The repository update uses clearAutomatically, which detaches 'booking',
        // so save() is required for these changes to be persisted.
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(payment.paymentId());
        Booking saved = bookingRepository.save(booking);
        outboxService.saveBookingConfirmedEvent(saved);
        return saved;
    }

    @Transactional
    public void failBookingAndReleaseSeats(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) return;

        // Release is idempotent: the sweeper may already have freed lapsed holds, or another
        // booking may have taken them, so a lower count is expected and must not block FAILED.
        int count = seatRepository.releaseHeldSeats(bookingId);
        if (count != booking.getSeatCount()) {
            log.info("Booking {} released {} of {} seats (holds already expired or reassigned)",
                    bookingId, count, booking.getSeatCount());
        }

        booking.setStatus(BookingStatus.FAILED);
        bookingRepository.save(booking); // booking was detached by clearAutomatically
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