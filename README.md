# Booking & Loyalty Service

[![CI](https://github.com/valetivivek/Booking-Loyalty-Service/actions/workflows/ci.yml/badge.svg)](https://github.com/valetivivek/Booking-Loyalty-Service/actions/workflows/ci.yml)

A production-style backend for a resort/hotel booking system. Customers register,
book rooms, and earn loyalty points when their stays are completed. Built with
Spring Boot 3 and Java 21, secured with JWT + role-based access control, backed by
PostgreSQL, and accelerated with Redis caching. **Backend only — no frontend.**

---

## Table of contents
- [Overview](#overview)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Core features](#core-features)
- [Database schema](#database-schema)
- [API endpoints](#api-endpoints)
- [Run locally](#run-locally)
- [Run with Docker](#run-with-docker)
- [Example curl requests](#example-curl-requests)
- [Testing](#testing)
- [Design notes & tradeoffs](#design-notes--tradeoffs)
- [Interview explanation](#interview-explanation)

---

## Overview

The service models the back office of a hotel chain:

- **Customers** register, manage a profile, create and cancel bookings, and view
  their loyalty balance/history.
- **Admins** see all bookings, look up any customer, and change a booking's
  status. Marking a booking **COMPLETED** triggers a transactional workflow that
  awards loyalty points and upgrades the customer's tier.

The interesting engineering is in three places: **stateless JWT security with
RBAC**, the **single-transaction booking-completion + points-award flow**, and
**Redis-cached profile reads with eviction on write**.

## Tech stack

| Area | Choice |
|------|--------|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.3 (Web, Security, Data JPA) |
| ORM | Hibernate / JPA |
| Database | PostgreSQL (H2 in tests) |
| Migrations | Flyway |
| Cache | Redis via Spring Cache abstraction |
| Auth | JWT (jjwt), BCrypt password hashing |
| Docs | OpenAPI / Swagger UI (springdoc) |
| Build | Maven (+ Maven Wrapper) |
| Tests | JUnit 5, Mockito, Spring Boot Test, Spring Security Test |
| Packaging | Docker + Docker Compose (multi-stage build) |

## Architecture

Layered, feature-packaged:

```
com.vivek.bookingloyalty
├── config      SecurityConfig, RedisConfig, OpenApiConfig
├── auth        AuthController/Service, JwtService, JwtAuthenticationFilter, AuthenticatedUser, dto/
├── user        User entity, Role enum, UserRepository
├── customer    CustomerController/Service, CustomerProfile, CustomerRepository, dto/
├── booking     BookingController/Service, Booking, BookingStatus, BookingRepository, dto/
├── loyalty     LoyaltyController/Service, LoyaltyAccount, LoyaltyTransaction, tier/type enums, repos, dto/
└── common      GlobalExceptionHandler, ApiError, ResourceNotFound/BadRequest/Unauthorized exceptions
```

**Request flow:** `Controller` (thin, validates input, reads the principal) →
`Service` (business logic, `@Transactional`) → `Repository` (Spring Data JPA) →
PostgreSQL. Entities never leave the service layer; everything crossing the HTTP
boundary is a **DTO record**.

> Note on layout: admin endpoints (`/api/admin/...`) live inside
> `BookingController`/`CustomerController` to keep to the requested package
> structure, gated with `@PreAuthorize("hasRole('ADMIN')")`. `AuthenticatedUser`
> is a small principal record added so the JWT filter has something to put in the
> security context.

## Core features

- **Auth:** register/login, BCrypt hashing, JWT issuance + validation filter,
  roles `CUSTOMER` and `ADMIN`.
- **Profiles:** auto-created on registration; cached in Redis; cache evicted on
  update.
- **Bookings:** create (price computed from room type × nights), view own, view
  one (owner/admin only), cancel; admin lists all and updates status.
- **Loyalty:** one account per customer; `EARNED` points on completion; ledger of
  transactions; tier auto-derived (`BRONZE` 0–999, `SILVER` 1000–4999, `GOLD`
  5000+).
- **Completion workflow:** one `@Transactional` method validates → completes →
  awards points → writes ledger entry → recomputes tier, with a guard against
  double-awarding.

## Database schema

```
users (1) ──< (1) customer_profiles (1) ──< (1) loyalty_accounts (1) ──< (N) loyalty_transactions
                          │                                                        │
                          └────────< (N) bookings (1) ──────────────────────────< (0..1)
```

- **users** — login identity: `email` (unique), `password_hash`, `role`.
- **customer_profiles** — one per user: `first_name`, `last_name`, `phone`.
- **bookings** — many per profile: `room_type`, `check_in_date`,
  `check_out_date`, `status`, `total_amount`.
- **loyalty_accounts** — one per profile: `points_balance`, `tier`.
- **loyalty_transactions** — many per account: `points`, `transaction_type`,
  optional `booking_id` (set for completion awards; also the dedupe key).

Schema is created and versioned by Flyway (`src/main/resources/db/migration/V1__init.sql`);
Hibernate runs in `validate` mode against it in production.

## API endpoints

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/api/auth/register` | public | Register a customer, returns JWT |
| POST | `/api/auth/login` | public | Login, returns JWT |
| GET | `/api/customers/me` | CUSTOMER/ADMIN | Get own profile (cached) |
| PUT | `/api/customers/me` | CUSTOMER/ADMIN | Update own profile (evicts cache) |
| POST | `/api/bookings` | authenticated | Create a booking |
| GET | `/api/bookings/me` | authenticated | List own bookings |
| GET | `/api/bookings/{id}` | owner or ADMIN | Get one booking |
| PATCH | `/api/bookings/{id}/cancel` | owner or ADMIN | Cancel a booking |
| GET | `/api/loyalty/me` | authenticated | Get own loyalty account |
| GET | `/api/loyalty/me/transactions` | authenticated | List own loyalty transactions |
| GET | `/api/admin/bookings` | ADMIN | List all bookings |
| GET | `/api/admin/customers/{id}` | ADMIN | Get any customer profile |
| PATCH | `/api/admin/bookings/{id}/status` | ADMIN | Update status (COMPLETED awards points) |

Interactive docs: **`http://localhost:8080/swagger-ui.html`** (use the
**Authorize** button to paste a `Bearer` token).

## Run locally

**Prerequisites:** JDK 21 (recommended), plus a local PostgreSQL and Redis — or
just use [Docker](#run-with-docker), which needs neither installed.

The repo ships a **Maven Wrapper** (`mvnw`/`mvnw.cmd`), so you don't need Maven
installed; the first run downloads it.

```bash
# 1. Start Postgres + Redis (or run your own). Quick way:
docker compose up -d postgres redis

# 2. Run the app (uses defaults in application.yml; override via env if needed)
./mvnw spring-boot:run
```

Default config (override with env vars):

| Env var | Default |
|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/bookingloyalty` |
| `SPRING_DATASOURCE_USERNAME` | `booking` |
| `SPRING_DATASOURCE_PASSWORD` | `booking_secret` |
| `SPRING_DATA_REDIS_HOST` | `localhost` |
| `SPRING_DATA_REDIS_PORT` | `6379` |
| `JWT_SECRET` | dev placeholder (set a real 32+ byte secret in prod) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) |

> **JDK note:** this project targets Java 21. If you build/test on a newer JDK
> (e.g. 26), the build already sets `net.bytebuddy.experimental=true` for tests so
> Mockito/Hibernate work. For running the app on a very new JDK, prefer Docker.

## Run with Docker

Everything (Postgres, Redis, app) with one command — only Docker required:

```bash
cp .env.example .env      # optional; compose has sane defaults
docker compose up --build
```

The app comes up on `http://localhost:8080`. The Dockerfile is multi-stage
(builds the jar with Maven, runs it on a slim JRE 21 image).

## Example curl requests

```bash
# Register (returns a JWT)
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password123","firstName":"Alice","lastName":"Smith","phone":"555-1000"}'

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password123"}' | jq -r .token)

# Get my profile (cached in Redis)
curl -s http://localhost:8080/api/customers/me -H "Authorization: Bearer $TOKEN"

# Create a booking (DELUXE = $200/night; 2 nights => $400 total)
curl -s -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"roomType":"DELUXE","checkInDate":"2026-09-01","checkOutDate":"2026-09-03"}'

# List my bookings
curl -s http://localhost:8080/api/bookings/me -H "Authorization: Bearer $TOKEN"

# Cancel a booking
curl -s -X PATCH http://localhost:8080/api/bookings/1/cancel -H "Authorization: Bearer $TOKEN"

# --- Admin: complete a booking to award loyalty points ---
# (create an ADMIN user directly in the DB, or promote a row's role to 'ADMIN')
curl -s -X PATCH http://localhost:8080/api/admin/bookings/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"COMPLETED"}'

# Check loyalty
curl -s http://localhost:8080/api/loyalty/me -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/loyalty/me/transactions -H "Authorization: Bearer $TOKEN"
```

> Room rates: `STANDARD` $100, `DELUXE` $200, `SUITE` $400 per night.
> Registration always creates a `CUSTOMER`; create an admin by inserting a user
> row with `role = 'ADMIN'` (e.g. via psql) — admins aren't self-serviceable by design.

## Testing

```bash
./mvnw test
```

Tests run entirely in-memory (H2 + simple cache) — **no Docker/Postgres/Redis
required**. Coverage:

| Test | What it proves |
|------|----------------|
| `AuthServiceTest` | registration creates user+profile+loyalty account; duplicate email rejected; login success; wrong password rejected |
| `BookingServiceTest` | total computed from room type × nights; create validation; cancel; can't cancel completed; complete awards points; can't complete cancelled; **customer can't read another's booking**; admin can |
| `LoyaltyServiceTest` | points added + tier recomputed + ledger written; **duplicate award blocked**; points floor to whole numbers; tier thresholds |
| `BookingLoyaltyIntegrationTest` | end-to-end over MockMvc + real security: 401 unauthenticated, **403 customer→admin endpoint**, admin allowed, **403 cross-customer booking**, and the full register→book→complete→points→no-double-award flow |

## Design notes & tradeoffs

- **Spring Cache over raw `RedisTemplate`** — declarative `@Cacheable`/`@CacheEvict`
  keeps caching out of business logic and gives correct invalidation for free.
- **Price computed server-side** — clients never send the amount, so totals can't
  be tampered with.
- **H2 for tests, not Testcontainers** — tests stay fast and need no Docker daemon.
  Testcontainers would give higher-fidelity Postgres testing and is the natural
  next step if the dev baseline includes Docker.
- **Stateless JWT** — no server session store, easy horizontal scaling; the
  tradeoff is no server-side revocation (mitigated by short expiry; a denylist
  would be the next step).
- **Admins provisioned out-of-band** — there's no "make me admin" endpoint by
  design (privilege escalation risk).

### Possible future improvements
Refresh tokens + token revocation denylist; point **redemption** endpoint; room
inventory/availability so bookings can't overbook; pagination on list endpoints;
Testcontainers integration tests; rate limiting on auth; audit logging;
optimistic locking (`@Version`) on loyalty accounts for concurrent completion.

## Interview explanation

### 30-second version
> "It's a hotel booking backend in Spring Boot and Java 21. Customers register,
> book rooms, and earn loyalty points; admins manage bookings. It uses stateless
> JWT auth with role-based access control, PostgreSQL with Flyway migrations, and
> Redis to cache profile reads. The core piece is completing a booking: in one
> database transaction it marks the booking complete, awards loyalty points,
> writes a ledger entry, and bumps the loyalty tier — so it either all succeeds or
> all rolls back."

### 2-minute version
> "The domain is a resort booking system with a loyalty program. There are two
> roles — CUSTOMER and ADMIN. A user registers, which in one transaction creates
> their login, a profile, and a loyalty account. Login and register return a JWT
> that encodes the user id and role.
>
> Security is stateless: a `OncePerRequestFilter` reads the `Bearer` token,
> verifies its signature, and puts an `AuthenticatedUser` into the Spring Security
> context. Public routes like login are open; everything else needs a valid token;
> admin routes are protected with method-level `@PreAuthorize`. Customers can only
> touch their own data — the service checks ownership and returns 403 otherwise.
>
> Data is in PostgreSQL via JPA, with Flyway owning the schema. I use DTO records
> at the API boundary so I never leak entities. Profile reads are cached in Redis
> with Spring's cache abstraction, and an update evicts the cache key so the next
> read is fresh.
>
> The headline workflow is booking completion. When an admin sets a booking to
> COMPLETED, a single `@Transactional` service method validates the booking isn't
> cancelled and hasn't already been awarded, flips the status, computes points at
> one per dollar, adds them to the loyalty account, writes a loyalty transaction,
> and recomputes the tier. Because it's one transaction, a failure anywhere rolls
> back both the status change and the points. It's fully tested — unit tests with
> Mockito and an end-to-end test over MockMvc with real security."

### Deep dive — transactional booking completion
- Entry point: `PATCH /api/admin/bookings/{id}/status` with `{"status":"COMPLETED"}`
  → `BookingService.complete(id)`, annotated `@Transactional`.
- Steps: load booking (404 if missing) → reject if `CANCELLED` (400) → reject if
  already `COMPLETED` (400) → set `COMPLETED` → call
  `LoyaltyService.awardForCompletedBooking(booking)`.
- The loyalty step **joins the same transaction** (default propagation
  `REQUIRED`, no separate `@Transactional`). It checks
  `existsByBookingIdAndTransactionType(bookingId, EARNED)` to prevent
  double-awarding, loads the account, adds `floor(totalAmount)` points, recomputes
  the tier, and saves a ledger row linked to the booking.
- **Why one transaction:** points and booking status must be consistent. If
  writing the ledger fails, the COMPLETED status must not persist — otherwise the
  booking looks done but the customer never got points. One `@Transactional`
  boundary gives that all-or-nothing guarantee.

### Deep dive — JWT & RBAC
- On login, `JwtService` signs an HS256 token with subject=email and claims
  `uid` + `role`, plus an expiry.
- Each request: `JwtAuthenticationFilter` extracts the `Bearer` token, verifies
  the signature/expiry, builds `AuthenticatedUser(id, email, role)`, and sets a
  `UsernamePasswordAuthenticationToken` with authority `ROLE_<role>`.
- Authorization happens in two layers: URL rules in `SecurityConfig` (public vs
  authenticated) and method rules via `@PreAuthorize("hasRole('ADMIN')")`.
  Ownership ("my own booking") is enforced in the service. No token → clean 401;
  wrong role / not owner → 403.
- Stateless: the server stores nothing per session, so any instance can serve any
  request — easy to scale.

### Deep dive — Redis caching
- `CustomerService.getByUserId` is `@Cacheable(cacheNames="customer", key="#userId")`,
  so the first read hits Postgres and subsequent reads come from Redis (key
  `customer::{userId}`, 10-minute TTL, JSON-serialized).
- `CustomerService.update` is `@CacheEvict(cacheNames="customer", key="#userId")`,
  so a profile change removes the stale entry and the next read repopulates it.
- In tests the cache type is `simple` (in-memory) and Redis auto-config is
  excluded, so caching behavior is exercised without a Redis server.

### Likely interview questions & strong answers
- **Why store role in the JWT instead of looking it up per request?** Avoids a DB
  hit on every call and keeps auth stateless. Tradeoff: a role change only takes
  effect on the next token; acceptable with short expiry, or add a denylist.
- **How do you stop a customer reading someone else's booking?** The service
  compares the booking's owner id to the authenticated user's id and throws a 403
  (`UnauthorizedException`) unless they're ADMIN. Covered by a test.
- **What if two completions race?** Today the duplicate-award guard + single
  transaction handle the common case; under true concurrency I'd add a unique
  constraint on `(booking_id, transaction_type)` and/or `@Version` optimistic
  locking on the account.
- **Why DTOs?** To decouple the API from the schema, avoid serializing lazy JPA
  relations, and never expose fields like `password_hash`.
- **Why Flyway with `ddl-auto=validate`?** Migrations are explicit, versioned, and
  reviewable; `validate` makes the app fail fast if entities and schema drift.
- **How is the password stored?** BCrypt hash via Spring Security's
  `PasswordEncoder` — never plaintext, and salted per-hash.
