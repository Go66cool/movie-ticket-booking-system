# AGENTS.md — AI workflow used for this submission

This file documents the AI-assisted workflow used to design and implement the
Movie Ticket Booking System take-home.

## Tooling

- **VS Code** with the **GitHub Copilot** chat agent (Claude-class model in
  agent mode), used as the primary pair-programmer.
- The agent had access to the workspace via standard file read/write/search
  tools and could run terminal commands (Maven, PowerShell) for compile and
  test feedback loops.

## Operating principles

1. **Brief-first scoping.** Read the PDF brief, restated the explicit
   requirements (multi-city, multi-theater, seat-level booking, time-bound
   holds, multiple pricing tiers, discount codes, payment, refunds with
   configurable policy, concurrent-safe seat allocation, async notifications,
   RBAC for admin/customer, REST APIs, persistence, validation, unit +
   integration tests) and identified what to leave out (UI, deployment, CI/CD,
   microservices, OAuth/SSO/MFA, prod observability).
2. **Decide before coding.** Picked the stack (Spring Boot 3.3, JDK 17, JPA +
   H2/Postgres, JWT, springdoc) and the concurrency strategy (DB-level
   pessimistic write locks on `ShowSeat`, ordered by id) up front before
   writing any code.
3. **Domain model first.** Built the entity graph (`User → City → Theater →
   Screen → Seat`, `Movie`, `Show`, `ShowSeat`, `SeatHold`, `Booking`,
   `BookingSeat`, `Payment`, `Refund`, `DiscountCode`, `RefundPolicy`,
   `PricingTier`) before any service or controller.
4. **Vertical slices.** Built each capability end-to-end (DTO → service →
   controller → test) instead of layer-by-layer, so the test feedback loop
   stayed tight.
5. **Compile-then-test cadence.** After every meaningful chunk, ran `mvn
   compile` to catch type errors, then `mvn test` to catch regressions.
   First test run surfaced FK-order issues in test cleanup; fixed and
   re-ran; final state is 18 tests / 0 failures.
6. **Explicit assumptions.** Anything that wasn't pinned down by the brief
   (pricing-tier precedence, refund-stacking rules, hold TTL, payment
   provider abstraction, single-code-per-booking, etc.) is documented in
   `README.md` § "Assumptions".

## Prompts that drove the build (paraphrased)

| Step | Intent                                                                                  |
|------|-----------------------------------------------------------------------------------------|
| 1    | Read the brief PDF; produce a scoping list and entity-graph sketch.                     |
| 2    | Scaffold a Maven Spring Boot 3.3 project on Java 17 with the chosen dependencies.       |
| 3    | Create the JPA entity model; enforce uniqueness/indexes; use `@Version` where needed.   |
| 4    | Wire JWT auth + role-based access control; admin role guards `/api/admin/**`.           |
| 5    | Implement seat-hold service with `SELECT ... FOR UPDATE` on ShowSeat (lock by id order).|
| 6    | Implement BookingService: hold-to-pending → pay → confirmed; refund + cancel paths.     |
| 7    | Add scheduled sweeper to expire stale holds in `REQUIRES_NEW` transactions.             |
| 8    | Add async NotificationService on a dedicated executor (non-blocking).                   |
| 9    | Write integration tests for concurrency (8-thread fan-out on one seat) + hold expiry.   |
| 10   | Compile & test loop; fix FK-order test-cleanup bug; verify 18/18 pass.                  |

## Things deliberately not delegated

- All design choices (data model, concurrency strategy, transaction boundaries,
  RBAC model, refund-policy semantics, pricing-tier precedence rules) were
  decided by the developer; the agent implemented to spec.
- The README's "Assumptions" section was authored to make those choices
  inspectable rather than implicit in code.

## Reproducibility

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean test            # all 18 tests pass
mvn spring-boot:run       # http://localhost:8080
```
