package com.digital.booking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single seat for a single show. Lifecycle: AVAILABLE -> HELD -> BOOKED,
 * with HELD -> AVAILABLE on release/expiry.
 * Assumes SeatStatus { AVAILABLE, HELD, BOOKED }.
 */
@Entity
@Table(
        name = "show_seat",
        uniqueConstraints = @UniqueConstraint(name = "uk_show_seat", columnNames = {"show_id", "seat_id"}),
        indexes = {
                // used by the expiry sweeper: WHERE status = 'HELD' AND hold_expires_at < now
                @Index(name = "idx_show_seat_status_hold", columnList = "status, hold_expires_at"),
                // used to find all seats for a booking (confirm / cancel)
                @Index(name = "idx_show_seat_booking", columnList = "booking_id"),
                // used to list a show's seat map
                @Index(name = "idx_show_seat_show", columnList = "show_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optimistic lock: prevents two users from holding/booking the same seat concurrently. */
    @Version
    private Long version;

    @Column(name = "show_id", nullable = false, updatable = false)
    private String showId;

    @Column(name = "seat_id", nullable = false, updatable = false)
    private String seatId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;


    public boolean isHoldExpired(Instant now) {
        return status == SeatStatus.HELD && holdExpiresAt != null && !holdExpiresAt.isAfter(now);
    }

    /** A seat can be taken if it is free, or if a previous hold has lapsed. */
    public boolean isAvailable(Instant now) {
        return status == SeatStatus.AVAILABLE || isHoldExpired(now);
    }

    public void hold(Long bookingId, Instant now, Instant expiresAt) {
        if (!isAvailable(now)) {
            throw new IllegalStateException("Seat %s for show %s is not available".formatted(seatId, showId));
        }
        this.status = SeatStatus.HELD;
        this.bookingId = bookingId;
        this.holdExpiresAt = expiresAt;
    }

    public void confirm(Long bookingId, Instant now) {
        if (status != SeatStatus.HELD || !bookingId.equals(this.bookingId) || isHoldExpired(now)) {
            throw new IllegalStateException("Seat %s has no active hold for booking %d".formatted(seatId, bookingId));
        }
        this.status = SeatStatus.BOOKED;
        this.holdExpiresAt = null;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.bookingId = null;
        this.holdExpiresAt = null;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShowSeat other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}