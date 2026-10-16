# Exercise 7 – The Engine as Shared Infrastructure

> **Prerequisite:** Exercise 6 is complete – the full membership process runs.
> **Working directory:** `services/process-application` (Part A) and `services/logistics-service` (Part B, created in this exercise)
> **New in this exercise:** Signal End Event and Signal Start Event, External Task, a second service as the process owner, the CIB seven Java client for `/engine-rest`.

## What this is about

More and more departments want to use the engine, but nobody wants to run one of their own. The
**logistics department** therefore gets its own service with its own process `sendWelcomeKit`.
The service owns the model, deploys it into the shared engine itself, and ships a welcome kit as
soon as the membership process reports a new member. The key point is ownership: model, worker,
deployment, and tests live with the logistics service, the engine merely runs the process.

> **Concept: signal and broadcast.** Unlike a message, a signal isn't addressed to a specific
> instance but to everyone listening for it. The thrower doesn't know its receivers and doesn't
> wait for a reply.

## Learning goals

After this exercise you can

- throw a signal with a payload via a Signal End Event and start a process via a Signal Start Event,
- design a Service Task as an External Task and fulfil it with a worker,
- drive the engine through `/engine-rest` with the typed CIB seven Java client,
- deploy a model idempotently from within another service.

## Target architecture

From now on two applications run on one engine:

```
process-application  (engine host – embedded engine + /engine-rest + Cockpit, :8080)
  • owns the membership process
  • new: the End Event "Membership activated" throws Signal_MemberActivated {name}
  • knows nothing about logistics and carries no send-welcome-kit.bpmn

logistics-service  (remote owner – own JVM, :8090)
  • CIB seven Java client (cibseven-engine-rest-client-complete-springboot-starter)
  • deploys send-welcome-kit.bpmn over REST at start-up (idempotent, the client's auto-deployment)
  • fulfils the Service Task shipWelcomeKit as an External Task   (engine → worker)
  • starts processes through the typed client                     (worker → engine)
  • owns its own tests (in-memory engine in test scope only)
```

## Target model

Membership process (`subscribeNewsletter`): the End Event "Membership activated" throws
`Signal_MemberActivated`.

![BPMN membership process](../assets/exercise-07-main.svg)

Logistics process (`sendWelcomeKit`): signal start and manual start, then the External Task
`shipWelcomeKit`.

![BPMN send-welcome-kit](../assets/exercise-07-sub.svg)

Reference models: `../../models/exercise-07/membership.bpmn`,
`../../models/exercise-07/send-welcome-kit.bpmn`

## The task

### 0. Establish the baseline

If you've completed Exercise 6, `services/process-application` is in the right state. If you're
jumping in here, load the baseline and then reload the Maven project in IntelliJ:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=06
```

### 1. Create the logistics service

Copy the template into `services/`:

```bash
cp -R templates/exercise-07/logistics-service services/logistics-service
```

Register the module in the root `pom.xml` under `<modules>`. It already compiles in its initial
state:

```xml
<module>services/logistics-service</module>
```

### Part A – Engine host

### 2. Turn the End Event into a signal thrower

Turn `endEvent_membershipActivated` into a **Signal End Event** with the signal
`Signal_MemberActivated` and set `asyncBefore`. You pass the payload `name` along via an
**In Mapping**. In the XML this produces:

```xml
<bpmn:endEvent id="endEvent_membershipActivated" name="Membership activated" camunda:asyncBefore="true">
  <bpmn:signalEventDefinition signalRef="Signal_MemberActivated">
    <bpmn:extensionElements>
      <camunda:in source="name" target="name" />
    </bpmn:extensionElements>
  </bpmn:signalEventDefinition>
</bpmn:endEvent>
```

The engine throws the signal itself, without a delegate and without `RuntimeService`.

### Part B – Logistics service

Work through the `TODO Exercise 7` spots in the logistics service one after another.

### 3. Model the process

Model `src/main/resources/bpmn/send-welcome-kit.bpmn`. The file contains only an empty model
with a Start Event:

| Element | Type | ID | Configuration |
|---|---|---|---|
| Process | – | `sendWelcomeKit` | `isExecutable="true"`, `historyTimeToLive` set |
| Production start | Signal Start Event | `startEvent_memberActivated` | Signal `Signal_MemberActivated`, `asyncBefore="true"` |
| Manual start | None Start Event | `startEvent_manualStart` | for testing and resending |
| Merge | Exclusive Gateway | `gateway_start` | merges both starts |
| Shipping | Service Task | `serviceTask_shipWelcomeKit` | Implementation **External**, topic `shipWelcomeKit` |
| End | End Event | `endEvent_welcomeKitShipped` | – |

The None Start Event allows a start via `startProcessInstanceByKey`, for instance to resend a
kit.

### 4. Generate the Process API

Activate the `bpmn-to-code` block in the logistics service's `pom.xml`. It produces the constant
`ServiceTasks.SHIP_WELCOME_KIT`.

```bash
./mvnw -pl services/logistics-service generate-sources
```

Afterwards `SendWelcomeKitProcessApi` sits under `src`.

### 5. Understand the client and the deployment

The logistics service uses the
[CIB seven Java client](https://github.com/cibseven-community-hub/cibseven-rest-client-java)
(`cibseven-engine-rest-client-complete-springboot-starter`). It is already in the `pom.xml` and brings
three things:

- the typed `/engine-rest` client: `ProcessDefinitionApi` & co. as ready-made beans
  (`org.cibseven.community.rest.client.*`),
- the External Task Client for the worker,
- the auto-deployment: at start-up it ships the BPMN files from `cibseven.auto-deploy.bpmn-resources`
  into the engine. A restart creates no second deployment.

Look at the configuration in `application.yaml`. The engine has to be running before the service
starts, otherwise the start-up fails at the deployment.

### 6. Write the worker

Make `ShipWelcomeKitWorker` a bean (`@Component`), have it extend `BaseExternalTaskWorker`, and
subscribe to the topic (`@ExternalTaskSubscription(topicName = ServiceTasks.SHIP_WELCOME_KIT)`). It
reads the `name` variable, ships the kit through the use case, and completes the task.

### 7. Drive the engine through the client

Inject the `ProcessDefinitionApi` into `RemoteWelcomeKitProcessAdapter` and start the process via
`startProcessInstanceByKey`. Wrap the client's checked `ApiException` into an unchecked exception. This sits behind
`POST /api/welcome-kits`.

## Constraints

- The host carries no `send-welcome-kit.bpmn`.
- "Remote" is the client's view. A standalone engine (CIB Seven Run) would give the same picture.
- The setup follows the blueprint
  [`miragon-blueprints/cibseven-remote-example`](https://github.com/miragon-blueprints/cibseven-remote-example)
  (Kotlin/Gradle there, Java/Maven here).

## Expected result

The logistics service's tests run without the engine host: worker unit test, process test on the
in-memory engine, and an adapter test against a mocked `ProcessDefinitionApi`.

```bash
./mvnw -pl solutions/exercise-07/logistics-service test
./mvnw -pl solutions/exercise-07/process-application test -Dtest=MembershipProcessTest
```

For the end-to-end run you need three terminals:

```bash
# Terminal 1: engine host (:8080)
cd solutions/exercise-07/process-application && ../../../mvnw spring-boot:run

# Terminal 2: logistics service (:8090), deploys its model at start-up
cd solutions/exercise-07/logistics-service && ../../../mvnw spring-boot:run

# Terminal 3: check the deployment, create and confirm a member, resend the kit
curl http://localhost:8080/engine-rest/deployment

MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","name":"Jane","age":30}')
curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/confirm

curl -X POST http://localhost:8090/api/welcome-kits \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane"}'
```

After the confirmation the signal fires, and a `sendWelcomeKit` instance runs. If you stop the
logistics service beforehand, the instance waits in the Cockpit at the External Task until the
service runs again and picks up the task.

## Self-check

- [ ] `send-welcome-kit.bpmn` lives only in the logistics service and appears in the deployments only after it has started
- [ ] Restarting the logistics service creates no second deployment
- [ ] An activated member starts a `sendWelcomeKit` instance, the worker ships the kit
- [ ] Membership activation doesn't wait on logistics
- [ ] The logistics service's tests are green without the engine host running

## Hints

**A signal is delivered in the thrower's transaction.** Without a marker, the new
`sendWelcomeKit` instance would run up to the External Task inside the activation transaction,
and an error there would roll back the activation. That's why `asyncBefore` sits on the Signal
End Event and on the Signal Start Event.

**The External Task brings its own transaction boundary.** The engine commits when it creates the
task. The worker works in its own transaction and reports `complete` or `handleFailure`. An error
in the worker rolls back nothing in the engine; the worker decides about retries.

## Reference solution

- Engine host: `../../solutions/exercise-07/process-application/`
- Logistics service: `../../solutions/exercise-07/logistics-service/`
- Load the engine-host part into the working module:
  `./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=07`

## Next step

🎉 **Done!** The extra task reworks the process to be engine-neutral.

➡️ [Next: Extra Task 1](extra-task-1.md)
