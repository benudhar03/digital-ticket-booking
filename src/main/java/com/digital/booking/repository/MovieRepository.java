package com.digital.booking.repository;

import com.digital.booking.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    @Query("""
        SELECT m FROM Movie m
        WHERE (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')))
          AND (:genre IS NULL OR LOWER(m.genre) = LOWER(:genre))
        ORDER BY m.title
        """)
    List<Movie> searchMovies(
            @Param("title") String title,
            @Param("genre") String genre
    );
}