# Phase 0 — Project Setup

## Overview
Spring Boot backend for restaurant delivery integration with Uber Direct and WooCommerce.

## Stack
| Layer | Technology             |
|---|------------------------|
| Language | Java 21                |
| Framework | Spring Boot 4.0.6      |
| Database | PostgreSQL 16 (Docker) |
| Build | Maven                  |
| Hosting | Railway / Render       |

## Prerequisites
- Java 21
- Maven 3.9+
- Docker Desktop

## Running Locally

### 1. Start the database
```bash
docker compose up -d
```

### 2. Set local profile in IntelliJ
Add `SPRING_PROFILES_ACTIVE=local` to Run Configuration environment variables.

### 3. Run the app
```bash
mvn spring-boot:run
```

### 4. Verify
Visit `http://localhost:8080/api/health` — expect `{ "status": "UP" }`.

## Environment Variables
All secrets live in `application-local.properties` (never committed).

| Variable | Description |
|---|---|
| `UBER_CLIENT_ID` | Uber Direct OAuth client ID |
| `UBER_CLIENT_SECRET` | Uber Direct OAuth client secret |
| `UBER_CUSTOMER_ID` | Uber Direct customer ID |
| `WOOCOMMERCE_URL` | WordPress site URL |
| `WOOCOMMERCE_KEY` | WooCommerce REST API key |
| `WOOCOMMERCE_SECRET` | WooCommerce REST API secret |
| `WOOCOMMERCE_WEBHOOK_SECRET` | Shared secret for webhook verification |
| `RESTAURANT_NAME` | Location display name |
| `RESTAURANT_ADDRESS` | Pickup address sent to Uber |
| `RESTAURANT_PHONE` | Restaurant contact number |

## Git Conventions
- Branch naming: `phase-N/short-description`
- Commit format: `phase-N: what changed`
- Never commit `application-local.properties` or any `.env` file