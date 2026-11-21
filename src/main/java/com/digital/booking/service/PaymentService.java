package com.digital.booking.service;
import com.digital.booking.domain.Booking;
import com.digital.booking.dto.PaymentResponse;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class PaymentService {
    // Demo only: replace with a provider client and stable provider-side idempotency key.
    // A timeout must be treated as UNKNOWN and reconciled, not as a decline.
    public PaymentResponse authorizePayment(Booking booking) {
        return new PaymentResponse(true, "PAY-" + UUID.randomUUID(), "Demo authorization success");
    }
}
