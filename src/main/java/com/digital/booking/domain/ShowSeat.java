package com.digital.booking.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="show_seat", uniqueConstraints=@UniqueConstraint(name="uk_show_seat", columnNames={"show_id","seat_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ShowSeat {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="show_id", nullable=false) private String showId;
    @Column(name="seat_id", nullable=false) private String seatId;
    @Column(nullable=false, precision=10, scale=2) private BigDecimal price;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private SeatStatus status;
    @Column(name="booking_id") private Long bookingId;
    @Column(name="hold_expires_at") private LocalDateTime holdExpiresAt;
}
