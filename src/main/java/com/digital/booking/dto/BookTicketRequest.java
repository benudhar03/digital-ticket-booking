package com.digital.booking.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record BookTicketRequest(
        @NotBlank String showId,
        @NotEmpty List<@NotBlank String> seatIds,
        @NotBlank String idempotencyKey) {}
