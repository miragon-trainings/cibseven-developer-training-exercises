# CIB Seven Developer Training – Exercises

> [🇩🇪 Deutsch](README.de.md) · 🇬🇧 **English**

Welcome to the CIB Seven Developer Training!

**Miravelo** is a lifestyle online shop for people in a quarterlife crisis – portafilter machines,
running gear, gravel bikes, road bikes. The company is growing, the customer base is growing,
and the processes have to keep up.

In this module you work your way step by step through 7 exercises that build a complete
Inner Circle membership process on top of CIB Seven (Camunda Platform 7).

## The complete target process

Here is what the process looks like at the end of Exercise 7 – with all the concepts you build up step by step:

![Complete membership process](../../docs/assets/exercise-07-main.svg)

The extracted sub-process for the rejection (Call Activity + DMN):

![Membership rejection sub-process](../../docs/assets/exercise-07-sub.svg)

## Quick Start

The engine uses an embedded H2 database that is stored as a file under `~/.cibseven-training` –
there is no database to install or start. The data survives a restart; for a clean slate, stop the
application and delete the folder `~/.cibseven-training` (on Windows `%USERPROFILE%\.cibseven-training`).

```bash
# Start the application (from this process-application directory)
../../mvnw spring-boot:run

# CIB Seven Cockpit
http://localhost:8080/webapp/#/seven/auth/start  (admin / admin)

# H2 console (engine tables) – replace the pre-filled JDBC URL jdbc:h2:~/test in the login form
http://localhost:8080/h2-console  (JDBC URL jdbc:h2:file:~/.cibseven-training/exercise, user sa, no password)
```

> On delivery this module starts in the state of **Exercise 1** – the
> CIB Seven engine is still commented out. In Exercise 1 you switch it on.

## Exercise overview

| Exercise | Topic | Description |
|---|---|---|
| [0](../../docs/en/exercise-00.md) | Business-level BPMN modeling | Model the core Inner Circle membership flow purely at the business level, using only the shapes from Chapter 1 — the starting point for the whole training |
| [1](../../docs/en/exercise-01.md) | Getting the engine running | Run the given start-form / Manual-Task model end-to-end, get to know the Cockpit and the engine's `act_*` tables |
| [2](../../docs/en/exercise-02.md) | User Task and Service Task | Turn "Confirm" into a User Task with a self-made Generated Form and "Send welcome mail" into a Service Task with a JavaDelegate (started from the Cockpit) |
| [3](../../docs/en/exercise-03.md) | The application takes over | Message start event, business key, REST endpoints for register + confirm, correlation, persistence, capacity check with an exclusive gateway |
| [4](../../docs/en/exercise-04.md) | Process tests | Transaction boundaries, process unit test with an in-memory engine and mocked use cases, without a running application |
| [4 · Add-on](../../docs/en/exercise-04-addon.md) | bpmn-to-code | Element IDs as generated constants instead of hand-typed strings, compile-safe path navigation with `PathWalk` |
| [5](../../docs/en/exercise-05.md) | Subprocess, boundary events & parallelism | Subprocess, timer and message boundary events, parallel gateway, Teams integration |
| [6](../../docs/en/exercise-06.md) | Compensation, call activity & DMN | Compensation boundary event, compensating end event, call activity, DMN decision table, business rule task |
| [7](../../docs/en/exercise-07.md) | Remote engine as shared infrastructure | A department owns its **own** small process (`sendWelcomeKit`) in its remote service: model, worker, deployment and tests live there; triggered by a signal broadcast; the engine is driven through a generated OpenAPI client |
| [Extra 1](../../docs/en/extra-task-1.md) | Process Engine API | Rebuild Exercise 7 engine-neutrally: workers instead of JavaDelegates, swapping adapters instead of engine lock-in |

## Architecture

The project follows the **hexagonal architecture** (Ports & Adapters):

```
REST / CIB7 Delegates     Application              CIB7 / Database
  (inbound adapters)  →  ports + services  →     (outbound adapters)
                              ↑
                           Domain
                     (engine-neutral)
```

**Packages under `src/main/java/io/miragon/training/`:**

- `adapter/inbound/rest/` – Spring MVC REST controllers
- `adapter/inbound/cibseven/` – JavaDelegate implementations (`BaseDelegate`)
- `adapter/outbound/cibseven/` – Process adapter (start process, correlate messages)
- `adapter/outbound/db/` – JPA persistence adapter
- `application/port/inbound/` – Use case interfaces
- `application/port/outbound/` – Repository and process port interfaces
- `application/service/` – Use case implementations
- `domain/` – Pure Java domain model, no framework dependencies

## Architecture tests

```bash
../../mvnw test -Dtest=ArchitectureTest
```

The ArchUnit tests verify at test time that the architecture rules are being observed.

## Solutions

For every exercise there is a reference solution under `../../solutions/exercise-X/`.
Each solution is a self-contained, runnable Spring Boot application.

If you don't quite manage to finish an exercise, you can load the reference solution directly into
this module and continue working with it (valid values: 00–07, two digits; 00 = back to the starter state):

```bash
../../mvnw antrun:run@load-solution -Dsolution=02
```

The task resets `src/` and `pom.xml` to the starter state and copies the solution on top: `src/main`
(Java, `application.yaml`, BPMN/DMN), the process tests and the matching `pom.xml` (e.g. with the
CIB Seven starters from Exercise 1). The stubs of the later exercises stay in place, `ArchitectureTest`
stays as it is. Your own changes in `src/` and `pom.xml` are replaced – the task first backs them up to
`.load-solution-backup/<timestamp>/` in this module. Then run Maven again (IntelliJ: reload the Maven
project), because the `pom.xml` has changed. If you went back to an earlier exercise, stop the
application and delete the folder `~/.cibseven-training`.

All modules run on the same port (`8080`) and the same embedded H2 database file, so only
ever one module runs at a time (the running application locks the file, a second instance fails at
startup).
