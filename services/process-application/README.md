# CIB Seven Developer Training – Exercises

> [🇩🇪 Deutsch](README.de.md) · 🇬🇧 **English**

Welcome to the CIB Seven Developer Training!

**Miravelo** is a lifestyle online shop for people in a quarterlife crisis – portafilter machines,
running gear, gravel bikes, road bikes. The company is growing, the customer base is growing,
and the processes have to keep up.

In this module you work your way step by step through 10 exercises that build a complete
Inner Circle membership process on top of CIB Seven (Camunda Platform 7).

## The complete target process

Here is what the process looks like at the end of Exercise 10 – with all the concepts you build up step by step:

![Complete membership process](../../docs/assets/exercise-10-main.svg)

The extracted sub-process for the rejection (Call Activity + DMN):

![Membership rejection sub-process](../../docs/assets/exercise-10-sub.svg)

## Prerequisites

```bash
# Start PostgreSQL and MailHog (in the stack directory; Docker or Podman)
cd ../../stack && docker compose up -d      # Podman: podman compose up -d

# Start the application (from this process-application directory)
../../mvnw spring-boot:run

# CIB Seven Cockpit
http://localhost:8080/webapp/#/seven/auth/start  (admin / admin)
```

> On delivery this module starts in the state of **Exercise 1** – the
> CIB Seven engine is still commented out. In Exercise 1 you switch it on.

## Exercise overview

| Exercise | Topic | Description |
|---|---|---|
| [0](../../docs/en/exercise-00.md) | Business BPMN modeling | Create the whole target process purely on the business level with the Miragon BPMN Modeler |
| [1](../../docs/en/exercise-01.md) | Engine & tooling | Run the given start-form / Manual-Task model, get to know the Cockpit & DB tables |
| [2](../../docs/en/exercise-02.md) | The first wait state | Turn the "Confirm" Manual Task into a User Task with a self-made Generated Form |
| [3](../../docs/en/exercise-03.md) | Automate a step | Turn the "Send welcome mail" Manual Task into a Service Task + JavaDelegate (Cockpit-started) |
| [4](../../docs/en/exercise-04.md) | The application takes over | Message start, REST register + confirm endpoints, correlation, persistence |
| [5](../../docs/en/exercise-05.md) | Membership & gateway | Exclusive gateway, capacity check |
| [6](../../docs/en/exercise-06.md) | Process tests | Process unit test: in-memory engine, mocked use cases, without PostgreSQL |
| [6 · Add-on](../../docs/en/exercise-06-addon.md) | bpmn-to-code | Generate a type-safe process API from the BPMN – strings out, constants in |
| [7](../../docs/en/exercise-07.md) | Boundary events & subprocesses | Parallel gateway, timer and message boundary events, subprocesses |
| [8](../../docs/en/exercise-08.md) | Compensation (SAGA) | Automatic rollback via BPMN compensation |
| [9](../../docs/en/exercise-09.md) | Call Activity & DMN | Process modularization with decision tables |
| [10](../../docs/en/exercise-10.md) | Remote engine & external task | Extract the notify-community delegate as an external task into a dedicated remote worker, notification into a Teams channel |
| [Extra 1](../../docs/en/extra-task-1.md) | Process engine API | Break the engine lock-in: workers instead of delegates, engine-neutral adapter layer |

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

If you don't quite manage to finish an exercise, you can copy the reference solution directly into
this module and continue working with it (valid values: 1–10):

```bash
../../mvnw antrun:run@load-solution -Dsolution=02
```

The task replaces `src/main` completely (Java, `application.yaml`, BPMN/DMN); `src/test` stays
untouched. All modules run on the same port (`8080`) and DB schema (`exercise`), so only ever
one module runs at a time. The prerequisite is that you enabled the CIB Seven dependencies in
**Exercise 1** (the `pom.xml` is not copied along).
