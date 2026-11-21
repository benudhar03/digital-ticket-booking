# Digital Ticket Booking

Learning project: Java 21, Spring Boot 4.1, PostgreSQL, Redis, Spring Data JPA.
Booking is the first use case; more system design use cases can be added as separate packages.

## Run
1. Install JDK 21, Maven 3.9+ and Docker.
2. Start infrastructure: `docker compose up -d`
3. Start app: `mvn spring-boot:run`

## API docs (Swagger)
- Swagger UI: http://localhost:9080/swagger-ui.html
- OpenAPI JSON: http://localhost:9080/v3/api-docs

## Seed seats
```sql
INSERT INTO show_seat (show_id, seat_id, price, status)
VALUES ('SHOW-100','A1',250.00,'AVAILABLE'),
       ('SHOW-100','A2',250.00,'AVAILABLE'),
       ('SHOW-100','A3',300.00,'AVAILABLE');
```

## API
`POST http://localhost:8080/api/v1/bookings`
Header: `X-User-Id: user-100`
```json
{
  "showId": "SHOW-100",
  "seatIds": ["A1", "A2"],
  "idempotencyKey": "user-100-request-0001"
}
```

Sending the same `idempotencyKey` again for the same user returns the original booking.
To retry after a failed or expired booking, use a new key.

## Booking statuses
| Status | Meaning |
|---|---|
| `PENDING_PAYMENT` | Seats held, waiting for the payment result |
| `CONFIRMED` | Payment succeeded and seats booked |
| `FAILED` | Payment declined; seats released |
| `EXPIRED` | Payment window elapsed; seats released |
| `RECOVERY_REQUIRED` | Payment outcome unknown, or paid but confirmation failed; needs reconciliation |

## Error responses
Errors are returned as RFC 7807 problem details.

| Exception | HTTP status |
|---|---|
| `BookingNotFoundException` | 404 |
| `SeatsUnavailableException`, `InvalidBookingStateException` | 409 |
| `PaymentFailedException` | 402 |
| Validation errors | 400 |
| `InventoryUpdateException` | 500 |

The sample payment adapter always succeeds. Replace it with a real provider integration.

## Important limitations
- This is a teaching project, not production-ready.
- A payment timeout is handled as unknown (the booking becomes `RECOVERY_REQUIRED`); never treat it as a decline.
- Still to add: payment-attempt persistence, stable provider idempotency keys, payment status reconciliation and void/refund handling.
- Still to add: booking expiry/recovery scheduler and Outbox publisher/retry worker.
- Still to add: Flyway migrations, authentication, authorization, observability, rate limiting and concurrency/integration tests.
- `ddl-auto: update` is for local learning only.
- `X-User-Id` is a demo header; production must derive identity from the authenticated security context.
- PostgreSQL conditional seat updates are the final allocation guard; Redis locks are supplemental coordination.
- The Outbox event is persisted transactionally but is not published to Kafka yet.
- Swagger UI should be disabled in production.