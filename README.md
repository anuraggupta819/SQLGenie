# SQLGenie

Converts natural language into SQL, validates it as read-only, executes it against PostgreSQL, explains it in plain English, and tracks query history and favorites.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full system design.

## Tech Stack

- Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Spring AI
- PostgreSQL
- React + TypeScript
- Docker, GitHub Actions, Azure

## Local Development

### Prerequisites

- JDK 21
- PostgreSQL 16 running locally (a `docker-compose.yml` is provided for machines with a working Docker daemon; a native install works identically)

### Database setup (one-time)

1. Run the superuser block from [`backend/scripts/local-db-setup.sql`](backend/scripts/local-db-setup.sql) (creates `app_owner`, the `sqlgenie` database, and `readonly_query_user`).
2. Start the app once (`./mvnw spring-boot:run`) so Flyway creates the `app` and `target` schemas.
3. Run the second block of the same script (as `app_owner`, connected to `sqlgenie`) to grant `readonly_query_user` read-only access to `target` only.

### Run

```bash
cd backend
./mvnw spring-boot:run
```

Backend starts on `http://localhost:8080`.

- Health check: `GET /actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- A dev-only admin account is seeded automatically (`admin@sqlgenie.dev` — see `application-dev.yml` for the password default; override via `ADMIN_SEED_PASSWORD`).
- The app boots without an API key, but natural-language-to-SQL calls need one: export `GROQ_API_KEY` before running. Uses [Groq](https://console.groq.com) (OpenAI-compatible endpoint, `llama-3.3-70b-versatile`) rather than OpenAI directly.

### Tests

```bash
./mvnw test              # unit tests only, no Docker required
./mvnw verify             # includes Testcontainers integration tests, requires a working Docker daemon
```

Integration tests spin up real PostgreSQL via Testcontainers and are run in CI (GitHub Actions); they don't require Docker on every contributor's machine.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Starts on `http://localhost:5173` by default. Talks to the backend at `http://localhost:8080`
unless `VITE_API_BASE_URL` is set (copy `.env.example` to `.env` to override) — make sure the
backend's `CORS_ALLOWED_ORIGINS` includes whatever origin the frontend actually runs on if you
change the port.

## Project Status

Building module by module — see the roadmap in [ARCHITECTURE.md](ARCHITECTURE.md).

- [x] Module 0 — Project foundation
- [x] Module 1 — JWT authentication
- [x] Module 2 — Query history & favorites data model
- [x] Module 3 — Natural language to SQL
- [x] Module 4 — SQL validation
- [x] Module 5 — SQL execution engine
- [x] Module 6 — SQL explanation
- [x] Module 7 — History & favorites API
- [x] Module 8 — Frontend
- [ ] Module 9 — Docker & CI/CD
- [ ] Module 10 — Azure deployment
