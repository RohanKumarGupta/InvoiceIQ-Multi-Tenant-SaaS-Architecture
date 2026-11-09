# InvoiceIQ

InvoiceIQ is a multi-tenant invoice processing application built with Spring Boot and React. It provides a starting point for securely uploading invoices, extracting invoice data, validating tax details, and routing records through a review workflow.

## Features

- Tenant-aware accounts, authentication, and member roles
- Invoice upload for PDF, PNG, and JPEG documents
- AI-assisted invoice and line-item extraction
- Validation rules for tax calculations, GSTIN checksums, duplicate invoices, dates, and vendor anomalies
- Invoice review actions, validation results, audit history, and AI usage records
- PostgreSQL persistence, Redis, RabbitMQ-based processing, and MinIO object storage
- React dashboard, invoice list, signup/login, and upload screens

## Technology

| Area                    | Technologies                                               |
| ----------------------- | ---------------------------------------------------------- |
| Backend                 | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA |
| Database and migrations | PostgreSQL 16, Flyway                                      |
| Messaging and cache     | RabbitMQ 3.13, Redis 7                                     |
| Object storage          | MinIO                                                      |
| Frontend                | React 18, TypeScript, Vite, Tailwind CSS                   |
| API documentation       | OpenAPI / Swagger UI                                       |

## Requirements

- Java 21
- Maven 3.9 or later
- Node.js 18 or later and npm
- Docker with the Docker Compose plugin

## Run Locally

The Compose file starts the supporting services only. Run the backend and frontend in separate terminals.

1. Create a local environment file and start the services from the repository root:

   ```powershell
   Copy-Item .env.example .env
   docker compose up -d
   ```

2. Start the backend in another terminal:

   ```powershell
   Set-Location backend
   mvn spring-boot:run
   ```

   The backend listens on <http://localhost:8080>. Its default local connection settings match the values in `.env.example`. The root `.env` file is read by Docker Compose; Spring Boot does not load that file automatically. Set any backend-specific overrides in the backend process environment.

3. Install frontend dependencies and start Vite in a third terminal:

   ```powershell
   Set-Location frontend
   npm ci
   npm run dev
   ```

   Open <http://localhost:5173>. Vite proxies `/api` requests to the backend at `http://localhost:8080`.

To stop the supporting services, run `docker compose down` from the repository root. Add `-v` only when you also want to delete the local database, Redis, RabbitMQ, and MinIO volumes.

### Environment Variables

`.env.example` documents the local service settings. The main configurable values are:

| Variable            | Purpose                                         |
| ------------------- | ----------------------------------------------- |
| `POSTGRES_*`        | PostgreSQL database, credentials, and host port |
| `REDIS_*`           | Redis password and host port                    |
| `RABBITMQ_*`        | RabbitMQ credentials and AMQP/management ports  |
| `MINIO_*`           | MinIO credentials and API/console ports         |
| `JWT_SECRET`        | Signing key for access tokens                   |
| `ANTHROPIC_API_KEY` | Optional API key for AI extraction              |
| `APP_PORT`          | Backend HTTP port                               |

The values in `.env.example` are for local development only. Replace all default credentials and use a securely generated JWT secret before deploying or exposing services outside your machine. Set `ANTHROPIC_API_KEY` in the backend process environment to enable calls to Anthropic.

## Services and Ports

| Service                | Default address                         |
| ---------------------- | --------------------------------------- |
| Frontend               | <http://localhost:5173>                 |
| Backend API            | <http://localhost:8080>                 |
| Swagger UI             | <http://localhost:8080/swagger-ui.html> |
| OpenAPI document       | <http://localhost:8080/api-docs>        |
| PostgreSQL             | `localhost:5432`                        |
| Redis                  | `localhost:6379`                        |
| RabbitMQ AMQP          | `localhost:5672`                        |
| RabbitMQ management UI | <http://localhost:15672>                |
| MinIO API              | <http://localhost:9000>                 |
| MinIO console          | <http://localhost:9001>                 |

The backend API is versioned under `/api/v1`. Authentication endpoints are under `/api/v1/auth`; invoice operations are under `/api/v1/invoices`; current-tenant operations are under `/api/v1/tenants/current`.

## Build and Test

Run these commands from the corresponding project directory:

```powershell
# Backend
Set-Location backend
mvn test

# Frontend (from the repository root)
Set-Location frontend
npm run build
```

## Project Layout

```text
backend/    Spring Boot API, migrations, and Dockerfile
frontend/   React application
docker-compose.yml
.env.example
```

## Development Notes

The frontend includes an offline demo fallback when the API cannot be reached. The dashboard also includes sample chart and empty-state values. Invoice review, reports, and settings routes are currently placeholders; consult the application source and OpenAPI documentation for the implemented API surface.
