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

Run [`backend/scripts/local-db-setup.sql`](backend/scripts/local-db-setup.sql) as a superuser
(e.g. `psql -U postgres`) — creates `app_owner`, the `sqlgenie` database, and
`readonly_query_user`. Its actual read-only grants are applied automatically by a Flyway
migration the first time the app starts, so there's nothing further to run manually.

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

### Full stack via Docker Compose

```bash
export GROQ_API_KEY=your-key-here   # optional - stack still boots without it
docker compose up --build
```

Starts Postgres (with `readonly_query_user` created automatically), the backend on
`http://localhost:8080`, and the frontend on `http://localhost:5173`. CI builds and
smoke-tests this exact stack on every push that touches backend, frontend, or Docker config.

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
- [x] Module 9 — Docker & CI/CD
- [ ] Module 10 — Azure deployment
