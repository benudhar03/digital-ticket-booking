package com.digital.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MovieResponse(
        UUID movieId,
        String title,
        String genre,
        String language,
        Integer durationMinutes,
        LocalDate releaseDate
) {}