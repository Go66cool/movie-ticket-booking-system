# Movie Ticket Booking System

A Spring Boot REST API implementing a multi-city, multi-theater, seat-level movie ticket
booking system with concurrent seat selection, time-bound holds, configurable pricing tiers,
discount codes, mock payments, and policy-driven refunds.

> SDE-2 take-home submission. Stack: Java 17, Spring Boot 3.3, Spring Data JPA,
> Spring Security (JWT), H2 (default) / PostgreSQL, JUnit 5 + Mockito.

---

## 1. Quick start

### Prerequisites
- **JDK 17+** (project was built and tested on Microsoft OpenJDK 17.0.15)
- **Maven 3.9+**

### Run

```powershell
# (Windows) point Maven at JDK 17 if your default is different
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

mvn spring-boot:run
```

The app starts on **http://localhost:8080**. H2 console at `/h2-console`
(JDBC URL `jdbc:h2:mem:booking`, user `sa`, no password). OpenAPI/Swagger UI at
`http://localhost:8080/swagger-ui.html`.

### Run tests

```powershell
mvn test
```

### Seeded users (dev profile)
| Email                  | Password   | Role     |
|------------------------|------------|----------|
| `admin@booking.local`  | `admin123` | ADMIN    |
| `alice@example.com`    | `password1`| CUSTOMER |
| `bob@example.com`      | `password1`| CUSTOMER |

Seed also creates 2 cities, 2 theaters, a 26-seat screen with multiple seat
categories, 2 movies, pricing tiers, refund policies, 2 discount codes, and 2 shows.

---

## 2. Architecture & design choices

### Tech stack rationale
| Concern        | Choice                                  | Why                                                                 |
|----------------|------------------------------------------|----------------------------------------------------------------------|
| Framework      | Spring Boot 3.3                          | Required by brief; mature, batteries-included.                       |
| Language       | Java 17                                  | LTS; records keep DTOs/value objects concise.                        |
| Persistence    | Spring Data JPA + Hibernate              | First-class transactions + pessimistic locking; portable to Postgres.|
| Database       | H2 (dev/test); Postgres driver included  | Zero-friction dev; same SQL surface via `MODE=PostgreSQL`.           |
| Security       | Spring Security + JWT (jjwt 0.12)        | Stateless, simple RBAC. Brief explicitly rules out OAuth/SSO.        |
| Validation     | Jakarta Bean Validation                  | Declarative request validation.                                      |
| API docs       | springdoc-openapi                        | Auto Swagger UI from controllers.                                    |
| Async notif.   | `@Async` on dedicated `ThreadPoolTaskExecutor` | Notifications never block the booking flow.                          |
| Tests          | JUnit 5, Mockito, Spring Boot Test       | Pure unit + full-context integration coverage.                       |

### Domain model
```
City 1─* Theater 1─* Screen 1─* Seat
                       └─ 1─* Show ─ 1─* ShowSeat ─*─1 Seat
                                          │
                                          ├─ status: AVAILABLE/HELD/BOOKED
                                          └─ price (per-seat, computed from PricingTier × basePrice)

User ─ * SeatHold * ─ ShowSeat   (hold pins ShowSeat with holdId + TTL)
User ─ * Booking * ─ ShowSeat (via BookingSeat)
Booking ─ 1..* Payment / 0..1 Refund / 0..1 DiscountCode
RefundPolicy (rules), PricingTier (rules)
```

### Concurrency model — no double-allocation
Seat allocation is serialized at the row level using **JPA pessimistic write locks**
on `ShowSeat` rows (`SELECT ... FOR UPDATE` ordered by `id` to avoid deadlocks):

- `ShowSeatRepository.lockByShowAndSeats(showId, seatIds)` is called inside the hold
  transaction. Concurrent transactions block until the holder commits, then re-read
  and observe the seat as `HELD`, returning `ConflictException → HTTP 409`.
- The integration test `concurrentHolds_onSameSeat_serializeWithoutDoubleAllocation`
  fans out 8 threads onto a single seat and asserts exactly 1 success / 7 conflicts.
- `ShowSeat` also carries a JPA `@Version` (optimistic) — belt-and-braces.

### Hold lifecycle
1. `POST /api/holds` → locks seats, transitions them `AVAILABLE → HELD`, creates a
   `SeatHold` with `expiresAt = now + ttl` (default 10 min, configurable via
   `app.hold.ttl-seconds`).
2. A `@Scheduled` sweeper (`app.hold.sweep-interval-ms`, default 15 s) finds
   `ACTIVE` holds past `expiresAt`, marks them `EXPIRED`, and releases the seats
   `HELD → AVAILABLE`. Each hold is expired in `REQUIRES_NEW` so one bad hold
   cannot stall the rest.
3. Confirming a hold (booking) transitions it `ACTIVE → CONVERTED` and seats
   `HELD → BOOKED` in a single transaction with seat re-lock.
4. Manual `DELETE /api/holds/{id}` releases early (owner or admin).

### Booking + payment + refund
- `POST /api/bookings { holdId, discountCode? }` → creates `PENDING_PAYMENT`
  booking, computes subtotal from `ShowSeat.price`, applies discount under a
  pessimistic lock on the code row (atomic `usedCount++`).
- `POST /api/bookings/{id}/pay` → re-locks seats, validates still `HELD`, calls
  the mock `PaymentService`. On success: `CONFIRMED`, seats → `BOOKED`, async
  notification dispatched. `{"mockOutcome":"FAIL"}` forces a failure path —
  booking stays `PENDING_PAYMENT` so the customer can retry.
- `POST /api/bookings/{id}/cancel` → if `CONFIRMED`, evaluates the
  `RefundPolicy` rules (highest matching refund % wins), creates `Refund`
  and negative `Payment`, frees seats. If `PENDING_PAYMENT`, decrements the
  discount-code usage and frees seats.

### Pricing tiers
A `PricingTier` is `(showType?, seatCategory?, multiplier)`. Both filters are
optional — `null` means "any". Most-specific match wins; absent any match the
multiplier defaults to `1.0`. Final `ShowSeat.price = basePrice × multiplier`,
computed once at show creation and snapshotted on each `ShowSeat`, so the price
quoted at hold time matches the price paid.

### Discount codes
Locked-per-code via `SELECT ... FOR UPDATE`; supports `percentOff`, `flatOff`,
`maxDiscount` cap, validity window, and global `usageLimit`. Usage is
incremented at booking creation and decremented on cancel-before-payment.

### Async notifications
`NotificationService` uses `@Async("notificationExecutor")` with a dedicated
`ThreadPoolTaskExecutor` (core 2 / max 8 / queue 500). Booking confirmation
and cancellation log lines are emitted asynchronously and never block the
HTTP response.

### RBAC
- All `/api/admin/**` endpoints require `ROLE_ADMIN` (enforced at filter chain
  level and via `@PreAuthorize` on the controller).
- All `/api/holds/**`, `/api/bookings/**` endpoints require authentication.
  Service-layer ownership checks ensure customers can only see/modify their
  own holds and bookings.
- Public GETs: `/api/cities/**`, `/api/theaters/**`, `/api/movies/**`,
  `/api/shows/**`, plus Swagger and H2 console.

### Error handling
`GlobalExceptionHandler` produces a uniform `ApiError` JSON body:
```json
{
  "timestamp": "...",
  "status": 409,
  "error": "Conflict",
  "message": "Seat A1 is not available",
  "path": "/api/holds",
  "fieldErrors": []
}
```
Mapped: `NotFoundException → 404`, `ConflictException`/`OptimisticLock`/`Pessimistic`/`DataIntegrity → 409`,
`BadRequestException`/`MethodArgumentNotValid → 400`, `ForbiddenException`/`AccessDenied → 403`,
`BadCredentialsException → 401`, everything else `→ 500`.

---

## 3. API surface

> All requests/responses are JSON. Authenticated endpoints require
> `Authorization: Bearer <token>` from `/api/auth/login`.

### Auth
| Method | Path                | Body                                      | Auth   |
|--------|---------------------|-------------------------------------------|--------|
| POST   | `/api/auth/register`| `{email,password,fullName}`               | public |
| POST   | `/api/auth/login`   | `{email,password}`                        | public |
| GET    | `/api/auth/me`      | —                                         | any    |

### Browse (public)
| Method | Path                                       |
|--------|--------------------------------------------|
| GET    | `/api/cities` / `/api/cities/{id}/theaters`|
| GET    | `/api/movies` / `/api/movies/{id}`         |
| GET    | `/api/shows/search?movieId&cityId&from&to` |
| GET    | `/api/shows/{id}` / `/api/shows/{id}/seats`|

### Customer
| Method | Path                          | Body                                                |
|--------|-------------------------------|-----------------------------------------------------|
| POST   | `/api/holds`                  | `{showId, seatIds:[..]}`                            |
| DELETE | `/api/holds/{id}`             | —                                                   |
| POST   | `/api/bookings`               | `{holdId, discountCode?}`                           |
| POST   | `/api/bookings/{id}/pay`      | `{method, mockOutcome?}` (`mockOutcome:"FAIL"` → 409)|
| POST   | `/api/bookings/{id}/cancel`   | —                                                   |
| GET    | `/api/bookings/{id}`          | —                                                   |
| GET    | `/api/bookings/me`            | `?page&size` (Spring `Pageable`)                    |

### Admin (`ROLE_ADMIN`)
| Method | Path                              | Purpose                  |
|--------|-----------------------------------|--------------------------|
| POST   | `/api/admin/cities`               | Create city              |
| POST   | `/api/admin/theaters`             | Create theater           |
| POST   | `/api/admin/screens`              | Create screen + seats    |
| POST   | `/api/admin/movies`               | Create movie             |
| POST   | `/api/admin/shows`                | Create show + ShowSeats  |
| POST   | `/api/admin/discounts`            | Create discount code     |
| GET    | `/api/admin/discounts`            | List codes               |
| POST   | `/api/admin/refund-policies`      | Add refund rule          |
| GET    | `/api/admin/refund-policies`      | List rules               |
| POST   | `/api/admin/pricing-tiers`        | Add pricing rule         |
| GET    | `/api/admin/pricing-tiers`        | List pricing rules       |

### Example: end-to-end happy path
```bash
# 1. Login (admin)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password1"}' | jq -r .token)

# 2. Browse a show and its seat map
curl http://localhost:8080/api/shows/1/seats | jq '.[0:5]'

# 3. Hold two seats
HOLD_ID=$(curl -s -X POST http://localhost:8080/api/holds \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"showId":1,"seatIds":[1,2]}' | jq .id)

# 4. Create a booking (with discount)
BOOKING_ID=$(curl -s -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"holdId\":$HOLD_ID,\"discountCode\":\"WELCOME10\"}" | jq .id)

# 5. Pay
curl -X POST http://localhost:8080/api/bookings/$BOOKING_ID/pay \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"method":"CARD"}'

# 6. Cancel (refund applied per policy)
curl -X POST http://localhost:8080/api/bookings/$BOOKING_ID/cancel \
  -H "Authorization: Bearer $TOKEN"
```

---

## 4. Testing approach

| Layer        | Test                                          | Coverage |
|--------------|-----------------------------------------------|----------|
| Unit         | `PricingServiceTest`                          | Most-specific-tier selection rules. |
| Unit         | `RefundPolicyServiceTest`                     | Refund quote across time windows; most-favorable policy chosen. |
| Integration  | `BookingFlowIntegrationTest`                  | Full happy path with discount; concurrent same-seat fan-out (8 threads → 1 success); hold expiry by background sweeper; cancel + refund (full policy); payment failure leaves booking pending; duplicate-hold conflict. |
| Web / sec    | `AuthAndAccessControlTest`                    | Register + login + role-based access (customer → admin endpoint = 403); 401 on unauthenticated; validation error shape. |

Total: **18 tests, all passing**.

Integration tests use the `test` Spring profile which shortens the hold TTL to
2 s and the sweep interval to 500 ms so expiry can be deterministically observed.

---

## 5. Configuration

`application.yml` keys under `app.*`:

| Key                          | Default  | Meaning                                          |
|------------------------------|----------|--------------------------------------------------|
| `app.security.jwt.secret`    | dev-only | HMAC-SHA secret (≥ 32 bytes). **Override in prod.** |
| `app.security.jwt.ttl-minutes` | `720`  | Token lifetime.                                  |
| `app.hold.ttl-seconds`       | `600`    | Seat-hold TTL.                                   |
| `app.hold.sweep-interval-ms` | `15000`  | Expiry sweeper cadence.                          |
| `app.notifications.enabled`  | `true`   | Toggle async notification publishing.            |
| `app.seed.enabled`           | `true`   | Run sample-data seeder on startup.               |

Switch to Postgres by overriding `spring.datasource.*` and disabling `spring.h2.console`.

---

## 6. Assumptions

1. **Single-region, single-process** deployment. The pessimistic-lock strategy
   is correct on any RDBMS but would need a distributed lock (Redis / DB row
   token) if you ran multiple writers across regions.
2. **Mock payment provider.** `PaymentService` returns success unless
   `mockOutcome=FAIL` is sent. A real integration would verify webhook
   signatures and store provider transaction IDs.
3. **Pricing is snapshotted** at show creation onto each `ShowSeat.price`. The
   customer always sees the same price between hold and payment, even if the
   admin edits tiers later.
4. **One refund policy per cancellation**: the active policy with the highest
   `refundPercent` whose `hoursBeforeShow` threshold is satisfied. Two
   stacked rules don't multiply.
5. **Discounts are exclusive**: at most one code per booking.
6. **Hold TTL** defaults to 10 minutes — a typical industry value.
7. **Notifications** are simulated by logging on a dedicated executor. The
   abstraction is in `NotificationService`, so plugging in SES/SNS/Kafka
   later is a single-file change.
8. **No double-booking guarantees**: enforced by pessimistic write lock on
   `ShowSeat` rows + DB unique constraint on `booking_seats.show_seat_id`.
   The latter is a defense-in-depth invariant.
9. **Authorization** is role-based; ownership checks are at the service layer.
   A scoped-token / per-tenant model is out of scope.
10. **Time** is stored as UTC `Instant`.

---

## 7. Project layout

```
src/main/java/com/example/booking
  BookingApplication.java
  config/        AppProperties, SecurityConfig, AsyncConfig, DataSeeder
  security/      JwtService, JwtAuthFilter, AppUserPrincipal, AppUserDetailsService
  domain/        JPA entities + enums
  repository/    Spring Data JPA repos (+ pessimistic-lock queries)
  service/       Business logic (Auth, City/Theater/Screen/Movie/Show, Pricing,
                 Discount, RefundPolicy, SeatHold, Booking, Payment, Notification)
  dto/           Request/response records (Bean Validation annotated)
  web/           REST controllers
  exception/     ApiError + GlobalExceptionHandler + typed exceptions
src/main/resources
  application.yml
src/test/java/com/example/booking
  service/PricingServiceTest.java
  service/RefundPolicyServiceTest.java
  BookingFlowIntegrationTest.java
  web/AuthAndAccessControlTest.java
```

---

## 8. What's intentionally out of scope

Per the brief: no UI, deployment, containerization, CI/CD, microservices,
advanced auth (OAuth/SSO/MFA), or production observability.

## 9. AI workflow

See [`AGENTS.md`](AGENTS.md).
