# Project Guidelines — Booking & Loyalty Service

Guidance for anyone (human or AI) working in this repo. Biased toward caution
and clarity over speed.

## What this is
A Spring Boot 3 / Java 21 backend for hotel booking + a loyalty-points program.
JWT auth, role-based access (CUSTOMER/ADMIN), PostgreSQL, Redis caching. No frontend.

## How to work here

1. **Think before coding.** State assumptions; if a requirement is ambiguous,
   name the options instead of silently picking one.
2. **Simplicity first.** Minimum code that solves the problem. No speculative
   abstractions, no config knobs nobody asked for, no error handling for
   impossible cases. If 200 lines could be 50, write 50.
3. **Surgical changes.** Touch only what the task needs. Match the surrounding
   style. Don't refactor or reformat unrelated code. Remove only the orphans
   *your* change created.
4. **Goal-driven.** Turn each task into a verifiable check, then loop until it
   passes. "Add X" → write a failing test for X, make it pass.

## Conventions in this codebase
- **Layering:** thin controllers → services hold business logic → repositories.
- **DTOs only at the edges.** Never serialize JPA entities; map to/from records
  in `*/dto`.
- **Exceptions:** throw `ResourceNotFoundException` (404), `BadRequestException`
  (400), `UnauthorizedException` (403). `GlobalExceptionHandler` maps them.
- **Transactions:** any multi-write workflow is one `@Transactional` service
  method. The booking-completion + points-award flow is the canonical example.
- **Auth:** the JWT carries `uid` + `role`; `JwtAuthenticationFilter` builds an
  `AuthenticatedUser` principal. Use `@AuthenticationPrincipal` in controllers
  and `@PreAuthorize("hasRole('ADMIN')")` for admin routes.
- **Caching:** `@Cacheable`/`@CacheEvict` on `CustomerService` (cache name
  `customer`, key = userId). Redis in prod; in-memory in the `test` profile.

## Verify before claiming done
- `./mvnw test` must pass (uses H2 + in-memory cache; no Docker needed).
- Don't add Testcontainers unless Docker is part of the dev baseline.

## Build/run notes
- Tests pass on JDK 21. This machine has JDK 26, so surefire sets
  `net.bytebuddy.experimental=true` (no-op on 21). Prefer Docker (temurin-21)
  or JDK 21 for running the app.
