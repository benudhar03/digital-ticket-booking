package com.digital.booking.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public class MovieProcedureRepository {

    private static final String ADD_MOVIE_SQL = "CALL add_movie(?, ?, ?, ?, ?, ?)";

    private static final int[] ADD_MOVIE_TYPES = {
            Types.OTHER,    // movie_id (uuid)
            Types.VARCHAR,  // title
            Types.VARCHAR,  // genre
            Types.VARCHAR,  // language
            Types.INTEGER,  // duration_minutes
            Types.DATE      // release_date (nullable)
    };

    private final JdbcTemplate jdbcTemplate;

    public MovieProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addMovie(UUID movieId, String title, String genre,
                         String language, int durationMinutes, LocalDate releaseDate) {

        // Declared SQL types let JdbcTemplate bind a null releaseDate correctly.
        jdbcTemplate.update(
                ADD_MOVIE_SQL,
                new Object[]{movieId, title, genre, language, durationMinutes, releaseDate},
                ADD_MOVIE_TYPES);
    }
}