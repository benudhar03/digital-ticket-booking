package com.digital.booking.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="outbox_event")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OutboxEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="aggregate_type", nullable=false) private String aggregateType;
    @Column(name="aggregate_id", nullable=false) private String aggregateId;
    @Column(name="event_type", nullable=false) private String eventType;
    @Column(nullable=false, columnDefinition="text") private String payload;
    @Column(nullable=false) private boolean published;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @PrePersist void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
