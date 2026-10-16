# Extra Task 1 – Breaking Free from Engine Lock-in

> **Prerequisite:** Exercise 7 is complete – the Membership process runs end to end, including the "Membership activated" signal end event.
> **Working directory:** `services/process-application` (Part A) and `services/logistics-service` (Part B)
> **New in this exercise:** Process-Engine-API from bpm-crafters, worker pattern instead of JavaDelegate, engine-neutral outbound adapter, embedded and remote adapter, ArchUnit guardrail.

## What this is about

Every service task hangs off a `JavaDelegate` with `DelegateExecution`, and the process adapter
calls `RuntimeService` and `TaskService` directly. Switching to Camunda 8 or Operaton would mean
touching every one of these spots. The [**Process-Engine-API**](https://github.com/bpm-crafters/process-engine-api)
from bpm-crafters abstracts the engine, the way JPA abstracts the database. An engine switch thus
essentially becomes a swap of one adapter. Domain, application services, and ports stay
untouched; you only change the adapter layer.

> *"Great that everything works. But what happens if we have to switch the engine in two years?"*
> — The person who has lived through a migration before.

## Learning goals

After this exercise you can

- wire up service tasks via the worker pattern (`@ProcessEngineWorker`) instead of `DelegateExpression`,
- start processes, correlate messages, and complete user tasks engine-neutrally,
- explain why external tasks make the `asyncBefore` markers unnecessary,
- run the same API once embedded and once remote against `/engine-rest`,
- guarantee via an architecture test that no `org.cibseven.bpm` import is left in the code.

## Target model

The model doesn't change functionally, only the technical wiring of the service tasks.

Main process:

![BPMN main process](../assets/extra-task-1-main.svg)

Called process `handleRejection`:

![BPMN subprocess](../assets/extra-task-1-sub.svg)

## The task

You rework both services from Exercise 7. Part A switches the engine host to the embedded adapter,
Part B switches the logistics service to the remote adapter.

### Part A – Engine host: embedded

### 1. Add the dependencies

Add to the module's `pom.xml`:

- `dev.bpm-crafters.process-engine-api:process-engine-api`
- `dev.bpm-crafters.process-engine-worker:process-engine-worker-spring-boot-starter`
- `dev.bpm-crafters.process-engine-adapters:process-engine-adapter-cib-seven-embedded-spring-boot-starter`

The CIB-Seven-embedded adapter is the dependency you would swap when switching engines.

### 2. Switch service tasks to external tasks

In the modeler, set **Implementation** to **External** on all seven service tasks, set the topic,
and add an input mapping for `membershipId` under **Input/Output**: `claimMembership`,
`sendConfirmationMail`, `sendWelcomeMail`, `sendRejectionMail`, `reSendConfirmationMail`,
`revokeClaim` (also as the compensation handler), and `notifyCommunity`. In the XML this produces,
for example:

```xml
<bpmn:serviceTask id="serviceTask_sendConfirmationMail" name="Send confirmation mail"
                  camunda:type="external" camunda:topic="sendConfirmationMail">
  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="membershipId">${membershipId}</camunda:inputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
```

### 3. Replace delegates with workers

Replace every delegate with a worker without an `org.cibseven.bpm` import:

```java
@Component
public class SendConfirmationMailWorker {

    private final SendConfirmationMailUseCase useCase;

    public SendConfirmationMailWorker(SendConfirmationMailUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_CONFIRMATION_MAIL)
    public void sendConfirmationMail(@Variable(name = "membershipId") String membershipId) {
        useCase.sendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
```

The `claimMembership` worker returns the result for the gateway:

```java
@ProcessEngineWorker(topic = ServiceTasks.CLAIM_MEMBERSHIP)
public Map<String, Object> claimMembership(@Variable(name = "membershipId") String membershipId) {
    var hasEmptySpots = useCase.claimMembership(new MembershipId(UUID.fromString(membershipId)));
    return Map.of("hasEmptySpots", hasEmptySpots);
}
```

### 4. Switch the outbound adapter

Replace `RuntimeService` with `StartProcessApi` and `CorrelationApi`:

```java
@Override
public void startProcess(Membership membership) {
    var membershipId = membership.id().value().toString();
    startProcessApi.startProcess(new StartProcessByMessageCmd(
            Messages.SUBSCRIPTION_REQUESTED.getValue(),
            Map.of(
                    "membershipId", membershipId,
                    "email", membership.email().value(),
                    "name", membership.name().value(),
                    "age", membership.age().value(),
                    CommonRestrictions.CORRELATION_KEY, membershipId
            )
    )).join();
}

@Override
public void rejectMembership(MembershipId membershipId) {
    var id = membershipId.value().toString();
    correlationApi.correlateMessage(new CorrelateMessageCmd(
            Messages.CONFIRMATION_REJECTED.getValue(),
            Map.of("membershipId", id),
            Correlation.withKey(id),
            CommonRestrictions.builder().withRestriction("useGlobalCorrelationKey", "true").build()
    )).join();
}
```

### 5. Complete the confirmation engine-neutrally

Instead of looking up the task via `TaskService`, you have the engine deliver open user tasks to
you. `UserTaskSupport` from the Process-Engine-API keeps track of the delivered tasks including
their payload. In the adapter's constructor, subscribe to `userTask_confirmMembership` with the
variable `membershipId`, and in `confirm(...)` complete the matching task via the
`UserTaskCompletionApi`:

```java
confirmationTasks.subscribe(taskSubscriptionApi, CommonRestrictions.builder().build(),
        FlowNodes.UserTaskConfirmMembership.ELEMENT_ID, Set.of("membershipId"));

// in confirm(...): find the task ID for the membershipId via getAllTasks() and getPayload(taskId)
userTaskCompletionApi.completeTask(new CompleteTaskCmd(/* task ID */)).join();
```

### 6. Adjust bootstrap and configuration

Remove `@EnableProcessApplication` from `TrainingApplication`; the adapter takes over deployment
and worker registration. Provide an `EngineCommandExecutor` that executes engine commands
synchronously on the calling thread. That way engine progress and business data commit together:

```java
@Bean
public EngineCommandExecutor engineCommandExecutor() {
    return new EngineCommandExecutor(Runnable::run);
}
```

Add the worker and adapter block to `application.yaml`:

```yaml
dev:
  bpm-crafters:
    process-api:
      worker:
        deployment:
          enabled: true
          bpmnResourcePattern: "classpath*:/**/*.bpmn"
          dmnResourcePattern: "classpath*:/**/*.dmn"
      adapter:
        cib-seven-embedded:
          enabled: true
          service-tasks:
            delivery-strategy: embedded_scheduled
            worker-id: extra-task-1-worker
            schedule-delivery-fixed-rate-in-seconds: 5
          user-tasks:
            delivery-strategy: embedded_scheduled
            schedule-delivery-fixed-rate-in-seconds: 5
```

### 7. Set the guardrail

Add a rule to `ArchitectureTest` that forbids every `org.cibseven.bpm` import:

```java
@ArchTest
static final ArchRule no_class_should_depend_on_the_native_engine = noClasses()
        .should().dependOnClassesThat().resideInAPackage("org.cibseven.bpm..");
```

Once the test is green, the engine only lives in `pom.xml` and `application.yaml`.

### Part B – Logistics service: the same API, remote

The logistics service from Exercise 7 has no engine of its own. It switches to the Process-Engine-API
as well, but uses the **remote** adapter: the code talks to the same API, the adapter translates it
into calls against `/engine-rest`.

### 8. Add the remote adapter

In the logistics service's `pom.xml`, replace the CIB seven Java client with:

- `dev.bpm-crafters.process-engine-api:process-engine-api`
- `dev.bpm-crafters.process-engine-worker:process-engine-worker-spring-boot-starter`
- `dev.bpm-crafters.process-engine-adapters:process-engine-adapter-cib-seven-remote-spring-boot-starter`
- `org.cibseven.commons:cibseven-commons-typed-values` (the adapter declares it as `provided`)

Configure the adapter in `application.yaml`:

```yaml
dev:
  bpm-crafters:
    process-api:
      adapter:
        cib-seven-remote:
          enabled: true
          client:
            base-url: http://localhost:8080/engine-rest
          service-tasks:
            delivery-strategy: remote_scheduled
            worker-id: logistics-service-worker
            schedule-delivery-fixed-rate-in-seconds: 5
          user-tasks:
            delivery-strategy: remote_scheduled
            schedule-delivery-fixed-rate-in-seconds: 5
```

### 9. Switch the worker, the start, and the deployment

- `ShipWelcomeKitWorker` becomes a `@ProcessEngineWorker(topic = ServiceTasks.SHIP_WELCOME_KIT)` with
  `@Variable(name = "name")`. `BaseExternalTaskWorker` goes away; the worker library handles
  completion and failure.
- `RemoteWelcomeKitProcessAdapter` starts the process via
  `startProcessApi.startProcess(new StartProcessByDefinitionCmd(PROCESS_ID, Map.of("name", ...)))`.
- A small adapter deploys the model through the engine-neutral `DeploymentApi`
  (`DeployBundleCommand` with the `NamedResource`s from `classpath*:bpmn/*.bpmn`).

Add an `ArchitectureTest` here as well that forbids every dependency on `org.cibseven..`. After that
the two services differ only in their adapter: embedded in the engine host, remote in the logistics
service.

## Constraints

- The `asyncBefore` markers on the service tasks are gone. An external task is a wait state: the
  engine commits when it creates the task and waits until a worker completes it.
- The topic constants (`ServiceTasks.*`) come from the generated Process-API from
  [Exercise 4 · Add-on](exercise-04-addon.md).
- Service tasks and user tasks are delivered via polling roughly every 5 seconds. A `confirm`
  right after the registration may therefore not find a task yet.
- The worker library's auto-deployment option (`dev.bpm-crafters.process-api.worker.deployment`)
  doesn't kick in with the remote adapter. That's why the logistics service deploys its model itself
  through the `DeploymentApi`. In the engine host, the CIB seven starter's auto-deployment keeps
  deploying.

## Expected result

Run the scenarios from Exercise 6. The behavior is identical, only workers run instead of
delegates:

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 35}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

After the withdrawal, `handleRejection` runs, then the `revokeClaim` worker compensates. Repeat
this with Hanna (age `25`): the called process additionally waits at the User Task *Write an email
expressing regret*. A third sign-up that you confirm after a few seconds with
`POST /api/memberships/$MEMBERSHIP_ID/confirm` ends at `Membership activated`.

If the logistics service runs alongside, the signal starts a `sendWelcomeKit` instance, and its
worker ships the kit through the remote adapter. `POST http://localhost:8090/api/welcome-kits` starts
the process through the `StartProcessApi`.

## Self-check

- [ ] All seven service tasks are `camunda:type="external"` with a topic
- [ ] There are no more `JavaDelegate` classes, only `@ProcessEngineWorker` workers
- [ ] The outbound adapter uses `StartProcessApi`, `CorrelationApi`, and `UserTaskCompletionApi`
- [ ] `@EnableProcessApplication` is removed, the `EngineCommandExecutor` bean is in place
- [ ] The ArchUnit test reports no dependency on `org.cibseven.bpm`
- [ ] The logistics service uses the remote adapter and has no dependency on `org.cibseven..` in its code

## Hints

The [**engine-safari**](https://github.com/emaarco/engine-safari) repository serves as a template,
module `cib-seven-with-process-engine-api`.

## Reference solution

- Engine host (embedded): `../../solutions/extra-task-1/process-application/`
- Logistics service (remote): `../../solutions/extra-task-1/logistics-service/`

## Next step

🦁 **Done!** CIB Seven still runs under the hood, but your code knows nothing about it anymore.
