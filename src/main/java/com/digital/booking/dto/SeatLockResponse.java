package com.digital.booking.dto;
import java.util.List;
public record SeatLockResponse(boolean acquired, String token, List<String> keys) {}
