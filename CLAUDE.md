# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Start PostgreSQL (required before running the app) — Docker or Podman
cd stack && docker compose up -d      # Podman: podman compose up -d

# Build
./mvnw clean install

# Run the process-application module (http://localhost:8080) — the main module participants work in
cd services/process-application && ../../mvnw spring-boot:run

# Run a specific solution
cd solutions/exercise-01 && ../../mvnw spring-boot:run

# Load a reference solution into the process-application module (catch-up; valid: 01-10, two-digit)
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=<TestClassName>
```

CIB Seven Cockpit: `http://localhost:8080/webapp/#/seven/auth/start` (admin/admin)

## Architecture

Hexagonal architecture (ports & adapters) enforced at build time via ArchUnit tests:

```
REST / JavaDelegates           Application              CIB7 / Database
  (inbound adapters)   →   ports + services   →     (outbound adapters)
                               ↑
                            Domain
                        (engine-neutral)
```

**Package layout** under `src/main/java/io/miragon/training/`:

- `adapter/inbound/rest/` — Spring MVC REST controllers
- `adapter/inbound/cibseven/` — JavaDelegate implementations (`DelegateExpression`)
- `adapter/outbound/cibseven/` — Process engine adapter (start process instances, correlate messages)
- `adapter/outbound/db/` — JPA persistence adapter
- `application/port/inbound/` — Use case interfaces
- `application/port/outbound/` — Repository and process port interfaces
- `application/service/` — Use case implementations
- `domain/` — Pure Java domain model (records), no framework dependencies

## Key Technologies

- **CIB Seven** — Community distribution of Camunda Platform 7, runs embedded in Spring Boot
- **JavaDelegate** — Service tasks use `DelegateExpression` (e.g. `#{sendWelcomeMailDelegate}`) to bind to Spring beans
- **ArchUnit** — Architecture tests in `ArchitectureTest.java`

## Project Structure

Multi-module Maven project:
- `services/process-application/` — The main module participants work in. Ships in the Aufgabe-1 (Hybrid) state:
  full hexagonal skeleton present, but CIB deps/config/`@SpringBootApplication` (`TODO Exercise 1`)
  and the business-layer beans are commented out. The early ramp is gradual: Exercise 1 = switch the
  engine on + run the given start-form / Manual-Task model (no code); Exercise 2 = model a User Task
  with a self-made Generated Form (Cockpit only, still no Java); Exercise 3 = uncomment + implement the
  JavaDelegate (`TODO Exercise 3`, Cockpit start); Exercise 4 = uncomment the REST / persistence /
  message-correlation / confirm-endpoint layer (`TODO Exercise 4`).
- `templates/exercise-10/logistics-service/` — Aufgabe 10 starter for the remote-owner service, kept OUT of
  `services/` (and the default reactor) so `services/` stays clean for exercises 0–9. In Aufgabe 10 the
  participant copies it into `services/logistics-service` and adds the `<module>` line. It OWNS a small
  `sendWelcomeKit` process (Signal-Start → external task `shipWelcomeKit` → End), deploys its own BPMN into
  the engine at start-up, fulfils the task via the external-task client, and drives the engine via a typed
  client it **generates itself** (`openapi-generator-maven-plugin`, spec `cibseven-engine-rest-openapi`,
  package `org.cibseven.rest.client.*`). Runs on :8090. Ships dormant/compilable with `TODO Exercise 10`
  (the generator block + client wiring are commented out). Verified in CI via the `-Pexercise-10` profile.
  (Replaces the former `notification-service` / the separate `cibseven-engine-client` module.)
- `docs/` — Per-exercise instructions (`exercise-00.md … exercise-10.md`) + assets.
- `solutions/exercise-{01-10}/` + `solutions/extra-task-1/` — Cumulative solutions, each building on the previous.
  Exercise 10 is nested into two sub-services: `solutions/exercise-10/process-application/` (the generic
  engine host, which additively broadcasts `Signal_MemberActivated`) + `solutions/exercise-10/logistics-service/`
  (the remote owner of the `sendWelcomeKit` process; generates its own typed engine client in-module).
- `models/` — Reference BPMN/DMN models
- All modules (process-application + every solution) run on the same port (`8080`) and DB schema (`exercise`) —
  one module at a time. `stack/init-schemas.sql` creates just that one schema.
- The `load-solution` antrun task replaces `services/process-application/src/main` wholesale (Java, `application.yaml`,
  BPMN/DMN) from a solution; `src/test` and `pom.xml` are left untouched.

## Domain

All exercises use Miravelo Inner Circle membership (Membership naming) from Aufgabe 0. The BPMN file is
`membership.bpmn`, but the process key stays `subscribeNewsletter` and the start message stays
`Message_SubscriptionRequested` for historic reasons (mentioned once in Aufgabe 4).

Workflow: register → send confirmation mail → wait for confirmation (with timer retry/abort) → send welcome mail.

## Architecture Rules (ArchUnit)

Architecture constraints are verified at test time. Domain classes must have zero framework imports; adapters must only depend on application ports. Run `./mvnw test -Dtest=ArchitectureTest` to verify.
