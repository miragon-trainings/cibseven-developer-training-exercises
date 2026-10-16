# Exercise 4 – Securing the process with tests

> **Prerequisite:** Exercise 3 is complete – the gateway, the capacity check, and both process outcomes are working.
> **Working directory:** `services/process-application`
> **New in this exercise:** transaction boundaries (`asyncBefore`/`asyncAfter`), in-memory engine with H2, the job executor turned off, `@MockitoBean`, assertions with `BpmnAwareTests`.

## What this is about

In the Cockpit demo the process worked once. Whether it still works next week, when someone
reroutes a sequence flow or flips a condition, nobody knows. A **process test** starts the real
process in an in-memory engine, lets the real delegates run, and replaces only the business logic
behind them with mocks. Which path the instance has to take is then an assertion in the test and
gets checked on every build. Before that, you make the process resilient with transaction
boundaries.

> *"Works on my machine" is not a test strategy. It's an excuse with better PR.*

## Learning goals

After this exercise you can

- set transaction boundaries and explain why a non-repeatable step must commit before an external effect,
- secure a process as a unit test on an in-memory engine with the job executor turned off,
- mock the use cases behind the delegates with `@MockitoBean`,
- check process paths with `BpmnAwareTests` (`isWaitingAt`, `hasPassedInOrder`, `isEnded`).

## Target model

![BPMN model of the exercise](../assets/exercise-04.svg)

There are no new elements, only transaction boundaries on the process from Exercise 3.

Reference model: `../../models/exercise-04/membership.bpmn`

## The task

### 1. Set transaction boundaries

The theory is in the training chapter **"Async & Transaction Boundaries"**. So far the process runs
synchronously. The engine commits automatically at every wait state. Everywhere else you set a
boundary with an **asynchronous continuation**: `asyncBefore` or `asyncAfter` make the engine
commit at this point, create a job, and continue the work in a new transaction. You find the
markers in the Properties Panel under *Asynchronous Before/After*.

There is no wait state between the Message Start and the User Task. Without markers,
`claimMembership` and `sendConfirmationMail` run in one transaction. If the mail send throws, the
engine rolls everything back and retries the job, and the spot gets reserved twice. The rule:
separate non-repeatable work from the external effect with its own boundary.

| Marker | Element | Why |
|---|---|---|
| `asyncBefore` | `startEvent_submitRegistration` | The correlation only creates the instance and returns |
| `asyncAfter` | `userTask_confirmMembership` | The completion commits immediately instead of rolling back with an error in the next task |
| `asyncBefore` | `serviceTask_sendConfirmationMail` | The reservation commits first, a mail failure only retries the send |
| `asyncBefore` | `serviceTask_sendRejectionMail` | otherwise sits in the same transaction as `claimMembership` |
| `asyncBefore` | `serviceTask_sendWelcomeMail` | external effect, like all mail tasks |

`claimMembership` gets no marker: it commits together with the **token**, the imagined game piece
that marks the current state of an instance in the model.

### 2. Enable the test dependency

In `services/process-application/pom.xml`, uncomment the `TODO Exercise 4` block. As in
Exercise 1, delete only the lines `<!--` and `-->` between `@exercise-4:begin` and
`@exercise-4:end`. Afterwards this dependency is active:

```xml
<dependency>
    <groupId>org.cibseven.bpm</groupId>
    <artifactId>cibseven-bpm-assert</artifactId>
    <scope>test</scope>
</dependency>
```

### 3. Create the test profile

Create `src/test/resources/application-test.yaml`. The `test` profile runs the engine on its own
in-memory H2 and turns the job executor off:

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    url: jdbc:h2:mem:cibseven-test;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
    database:
      type: h2
      schema-update: true
    job-execution:
      enabled: false   # the test runs the async continuations itself
    webapp:
      enabled: false

# The webapp validates this secret at start-up, even when it is turned off
cibseven:
  webclient:
    authentication:
      jwtSecret: M9nU3ORo3s+gK23D9mO5I2h+EIqnosCFDCJi+2bKoulKqZkeQT8pGYg5RhuORlf/fWhLu5meC/SPZCv9NNuj6SK/vE5Sid04UQGrnyh04EpBdiAosAO91xezjgmbSeALUtneibseGpS0tNE4RvLIl+gXiAKqNXyO
```

> **Term: job executor.** The engine's background thread that works through the jobs from
> asynchronous continuations. In a test it means you never know how far the instance currently
> is. That's why it is turned off, and the test runs the jobs itself.

### 4. Write the happy-path test

Replace the placeholder in `src/test/java/io/miragon/training/process/MembershipProcessTest.java`
(marked with `TODO Exercise 4`). To drive the instance, use the provided helper
`process/util/ProcessEngineTestUtils`:

- `continueToNextWaitState(processEngine)` executes the open jobs until the instance reaches its
  next wait state or its end.
- `findProcessInstance(runtimeService, membershipId)` looks up the instance for the `membershipId`.

The scaffold is the same for every process test:

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

The happy path follows the Given – When – Then shape. Replace each placeholder `"<…>"` with the
element ID from your model:

```java
@Test
void happyPath_membershipIsConfirmedAndWelcomeMailIsSent() {
    // Given: the capacity check grants a spot
    when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);

    // When: the instance starts and runs to the first wait state
    Membership membership = new Membership(new Email("jane@example.com"), new Name("Jane"), new Age(30));
    membershipProcess.startProcess(membership);
    ProcessInstance instance = findProcessInstance(runtimeService, membership.id().value().toString());
    continueToNextWaitState(processEngine);

    // Then: the instance waits at the user task
    assertThat(instance).isWaitingAt("<user-task-id>");

    // When: the user task is completed
    String taskId = taskService.createTaskQuery()
            .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
    taskService.complete(taskId);
    continueToNextWaitState(processEngine);

    // Then: the instance ends along the confirm path
    assertThat(instance)
            .isEnded()
            .hasPassedInOrder("<start>", "<…confirm-path activities, in order…>", "<confirmed-end>")
            .hasNotPassed("<reject-activity>", "<rejected-end>");

    verify(sendWelcomeMailUseCase).sendWelcomeMail(membership.id());
}
```

### 5. Test the rejection path

Write the second test, `noCapacity_membershipIsRejected`: `claimMembership` returns `false`, the
instance runs without a wait state to `endEvent_membershipRejected`. Check that the rejection mail
was passed, the confirmation and the welcome mail were not, and that `sendWelcomeMailUseCase` was
never called (`verify(..., never())`).

## Constraints

- The test needs no running application. The in-memory H2 is created fresh for each test run and
  is independent of the data under `~/.cibseven-training`.
- Only the use cases get mocked. Delegates, model, and engine run for real.
- The element IDs are strings in the test. The [add-on](exercise-04-addon.md) replaces them with
  generated constants.

## Expected result

Run the test class from the root directory:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

Both tests pass in a few seconds. If one fails, the assertion shows at which activity the instance
actually stood.

## Self-check

- [ ] `asyncBefore` is on the Message Start Event and on the three mail tasks, `asyncAfter` on the User Task, `claimMembership` has no marker
- [ ] The job executor is turned off in the test profile
- [ ] The happy-path test checks the order and the paths not taken
- [ ] The rejection test checks that the welcome mail was never called
- [ ] Both tests pass green without the application running

## Hints

**Idempotency:** A retry may re-execute a Service Task. If an action may happen only once
(reservation, payment), it must commit before the boundary or be idempotent.

## Reference solution

`../../solutions/exercise-04/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

The solution contains both variants: `MembershipProcessPlainTest` with ID strings and
`MembershipProcessTest` with the generated Process-API from the add-on.

## Next step

The element IDs are still hand-typed strings in the test. The add-on turns them into verified
constants.

➡️ [Next — Add-on: bpmn-to-code](exercise-04-addon.md)
