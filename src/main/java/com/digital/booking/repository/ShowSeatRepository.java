package com.digital.booking.repository;

import com.digital.booking.domain.SeatStatus;
import com.digital.booking.domain.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findAllByShowIdAndSeatIdIn(String showId, Collection<String> seatIds);

    List<ShowSeat> findAllByBookingId(Long bookingId);

    /**
     * Atomic compare-and-set: only AVAILABLE seats are held.
     * Caller MUST compare the returned count with seatIds.size() and roll back if it is lower.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShowSeat s
               set s.status = :held, s.bookingId = :bookingId, s.holdExpiresAt = :expiry
             where s.showId = :showId
               and s.seatId in :seatIds
               and s.status = :available
            """)
    int holdAvailableSeats(@Param("showId") String showId,
                           @Param("seatIds") Collection<String> seatIds,
                           @Param("bookingId") Long bookingId,
                           @Param("expiry") LocalDateTime expiry,
                           @Param("held") SeatStatus held,
                           @Param("available") SeatStatus available);

    /** HELD -> BOOKED for this booking. A count lower than expected means the hold was lost. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShowSeat s
               set s.status = :booked, s.holdExpiresAt = null
             where s.bookingId = :bookingId
               and s.status = :held
            """)
    int confirmHeldSeats(@Param("bookingId") Long bookingId,
                         @Param("booked") SeatStatus booked,
                         @Param("held") SeatStatus held);

    /** HELD -> AVAILABLE for this booking (payment failed or hold expired). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ShowSeat s
               set s.status = :available, s.bookingId = null, s.holdExpiresAt = null
             where s.bookingId = :bookingId
               and s.status = :held
            """)
    int releaseHeldSeats(@Param("bookingId") Long bookingId,
                         @Param("available") SeatStatus available,
                         @Param("held") SeatStatus held);
}