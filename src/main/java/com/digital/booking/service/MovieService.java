package com.digital.booking.service;

import com.digital.booking.dto.AddMovieRequest;
import com.digital.booking.dto.MovieResponse;
import com.digital.booking.entity.Movie;
import com.digital.booking.repository.MovieProcedureRepository;
import com.digital.booking.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MovieService {

    private final MovieProcedureRepository movieProcedureRepository;
    private final MovieRepository movieRepository;

    public MovieService(
            MovieProcedureRepository movieProcedureRepository,
            MovieRepository movieRepository) {
        this.movieProcedureRepository = movieProcedureRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public MovieResponse addMovie(AddMovieRequest request) {

        UUID movieId = UUID.randomUUID();

        movieProcedureRepository.addMovie(
                movieId,
                request.title(),
                request.genre(),
                request.language(),
                request.durationMinutes(),
                request.releaseDate()
        );

        return new MovieResponse(movieId, request.title(), request.genre(),
                request.language(),
                request.durationMinutes(),
                request.releaseDate()
        );
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> searchMovies(String title, String genre) {

        return movieRepository.searchMovies(title, genre)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private MovieResponse mapToResponse(Movie movie) {

        return new MovieResponse(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getGenre(),
                movie.getLanguage(),
                movie.getDurationMinutes(),
                movie.getReleaseDate()
        );
    }
}