package com.digital.booking.repository;

import com.digital.booking.domain.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findAllByShowIdAndSeatIdIn(String showId, Collection<String> seatIds);

    List<ShowSeat> findAllByBookingId(Long bookingId);

    long countByBookingIdAndStatus(Long bookingId, com.digital.booking.domain.SeatStatus status);

    /** Seat map for a show, ordered for stable rendering. */
    List<ShowSeat> findAllByShowIdOrderBySeatIdAsc(String showId);

    /**
     * Pessimistic variant for very hot shows: locks rows so concurrent requests queue
     * instead of failing. Prefer the atomic updates below for normal traffic.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.showId = :showId and s.seatId in :seatIds order by s.seatId")
    List<ShowSeat> lockByShowIdAndSeatIds(@Param("showId") String showId,
                                          @Param("seatIds") Collection<String> seatIds);

    /**
     * Atomic compare-and-set. Takes seats that are AVAILABLE, or HELD with a lapsed hold,
     * so the system stays correct even if the expiry sweeper is late.
     * Caller MUST compare the returned count with seatIds.size() and roll back if lower.
     * Callers must not pass an empty collection.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShowSeat s
               set s.status = com.digital.booking.domain.SeatStatus.HELD,
                   s.bookingId = :bookingId,
                   s.holdExpiresAt = :expiry,
                   s.version = s.version + 1
             where s.showId = :showId
               and s.seatId in :seatIds
               and (s.status = com.digital.booking.domain.SeatStatus.AVAILABLE
                    or (s.status = com.digital.booking.domain.SeatStatus.HELD
                        and s.holdExpiresAt <= :now))
            """)
    int holdAvailableSeats(@Param("showId") String showId,
                           @Param("seatIds") Collection<String> seatIds,
                           @Param("bookingId") Long bookingId,
                           @Param("now") Instant now,
                           @Param("expiry") Instant expiry);

    /**
     * HELD -> BOOKED for this booking, only while the hold is still valid.
     * A count lower than expected means the hold lapsed or was lost: refund / fail the booking.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShowSeat s
               set s.status = com.digital.booking.domain.SeatStatus.BOOKED,
                   s.holdExpiresAt = null,
                   s.version = s.version + 1
             where s.bookingId = :bookingId
               and s.status = com.digital.booking.domain.SeatStatus.HELD
               and s.holdExpiresAt > :now
            """)
    int confirmHeldSeats(@Param("bookingId") Long bookingId, @Param("now") Instant now);

    /** HELD -> AVAILABLE for this booking (payment failed or user cancelled). */
    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShowSeat s
               set s.status = com.digital.booking.domain.SeatStatus.AVAILABLE,
                   s.bookingId = null,
                   s.holdExpiresAt = null,
                   s.version = s.version + 1
             where s.bookingId = :bookingId
               and s.status = com.digital.booking.domain.SeatStatus.HELD
            """)
    int releaseHeldSeats(@Param("bookingId") Long bookingId);

    /** Sweeper: frees every lapsed hold in one statement. Run on a schedule. */
    @Transactional(propagation = Propagation.MANDATORY)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShowSeat s
               set s.status = com.digital.booking.domain.SeatStatus.AVAILABLE,
                   s.bookingId = null,
                   s.holdExpiresAt = null,
                   s.version = s.version + 1
             where s.status = com.digital.booking.domain.SeatStatus.HELD
               and s.holdExpiresAt <= :now
            """)
    int releaseExpiredHolds(@Param("now") Instant now);
}