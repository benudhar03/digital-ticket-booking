package com.digital.booking.controller;

import com.digital.booking.dto.BookTicketRequest;
import com.digital.booking.dto.BookTicketResponse;
import com.digital.booking.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
@Validated
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookTicketResponse book(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @Valid @RequestBody BookTicketRequest request) {
        return bookingService.bookTicket(userId, request);
    }
}