# CIB Seven Developer Training Exercises

> [🇩🇪 Deutsch](README.de.md) · 🇬🇧 **English**

Hands-on exercises for the CIB Seven Developer Training. The project implements the Miravelo Inner Circle membership process with CIB Seven as the process engine and a hexagonal architecture that decouples business logic from infrastructure.

## Exercises

### Background: Miravelo

**Miravelo** is an online shop for premium bicycles — gravel bikes for long
weekend tours, road bikes for everyone who likes going fast on the asphalt. Its customers
are young, brand-conscious, and pretty passionate.

The shop is growing, and new products keep arriving. The team decides:
let's build a **newsletter** so customers stay informed about product launches and
exclusive offers. Someone signs up, gets a welcome mail — done.

> *"Surely that's built in an hour."*
> — Every developer who has ever underestimated a newsletter.

The training takes place in the context of the exclusive **Miravelo Inner Circle** — a membership
limited to a thousand seats for the most loyal customers. You first model the whole target process
at the business level, then automate it step by step.

What follows is a journey through increasingly complex BPMN patterns: gateways, boundary events,
subprocesses, parallel gateways, call activities, DMN decision tables, and compensation —
each exercise builds on the previous one.

![Process model](docs/newsletter-subscription.png)

### Exercise overview

Detailed exercise descriptions can be found in [`docs/`](docs/).

| Exercise | Topic | Description |
|---|---|---|
| [0](docs/en/exercise-00.md) | Business-level BPMN modeling | Model the complete Inner Circle membership process at the business level — the shared target for the whole training |
| [1](docs/en/exercise-01.md) | Getting the engine running | Run the given start-form / Manual-Task model end-to-end, get to know the Cockpit and the engine's `act_*` tables |
| [2](docs/en/exercise-02.md) | The first wait state | Turn the "Confirm" Manual Task into a User Task and give it a self-made Generated Form |
| [3](docs/en/exercise-03.md) | Automate a step | Turn the "Send welcome mail" Manual Task into a Service Task backed by a JavaDelegate (Cockpit-started) |
| [4](docs/en/exercise-04.md) | The application takes over | Message start event, REST register + confirm endpoints, message correlation, persistence |
| [5](docs/en/exercise-05.md) | Capacity check with a gateway | Exclusive gateway, business key, task form |
| [6](docs/en/exercise-06.md) | Process tests | Transaction boundaries, process unit test with an in-memory engine, mocked use cases, without PostgreSQL |
| [6 · Add-on](docs/en/exercise-06-addon.md) | bpmn-to-code | Element IDs as generated constants instead of hand-typed strings |
| [7](docs/en/exercise-07.md) | Subprocess, boundary events & parallelism | Subprocess, timer and message boundary events, parallel gateway, Teams integration |
| [8](docs/en/exercise-08.md) | Compensation (SAGA) | Compensation boundary event, compensating end event, compensation handler |
| [9](docs/en/exercise-09.md) | Call activity & DMN | Call activity, DMN decision table, business rule task |
| [10](docs/en/exercise-10.md) | Remote engine as shared infrastructure | One department owns its **own** small process (`sendWelcomeKit`) in its remote service: model, worker, deployment, and tests live there; triggered via signal broadcast; the engine driven through a generated OpenAPI client |
| [Extra 1](docs/en/extra-task-1.md) | Process engine API | Rebuild Exercise 10 to be engine-neutral: worker instead of JavaDelegate, swapping adapters instead of engine lock-in |

> The structure, language, and quality criteria of the exercises are captured in
> [`docs/aufgaben-template.md`](docs/aufgaben-template.md) (German). New or changed
> exercises are cross-checked with the `aufgaben-review` skill
> (`.claude/skills/aufgaben-review/SKILL.md`).

## Quick Start

```bash
# Start PostgreSQL + MailHog (Docker or Podman)
cd stack && docker compose up -d      # Podman: podman compose up -d

# Build everything
./mvnw clean install

# Start the process-application module (the one module where you work on all exercises)
cd services/process-application && ../../mvnw spring-boot:run

# CIB Seven Cockpit
open http://localhost:8080/webapp/#/seven/auth/start    # admin / admin
```

### Loading an exercise solution

The `process-application` module starts in the state of **Exercise 1** (engine still commented out). If you
can't fully finish an exercise, you can copy the reference solution into your `process-application` module
and continue working with it:

```bash
# Copy solutions/exercise-02 into the process-application module (valid values: 01–10, two-digit)
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

The task replaces `src/main` completely (Java, `application.yaml`, BPMN/DMN); `src/test` stays
untouched. All modules – the `process-application` module **and** all solutions – run on the same port
(`8080`) and the same DB schema (`exercise`); so only **one** module runs at a time.
The CIB Seven dependencies activated in **Exercise 1** (`pom.xml`) remain in place – so only load
a solution from `exercise-2` onward after Exercise 1 is complete.

## Repository structure

```
cibseven-developer-training-exercises/
├── docs/                             # Exercise descriptions: docs/de/ (German) + docs/en/ (English) + assets
├── services/                         # The services you work on
│   ├── process-application/          # Process application (starts in the state of Exercise 1)
│   │   └── src/main/java/io/miragon/training/
│   │       ├── adapter/
│   │       │   ├── inbound/
│   │       │   │   ├── cibseven/     # JavaDelegate implementations
│   │       │   │   └── rest/         # REST controllers
│   │       │   └── outbound/
│   │       │       ├── cibseven/     # Process engine adapter (start/correlation)
│   │       │       └── db/           # JPA persistence adapter
│   │       ├── application/
│   │       │   ├── port/
│   │       │   │   ├── inbound/      # Use-case interfaces
│   │       │   │   └── outbound/     # Repository and process port interfaces
│   │       │   └── service/          # Use-case implementations
│   │       └── domain/               # Domain model (pure Java, no framework dependencies)
│   └── (logistics-service/)          # created from templates/ only in Exercise 10 (remote owner)
├── templates/
│   └── exercise-10/logistics-service/ # Template for the Exercise 10 worker (copied into services/)
├── solutions/                        # Cumulative solutions per exercise (exercise-01 … exercise-10, extra-task-1)
│   ├── exercise-{01-10}/             # exercise-10/ is nested: process-application/ + logistics-service/
│   └── extra-task-1/
├── models/                           # Reference BPMN/DMN models
├── stack/
│   ├── docker-compose.yml            # PostgreSQL + MailHog (Docker or Podman)
│   └── init-schemas.sql
└── pom.xml
```

## Technology stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Process Engine | CIB Seven 2.2.0 |
| Database | PostgreSQL (JPA / Hibernate) |
| Build | Maven |
| Architecture tests | ArchUnit |

## CIB Seven

[CIB Seven](https://cibseven.org) is a community-maintained distribution of Camunda Platform 7. It offers full compatibility with the Camunda 7 API and is developed further independently as open source.

In this project, CIB Seven runs embedded in Spring Boot, provides the Camunda web application at `http://localhost:8080/webapp/#/seven/auth/start`, and handles the BPMN process execution for the Inner Circle membership process.

Service tasks are wired up via the `JavaDelegate` pattern with `DelegateExpression`:

```java
@Component
public class SendWelcomeMailDelegate extends BaseDelegate {

    private final SendWelcomeMailUseCase useCase;

    public SendWelcomeMailDelegate(SendWelcomeMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var subscriptionId = (String) execution.getVariable("subscriptionId");
        useCase.sendWelcomeMail(new SubscriptionId(UUID.fromString(subscriptionId)));
    }
}
```

## Architecture

The project follows a **hexagonal architecture** (ports & adapters):

```
REST / JavaDelegates           Application              CIB7 / Database
  (Inbound adapters)   →   Ports + Services   →     (Outbound adapters)
                               ↑
                            Domain
                        (engine-neutral)
```

Architecture rules are enforced at build time via [ArchUnit](https://www.archunit.org/) tests.
