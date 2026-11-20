# Order Microservices

Two Spring Boot microservices — **product-service** and **order-service** — that communicate over REST, each with its own PostgreSQL database, orchestrated with Docker Compose.

![CI](../../actions/workflows/ci.yml/badge.svg) ![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen) ![Docker](https://img.shields.io/badge/Docker%20Compose-ready-2496ED)

## Architecture

```
            POST /api/orders
 client ──────────────────────► order-service (8082) ──► orders-db
                                     │
                                     │ POST /api/products/{id}/reserve   (RestClient, timeouts)
                                     ▼
                               product-service (8081) ──► products-db
```

**Order flow**
1. `order-service` receives an order request and validates it.
2. It asks `product-service` to **reserve** stock. The reservation is a single atomic SQL statement (`UPDATE … SET stock = stock - :qty WHERE id = :id AND stock >= :qty`), so concurrent orders can never oversell.
3. Only after a successful reservation is the order stored, with the product name and price snapshotted at order time.

**Failure handling**

| product-service response | order-service returns |
|---|---|
| 404 — unknown product | `404 Not Found` |
| 409 — insufficient stock | `409 Conflict` (no order stored) |
| unreachable / timeout | `503 Service Unavailable` |

All errors use RFC 7807 `ProblemDetail` JSON.

## Key design points
- **Database per service** — services never share tables, only an HTTP contract
- **Atomic stock reservation** — proven by a concurrency test (30 parallel requests, 10 in stock → exactly 10 succeed)
- **Resilient HTTP client** — connect/read timeouts, remote status codes mapped to domain exceptions
- **Single multi-stage Dockerfile** for both services, non-root runtime user
- **Tests** — MockMvc + H2; the order service mocks the product client with `@MockitoBean`

## Running

```bash
cp .env.example .env      # set DB_PASSWORD
docker compose up --build
```

```bash
curl localhost:8081/api/products
curl -X POST localhost:8082/api/orders -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
```

Tests:
```bash
./mvnw verify
```

## Tech stack
Java 17 · Spring Boot 3.4 · Spring Data JPA · RestClient · PostgreSQL · H2 · JUnit 5 · Mockito · Docker Compose · GitHub Actions
