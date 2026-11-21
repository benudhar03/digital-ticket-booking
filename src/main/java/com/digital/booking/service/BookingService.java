package com.digital.booking.service;

import com.digital.booking.domain.Booking;
import com.digital.booking.dto.BookTicketRequest;
import com.digital.booking.dto.BookTicketResponse;
import com.digital.booking.dto.PaymentResponse;
import com.digital.booking.dto.SeatLockResponse;
import com.digital.booking.exception.PaymentFailedException;
import com.digital.booking.exception.SeatsUnavailableException;
import com.digital.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates the booking flow. Deliberately NOT @Transactional: the payment call
 * must run outside any DB transaction, and each DB step runs in its own short
 * transaction inside BookingTransactionService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final BookingTransactionService transactionService;

    public BookTicketResponse bookTicket(String userId, BookTicketRequest request) {

        Optional<Booking> existing =
                bookingRepository.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey());
        if (existing.isPresent()) {
            return BookTicketResponse.from(existing.get());
        }

        inventoryService.validateShowAndSeats(request);
        SeatLockResponse lock = inventoryService.acquireLock(request);
        if (!lock.acquired()) {
            throw new SeatsUnavailableException("Seats are currently being selected");
        }
        try {
            Booking booking;
            try {
                booking = transactionService.createBookingAndHoldSeats(userId, request);
            } catch (DataIntegrityViolationException duplicate) {
                return bookingRepository.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey())
                        .map(BookTicketResponse::from)
                        .orElseThrow(() -> duplicate);
            }
            PaymentResponse payment;
            try {
                payment = paymentService.authorizePayment(booking);
            } catch (RuntimeException ex) {
                // Timeout or network error: the customer may or may not have been charged.
                log.error("Payment outcome unknown for booking {}", booking.getId(), ex);
                return BookTicketResponse.from(transactionService.markRecoveryRequired(booking.getId()));
            }

            if (!payment.successful()) {
                releaseSeatsQuietly(booking.getId());
                throw new PaymentFailedException(payment.message());
            }

            try {
                Booking confirmed = transactionService.confirmBookingAndSaveOutbox(booking.getId(), payment);
                return BookTicketResponse.from(confirmed);
            } catch (RuntimeException ex) {
                // Customer was charged but we could not confirm: a retry job must finish this.
                log.error("Payment succeeded but confirmation failed for booking {}", booking.getId(), ex);
                return BookTicketResponse.from(transactionService.markRecoveryRequired(booking.getId()));
            }
        } finally {
            releaseLockQuietly(lock);
        }
    }

    private void releaseSeatsQuietly(Long bookingId) {
        try {
            transactionService.failBookingAndReleaseSeats(bookingId);
        } catch (RuntimeException ex) {
            // The expiry job will clean this up; do not hide the real payment failure.
            log.error("Failed to release seats for booking {}", bookingId, ex);
        }
    }

    private void releaseLockQuietly(SeatLockResponse lock) {
        try {
            inventoryService.releaseLock(lock);
        } catch (RuntimeException ex) {
            log.warn("Failed to release seat lock; it will expire on its own", ex);
        }
    }
}