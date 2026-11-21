package com.digital.booking.dto;
import com.digital.booking.domain.Booking;
import com.digital.booking.domain.BookingStatus;
import com.digital.booking.domain.*;
import java.math.BigDecimal;
public record BookTicketResponse(Long bookingId, String showId, int seatCount, BigDecimal totalAmount, BookingStatus status, String paymentId) {

    public static BookTicketResponse from(Booking b) {
        return new BookTicketResponse(b.getId(), b.getShowId(), b.getSeatCount(), b.getTotalAmount(), b.getStatus(), b.getPaymentId());
    }
}
