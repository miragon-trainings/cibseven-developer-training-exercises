# Exercise 6 – Securing the process with tests

> **Prerequisite:** Exercise 5 is complete – the gateway, the capacity check, and both process outcomes are working.
> **Working directory:** `services/process-application`
> **New in this exercise:** transaction boundaries (`asyncBefore`/`asyncAfter`), in-memory engine with h2, the job executor turned off, `@MockitoBean`, assertions with `BpmnAwareTests`.

## What this is about

**Monday morning. The process is running. Supposedly.**

You've built a solid process: gateway, confirmation mail, rejection. In the
Cockpit demo everything worked – once. But how do you know it will **still** work
next week, when someone attaches a boundary event, reroutes a sequence flow, or
flips a condition?

Are you going to click through the Cockpit every single time? Start PostgreSQL, fire off curl calls,
read logs? Nobody does that reliably. That's exactly where processes die quietly: a sequence flow
points into the void after a refactoring, the gateway takes the wrong path – and nobody notices
until someone gets a rejection despite a free spot.

> *"Works on my machine" is not a test strategy. It's an excuse with better PR.*

A **process test** starts the real process in an in-memory engine, lets the real
delegates run, and only replaces the business logic behind them with mocks. Which path the
process instance has to take is then written down as an **assertion** in the test – verifiable on every
build instead of just once in a demo.

## Learning goals

After this exercise you can

- deliberately set **transaction boundaries** and explain why a non-repeatable
  step must commit before an external effect,
- secure a process as a unit test, without PostgreSQL and without any running infrastructure,
- run the engine in the test on h2 and with the job executor turned off,
- mock the use cases behind the delegates in a targeted way using `@MockitoBean`,
- execute the async continuations yourself in the test and thereby drive the instance in a controlled way up to
  the next wait state,
- check process paths with `BpmnAwareTests` (`isWaitingAt`, `hasPassedInOrder`, `isEnded`).

## Target model

![BPMN model of the exercise](../assets/exercise-06.svg)

There are **no new model elements**. You add the transaction boundaries (Step 1) to the
existing process from Exercise 5 and then secure it with tests: Message Start →
Claim → Gateway → confirmation → welcome mail, respectively rejection.

Reference model (the Exercise 5 process with the transaction boundaries added):
`../../models/exercise-06/membership.bpmn`

## The task

### 1. Set transaction boundaries

> Theory for this: training chapter **"Async & Transaction Boundaries"** (Topic 4, *Execution
> Resilience*) – save points, default and manual boundaries, rollback in action. This is
> the first place where you apply it.

Before you secure the process with tests, you make it resilient. Until now it ran completely
**synchronously**. Starting with this model you set transaction boundaries – in two steps.

**a) Boundaries at the wait states.** The engine commits automatically at every wait state –
at a User Task it has to persist the state anyway. Everywhere else you set the boundary
yourself, with an **asynchronous continuation**: the markers `asyncBefore` and `asyncAfter`
tell the engine to commit at this point, create a job, and continue the work afterwards in a
**new** transaction.

Add the two continuations that are missing here:

- `asyncBefore` on the Message Start Event `startEvent_submitRegistration` – a clean boundary
  after correlation; the `correlateMessage` call only creates the instance and returns.
- `asyncAfter` on the User Task `userTask_confirmMembership` – the completion commits immediately.
  Otherwise the completion **and** the downstream Service Task run in **one** transaction:
  if it throws, the completion rolls back with it and the task reappears in the tasklist.

**b) Boundaries at the Service Tasks.** With `claimMembership` there is, for the first time, a
**non-repeatable** step – the spot reservation – directly before a mail send. Between the
Message Start and the User Task there is **no** wait state; without further markers,
`claimMembership` and `sendConfirmationMail` therefore run in **one** engine transaction.

If the mail send throws an exception, the engine rolls back the *entire* transaction and
re-executes the job. Result: `claimMembership` runs a second time – a double-reserved spot,
even though only the mail send failed.

**Rule:** Separate the *non-repeatable* work from the *external, non-rollbackable*
effect with its own transaction boundary. Set `asyncBefore` on every Service Task with an
external effect:

| Marker | Element | Why |
|---|---|---|
| `asyncBefore` | `serviceTask_sendConfirmationMail` | commits the reservation first; a mail failure only retries the send |
| `asyncBefore` | `serviceTask_sendRejectionMail` | otherwise sits in the same transaction as `claimMembership` |
| `asyncBefore` | `serviceTask_sendWelcomeMail` | consistency; from Exercise 7 on it also matters on a parallel branch |

`claimMembership` deliberately gets **no** marker – it should commit early, together with
the token that advances in the model (the *token* is the imagined game piece that marks the
current state of an instance in the process model). The marker belongs on the *downstream*
call, which would otherwise roll back the reservation with it. In the modeler: select the
element → Properties Panel → *Asynchronous Before*.

### 2. Add the test dependency

The assertions come from the CIB Seven port of `camunda-bpm-assert`. The version is
managed centrally in the root `pom.xml`:

```xml
<dependency>
    <groupId>org.cibseven.bpm</groupId>
    <artifactId>cibseven-bpm-assert</artifactId>
    <scope>test</scope>
</dependency>
```

`spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) and `h2` are already present.

### 3. Create the test profile

**New file:** `src/test/resources/application-test.yaml`

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    url: jdbc:h2:mem:cibseven-test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS exercise
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        default_schema: exercise

camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
    database:
      type: h2
      schema-update: true
    job-execution:
      enabled: false   # <-- the key point: we run the async continuations ourselves
    webapp:
      enabled: false

# The webapp bean validates this secret at start-up, even when the webapp is off:
cibseven:
  webclient:
    authentication:
      jwtSecret: M9nU3ORo3s+gK23D9mO5I2h+EIqnosCFDCJi+2bKoulKqZkeQT8pGYg5RhuORlf/fWhLu5meC/SPZCv9NNuj6SK/vE5Sid04UQGrnyh04EpBdiAosAO91xezjgmbSeALUtneibseGpS0tNE4RvLIl+gXiAKqNXyO
```

> **Term: job executor.** The engine's background thread. It picks up the jobs that
> arise from an asynchronous continuation (`asyncBefore` / `asyncAfter`, which you set in
> Step 1) and works through them – exactly right in production,
> but a source of randomness in a test: the test never knows how far the instance currently is.
> That's why we turn it off and run the jobs ourselves.

### 4. The test helper – already provided

You neither write nor copy this plumbing: it already ships in the test module at
`src/test/java/io/miragon/training/process/util/ProcessEngineTestUtils.java`. It is the same for
every process test; you just call its methods. What it gives you:

- **`continueToNextWaitState(processEngine)`** – because the job executor is off (Step 3),
  nobody picks up the async-continuation jobs (`asyncBefore`/`asyncAfter`). This method executes
  them from the test thread until the instance reaches its next wait state (user task or end).
  You call it right after starting the process and again after completing a task.
- **`fireTimer(processEngine, activityId)`** – executes a timer job directly, ignoring its due
  date. You don't need it here; it first comes into play with the boundary events in
  [Exercise 7](exercise-07.md).
- **`findProcessInstance(runtimeService, membershipId)`** – looks up the running instance by the
  process key `subscribeNewsletter` and the `membershipId` variable, so your test can assert
  against it.

> The helper needs the engine classes to compile. That is already wired into the module's
> `pom.xml` (the `cibseven-engine` core dependency), so it compiles from the start – you don't
> add anything for it. Open the file once to see how the two or three lines per method work; then
> just use it.

### 5. Write the happy-path test yourself

**New file:** `src/test/java/io/miragon/training/process/MembershipProcessTest.java`

This part is yours to write. Start from the scaffold – the class annotations, the injected engine
services, the mocked use cases and the `init(...)` call are the same for every process test:

```java
@SpringBootTest
@ActiveProfiles("test")
class MembershipProcessTest {

    @Autowired private MembershipProcess membershipProcess;
    @Autowired private RuntimeService runtimeService;
    @Autowired private TaskService taskService;
    @Autowired private ProcessEngine processEngine;

    @MockitoBean private ClaimMembershipUseCase claimMembershipUseCase;
    @MockitoBean private SendConfirmationMailUseCase sendConfirmationMailUseCase;
    @MockitoBean private SendRejectionMailUseCase sendRejectionMailUseCase;
    @MockitoBean private SendWelcomeMailUseCase sendWelcomeMailUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine); // BpmnAwareTests.init(...)
    }
}
```

Every process test follows the same **Given – When – Then** shape. Here is a **generic worked
example** of the happy path: it shows the exact API calls, but the element IDs are placeholders –
you replace each `"<…>"` with the real ID from your model.

```java
@Test
void happyPath_membershipIsConfirmedAndWelcomeMailIsSent() {
    // Given: the capacity check grants a spot
    when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);

    // When: the process is started and driven to its first wait state
    Membership membership = new Membership(new Email("jane@example.com"), new Name("Jane"), new Age(30));
    membershipProcess.startProcess(membership);
    ProcessInstance instance = findProcessInstance(runtimeService, membership.id().value().toString());
    continueToNextWaitState(processEngine);

    // Then: the instance waits at the user task
    assertThat(instance).isWaitingAt("<user-task-id>");

    // When: that user task is completed and the process runs on
    String taskId = taskService.createTaskQuery()
            .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
    taskService.complete(taskId);
    continueToNextWaitState(processEngine);

    // Then: it ended along the confirm path and never touched the reject path
    assertThat(instance)
            .isEnded()
            .hasPassedInOrder("<start>", "<…confirm-path activities, in order…>", "<confirmed-end>")
            .hasNotPassed("<reject-activity>", "<rejected-end>");

    // And: the welcome-mail use case was invoked
    verify(sendWelcomeMailUseCase).sendWelcomeMail(membership.id());
}
```

Everything else you need:

- **Assertion vocabulary** (from `BpmnAwareTests`, via the statically imported `assertThat`):
  `isWaitingAt(id)`, `isEnded()`, `hasPassedInOrder(ids…)`, `hasNotPassed(ids…)` – plus Mockito's
  `when(...)`/`verify(...)` for the use cases.
- **Driving and lookup** come from the provided helper: `continueToNextWaitState(processEngine)`
  and `findProcessInstance(runtimeService, membership.id().value().toString())`.
- **The element IDs** are deliberately not listed here – read them off `membership.bpmn` in the
  modeler. The confirm path is start → claim → gateway → confirmation mail → user task → welcome
  mail → confirmed end; at the gateway the reject path branches to the rejection mail → rejected end.

### 6. Test the rejection path yourself

Now the second test, `noCapacity_membershipIsRejected` – same approach, you write it:

- `claimMembership` returns `false`.
- The process instance runs without a wait state straight through to the rejected end event.
- What's checked: the rejection-mail activity was passed, confirmation and
  welcome mail were **not**, and `sendWelcomeMailUseCase` was never called
  (`verify(..., never())`).

## Constraints

- The test runs **without** PostgreSQL and without a running stack. Two knobs make
  it fast and reproducible:
  1. **h2 instead of PostgreSQL** – an in-memory database that is freshly created
     and discarded for each test run (`ddl-auto: create-drop`).
  2. **Job executor off** – you run the continuations yourself from the test thread. This way
     you determine how far the instance is when you write your assertion.
- Only **the use cases** get mocked. Delegates, model, and engine run for real – otherwise
  you're testing your mocks instead of your process.
- In this exercise the element IDs are still string literals in the test. Note how many there
  are – the [add-on](exercise-06-addon.md) clears them away next.

## Expected result

Run just this one test class – from the repository root directory:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

Both tests pass in a few seconds, without PostgreSQL running. If one
fails, the assertion shows you at which activity the instance actually stood.

## Self-check

- [ ] `asyncBefore` is on the Message Start Event and on the three mail tasks,
      `asyncAfter` on the User Task, `claimMembership` has **no** marker
- [ ] `application-test.yaml` exists, the job executor is turned off in the test profile
- [ ] `ProcessEngineTestUtils` brings the instance up to the next wait state
- [ ] The happy-path test checks the order **and** the paths not taken
- [ ] The rejection test checks that the welcome mail was never called
- [ ] Both tests pass green, without the container stack (Docker/Podman) running

## Hints

**Idempotency rule of thumb:** A retry may re-execute a Service Task. As soon as an action
may happen only *once* (reservation, payment), it must either commit before the boundary or
be idempotent. With external interfaces you'll meet the same pattern again in
[Exercise 10](exercise-10.md) and in [Extra Exercise 1](extra-task-1.md).

## Reference solution

`../../solutions/exercise-06/`

## Next step

The element IDs are still hand-typed strings in the test – fragile the moment someone renames
in the modeler. The add-on turns them into verified constants.

➡️ [Next — Add-on: bpmn-to-code](exercise-06-addon.md)
