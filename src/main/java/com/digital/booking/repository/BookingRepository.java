package com.digital.booking.repository;
import com.digital.booking.domain.Booking;
import com.digital.booking.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** Expiry job: stale bookings still waiting for payment, oldest batch first. */
    List<Booking> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            BookingStatus status, Instant cutoff);

    /** Recovery job: paid bookings whose inventory update must be retried. */
    List<Booking> findTop100ByStatusOrderByCreatedAtAsc(BookingStatus status);
}
