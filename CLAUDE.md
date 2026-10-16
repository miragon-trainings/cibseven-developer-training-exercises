# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
./mvnw clean install

# Run the process-application module (http://localhost:8080) — the main module participants work in
cd services/process-application && ../../mvnw spring-boot:run

# Run a specific solution
cd solutions/exercise-01 && ../../mvnw spring-boot:run

# Load a reference solution into the process-application module (catch-up; valid: 00-07, two-digit; 00 = starter)
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=<TestClassName>
```

CIB Seven Cockpit: `http://localhost:8080/webapp/#/seven/auth/start` (admin/admin)

H2 console: `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:file:~/.cibseven-training/exercise`, user `sa`,
empty password (the login form pre-fills `jdbc:h2:~/test`; replace it).

**Database:** nothing to install or start. The app runs the engine on an embedded, file-based H2
(`~/.cibseven-training/exercise.mv.db`), created on first start; the engine creates its `act_*` tables itself
(`camunda.bpm.database.schema-update: true`). State survives restarts; reset = stop the app and delete
`~/.cibseven-training` (Windows: `%USERPROFILE%\.cibseven-training`).
The running app locks the file — external DB tools cannot open it meanwhile (use the H2 console). Process tests
(Exercise 4+) use the `test` profile with a separate in-memory H2 (`jdbc:h2:mem:cibseven-test`).

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
  with a self-made Generated Form + uncomment/implement the JavaDelegate (`TODO Exercise 2`, Cockpit
  start); Exercise 3 = uncomment the REST / persistence / message-correlation / confirm-endpoint /
  capacity-gateway layer and implement its engine stubs (`TODO Exercise 3`). The confirm endpoint
  (`POST /api/memberships/{id}/confirm`, `TaskService` query by business key) stays in every later
  solution; from Exercise 3 on the confirm User Task has no form.
- `templates/exercise-07/logistics-service/` — Aufgabe 7 starter for the remote-owner service, kept OUT of
  `services/` (and the default reactor) so `services/` stays clean for exercises 0–6. In Aufgabe 7 the
  participant copies it into `services/logistics-service` and adds the `<module>` line. It OWNS a small
  `sendWelcomeKit` process (Signal-Start → external task `shipWelcomeKit` → End). Engine access comes from the
  CIB seven community REST client (`org.cibseven.community.rest-client:cibseven-engine-rest-client-complete-springboot-starter`,
  package `org.cibseven.community.rest.client.*`): typed `ProcessDefinitionApi` beans, the external-task client,
  and auto-deployment of its own BPMN at start-up (`cibseven.auto-deploy.*`; engine must be up first).
  Runs on :8090. Ships dormant/compilable with `TODO Exercise 7` (bpmn-to-code block commented out, worker +
  process adapter stubbed). Verified in CI via the `-Pexercise-07` profile.
  (Replaces the former `notification-service` / the separate `cibseven-engine-client` module.)
- `templates/process-application-starter/` — Byte copy of the starter state of `services/process-application`
  (`src/` + `pom.xml`), outside the reactor; the baseline `load-solution` resets to. **Contract:** must stay
  byte-identical to the work module (CI: `diff -r`). After changing the starter, sync it:
  `rm -rf templates/process-application-starter/src && cp -R services/process-application/src templates/process-application-starter/ && cp services/process-application/pom.xml templates/process-application-starter/`
- `docs/` — Per-exercise instructions (`exercise-00.md … exercise-07.md`, `exercise-04-addon.md`, `extra-task-1.md`) + assets.
- `solutions/exercise-{01-07}/` + `solutions/extra-task-1/` — Cumulative solutions, each building on the previous.
  Extra Task 1 is nested like Exercise 7: `process-application/` (process-engine-api via the CIB seven embedded
  adapter) + `logistics-service/` (process-engine-api via the CIB seven remote adapter; deploys its model itself
  through the `DeploymentApi`, since the worker library's auto-deployment doesn't trigger with the remote adapter).
  Exercise 7 is nested into two sub-services: `solutions/exercise-07/process-application/` (the generic
  engine host, which additively broadcasts `Signal_MemberActivated`) + `solutions/exercise-07/logistics-service/`
  (the remote owner of the `sendWelcomeKit` process; uses the CIB seven community REST client).
- `models/` — Reference BPMN/DMN models
- All modules (process-application + every solution) run on the same port (`8080`) and the same H2 file
  (`~/.cibseven-training/exercise.mv.db`) — one module at a time; a second instance fails at startup with
  "Database may be already in use".
- `load-solution` (`-Dsolution=NN`, 00–07; 00 = starter) puts `services/process-application` into the "Exercise NN
  solved" state, deterministically (same NN → same `src/` + `pom.xml`, whatever was loaded before): backs up `src/` +
  `pom.xml` to `services/process-application/.load-solution-backup/<timestamp>/` (git-ignored); resets `src/main`,
  `src/test/java/.../process` and `src/test/resources` to the starter template, leaving out every file containing
  `TODO Exercise M` with M ≤ NN; copies solution NN on top (`src/main`, process tests, test resources); rebuilds
  `pom.xml` = starter pom with the `@exercise-N` blocks for N ≤ NN uncommented; deletes `target/`. `ArchitectureTest`
  is never touched. Afterwards: reload Maven; after going back, delete `~/.cibseven-training`. No catch-up for
  `extra-task-1` and the `logistics-service` half of Exercise 7.
  - **Contract:** `TODO Exercise N` in a starter file under `src/` is load-bearing — solution N and every later
    solution must contain that file, unless it is dropped on purpose. Conversely,
    an unmarked starter file is always restored (a solution can only overwrite it), so a file a solution deletes or
    renames needs a marker. A new exercise number needs a new `solvedExercises` condition in the Ant task and in
    `validate_nn` of `.github/scripts/check-load-solution.sh`; the last exercise is nested (special case in both).
  - Prepared pom blocks: every dependency/plugin a solution pom adds must exist in the starter pom as one, or
    `load-solution` won't bring it. Format: a `TODO Exercise N` comment for participants, then
    `<!-- @exercise-N[-suffix]:begin – … -->`, `<!--` on its own line, the XML, `-->` on its own line,
    `<!-- @exercise-N[-suffix]:end -->`. Existing: `@exercise-1` (CIB Seven starters), `@exercise-4`
    (`cibseven-bpm-assert`), `@exercise-4-addon` ×2 (`bpmn-to-code-runtime`, `bpmn-to-code-maven` plugin).

## Domain

All exercises use Miravelo Inner Circle membership (Membership naming) from Aufgabe 0. The BPMN file is
`membership.bpmn`, but the process key stays `subscribeNewsletter` and the start message stays
`Message_SubscriptionRequested` for historic reasons (mentioned once where introduced: key in Aufgabe 2, message in Aufgabe 3).

Workflow: register → send confirmation mail → wait for confirmation (with timer retry/abort) → send welcome mail.

## Architecture Rules (ArchUnit)

Architecture constraints are verified at test time. Domain classes must have zero framework imports; adapters must only depend on application ports. Run `./mvnw test -Dtest=ArchitectureTest` to verify.
