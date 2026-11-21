
package com.digital.booking.service;

import com.digital.booking.domain.Booking;
import com.digital.booking.domain.OutboxEvent;
import com.digital.booking.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void saveBookingConfirmedEvent(Booking booking) {

        try {
            Map<String, Object> payload = Map.of(
                    "bookingId", booking.getId(),
                    "showId", booking.getShowId(),
                    "status", "CONFIRMED"
            );

            String eventPayload = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("BOOKING")
                    .aggregateId(booking.getId().toString())
                    .eventType("BOOKING_CONFIRMED")
                    .payload(eventPayload)
                    .published(false)
                    .build();

            repository.save(event);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize booking confirmed event",
                    exception
            );
        }
    }
}
