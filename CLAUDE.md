# HR CRM Project - Claude Settings

## Project Overview 
This is a Spring Boot microservices HR CRM system for recruitment agencies.
It uses Claude API for AI features (lead scoring, call summaries, resume parsing).

- Parent Maven project with 5 microservices
- Each service is independent, owns its own DB schema
- Services communicate via REST (no shared DB joins across services)
- All ports: gateway=8080, lead=8081, ai=8082, calling=8083, whatsapp=8084, ops=8085

## Non-negotiable conventions
- Java 17, Spring Boot 3.2.x
- Constructor injection ONLY — no @Autowired on fields, ever
- All entities use Long id with @GeneratedValue(strategy = IDENTITY)
- Flyway for DB migrations — files named V{n}__{description}.sql
- DTOs separate from entities — never return JPA entities from controllers
- Global exception handler in every service — no raw 500s to clients
- Every public service method needs a unit test
- add logger on every service class, log at INFO level for important events, DEBUG for details
- all inter service communication is via fiengt clients, no RestTemplate or WebClient calls directly in service classes

## Architecture
- `hr-gateway/` — Spring Cloud Gateway, JWT validation
- `hr-lead-service/` — Lead CRUD, pipeline management (port 8081)
- `hr-ai-service/` — All Claude API integrations (port 8082)
- `hr-calling-service/` — Twilio integration, call masking (port 8083)
- `hr-whatsapp-service/` — Meta WhatsApp API (port 8084)
- `hr-ops-service/` — Employees, payslips, leave (port 8085)

## Database
- h2 for development, PostgreSQL for production
- PostgreSQL 16, each service has its own schema
- Naming: snake_case tables, e.g. `lead_pipeline`, `call_recordings`
- Migrations: Flyway, files in `src/main/resources/db/migration/`
- Never write raw SQL — use Spring Data JPA repositories

## Testing rules
- Every service endpoint needs an integration test using @SpringBootTest
- Mock the Claude API calls using WireMock in tests
- Run `./mvnw test` before asking me to review any PRs

## Environment variables
- `ANTHROPIC_API_KEY` — Claude API key (never hardcode, never log)
- `DB_URL`, `DB_USER`, `DB_PASS` — PostgreSQL connection
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` — calling service
- `WHATSAPP_TOKEN` — Meta WhatsApp API token

## What NOT to do
- Do not create new services without updating the parent pom.xml
- Do not use `@Autowired` field injection — use constructors
- Do not add dependencies without checking if they conflict with Spring BOM
- Do not call the Claude API synchronously in a web request thread — use async/reactive                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       mn  