package com.digital.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AddMovieRequest(

        @NotBlank
        String title,

        @NotBlank
        String genre,

        @NotBlank
        String language,

        @NotNull
        @Positive
        Integer durationMinutes,

        LocalDate releaseDate

) {}