package com.digital.booking.controller;

import com.digital.booking.dto.AddMovieRequest;
import com.digital.booking.dto.MovieResponse;
import com.digital.booking.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse addMovie(@Valid @RequestBody AddMovieRequest request) {
        return movieService.addMovie(request);
    }

    @GetMapping
    public List<MovieResponse> searchMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre) {

        return movieService.searchMovies(title, genre);
    }
}