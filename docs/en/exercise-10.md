# Exercise 10 – The Engine as Shared Infrastructure

> **Prerequisite:** Exercise 9 is complete – the full membership process runs, including the `notifyCommunity` branch off the Parallel Gateway from Exercise 7.
> **Working directory:** `services/process-application` (Part A) and `services/logistics-service` (Part B, created in this exercise)
> **New in this exercise:** Signal End Event and Signal Start Event, External Task, a second service as the process owner, a generated engine client from the OpenAPI spec.

## What this is about

The engine has established itself across the company. More and more departments want to use it –
but nobody wants to run an engine of **their own**.

So we demonstrate that the engine is a reusable infrastructure component: the
membership process throws a **signal** on activation.

> **Concept: signal and broadcast.** A signal is a **broadcast** – unlike a
> message (which you've known since [Exercise 4](exercise-04.md)), it isn't addressed to a
> specific process instance but to everyone listening for it. The thrower doesn't know its
> receivers and doesn't wait for a reply: 1 sender, n receivers.

The **logistics department** runs its own service with its own `sendWelcomeKit` process.
This service **owns the model**, **deploys it itself** into the
shared engine, is started off the broadcast via a **Signal Start Event**, and ships
a welcome kit.

**The key point isn't the mechanism, it's ownership.** External Task
merely means: the engine parks a task, a worker picks it up over REST and reports back.
That says nothing about **who owns the process**. Here it belongs entirely to the
logistics service – model, worker, deployment, and tests all live with it. The shared engine
just runs it.

## Learning goals

After this exercise you can

- turn an End Event into a Signal End Event and throw a signal with a payload,
- make a process react to a broadcast via a Signal Start Event,
- design a Service Task as an External Task and fulfil it with a worker,
- generate a typed engine client from the official OpenAPI spec and drive the engine
  through `/engine-rest` with it,
- deploy a model idempotently from within another service,
- separate the question "who owns the process?" from the External Task mechanism.

## Target architecture

From this exercise on, **two** applications run: the previous engine host and a second
service that belongs to another department. There's still exactly one engine:

```
process-application  (generic engine host — embedded engine + /engine-rest + Cockpit, :8080)
  • owns the membership process; the in-engine "Notify community" (Teams) branch stays
  • new (additive): the terminal End Event "Membership activated" throws Signal_MemberActivated {name}
  • knows nothing about logistics and carries no send-welcome-kit.bpmn

logistics-service  (remote owner — own JVM, :8090)
  • generated, typed client (openapi-generator from cibseven-engine-rest-openapi)
  • deploys send-welcome-kit.bpmn into the engine over REST at start-up (idempotent)
  • fulfils the Service Task shipWelcomeKit as an External Task   (direction 1: engine → worker)
  • drives the engine through the generated client                 (direction 2: worker → engine)
  • owns its own tests (in-memory engine in test scope only)
```

## Target model

Two process models that know each other only through a signal – the host has no idea
logistics exists, and logistics doesn't know the membership process:

Membership process (`subscribeNewsletter`, engine host) – the terminal End Event "Membership
activated" throws `Signal_MemberActivated`:

![BPMN membership process](../assets/exercise-10-main.svg)

Logistics process (`sendWelcomeKit`, modelled and deployed in the logistics-service) – a signal
start plus a manual start, then the External Task `shipWelcomeKit`:

![BPMN send-welcome-kit](../assets/exercise-10-sub.svg)

Reference models: `../../models/exercise-10/membership.bpmn`,
`../../models/exercise-10/send-welcome-kit.bpmn`

The Parallel Gateway from Exercise 7 stays unchanged – only the terminal End Event
"Membership activated" becomes a Signal End Event. It ends the process **and** throws the
signal; no new branch appears.

## The task

### 0. Establish the baseline

Exercise 10 builds on the finished membership process. If you've worked through exercises 1–9,
`services/process-application` is already in the right state. If you're jumping in
directly here, grab the baseline first:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=09
```

### 1. Create the new department

Until now `services/` contained only `process-application`. Now the logistics service joins
it:

```bash
cp -R templates/exercise-10/logistics-service services/logistics-service
```

Register the module in the root `pom.xml` under `<modules>`:

```xml
<module>services/logistics-service</module>
```

From now on `./mvnw` builds the new service too. It already compiles in its initial state – the
client part is still commented out.

### Part A – Engine host

### 2. Turn the End Event into a signal thrower

In the host exactly one change is needed. Turn the terminal End Event
`endEvent_membershipActivated` (after the join) into a **Signal End Event**. `Send Welcome
Mail` and `Notify community` stay unchanged, and **no** new element is added.

You make all changes in the **Miragon BPMN Modeler**, not in the XML: select the End Event →
convert it to a **Signal End Event** → create/select the signal `Signal_MemberActivated` →
set `asyncBefore`. The End Event passes the payload (`name`) along via an **In Mapping**
(`camunda:in`). In the XML this produces:

```xml
<bpmn:endEvent id="endEvent_membershipActivated" name="Membership activated" camunda:asyncBefore="true">
  <bpmn:signalEventDefinition signalRef="Signal_MemberActivated">
    <bpmn:extensionElements>
      <camunda:in source="name" target="name" />
    </bpmn:extensionElements>
  </bpmn:signalEventDefinition>
</bpmn:endEvent>
```

Creating the signal in the modeler produces the definitions-level declaration automatically:

```xml
<bpmn:signal id="Signal_MemberActivated" name="Signal_MemberActivated" />
```

No `RuntimeService`, no delegate – the engine throws the signal natively. The host merely
calls "new member activated" into the room; who reacts to it is none of its business.

### Part B – Logistics service

This is where the real work sits. The order is deliberate: first model the process,
then generate the APIs, then write the code. Work through the
`TODO Exercise 10` spots one after another.

### 3. Model the process

**File:** `src/main/resources/bpmn/send-welcome-kit.bpmn` – it deliberately contains only an
empty model with a Start Event. Model the process yourself; this is your
final check on whether what you've learned has stuck.

You set all the attributes in the table in the **Miragon BPMN Modeler** (select the element →
Properties Panel), not in the XML.

| Element | Type | ID | Configuration |
|---|---|---|---|
| Process | – | `sendWelcomeKit` | `isExecutable="true"`, `historyTimeToLive` set |
| Production start | Signal Start Event | `startEvent_memberActivated` | Signal `Signal_MemberActivated`, `asyncBefore="true"` |
| Manual start | None Start Event | `startEvent_manualStart` | for testing and resending |
| Merge | Exclusive Gateway | `gateway_start` | merges both starts |
| Shipping | Service Task | `serviceTask_shipWelcomeKit` | Implementation **External**, topic `shipWelcomeKit` |
| End | End Event | `endEvent_welcomeKitShipped` | – |

**Why two Start Events?** The Signal Start Event is the production trigger. The empty
Start Event allows a start via `startProcessInstanceByKey` over the REST API – for instance to
resend a kit or in case the signal doesn't get through some time.

### 4. Generate the APIs

Activate the two commented-out generator blocks in `pom.xml`:

- **Process API** (`bpmn-to-code`) – produces the constant
  `SendWelcomeKitProcessApi.ServiceTasks.SHIP_WELCOME_KIT` from your External Task.
- **Engine client** (`openapi-generator`) – produces a typed `/engine-rest` client from
  CIB Seven's official OpenAPI spec instead of hand-written REST calls.
  Set the two `TODO` values: `generatorName` = `java`, `library` = `restclient`.

```bash
./mvnw -pl services/logistics-service generate-sources
```

Afterwards `org.cibseven.rest.client.api` / `.model` sit under `target/…` and
`SendWelcomeKitProcessApi` under `src`.

### 5. Deploy the model

**Class:** `EngineDeploymentAdapter` – ships its own
`send-welcome-kit.bpmn` into the engine over REST at start-up. **Idempotent**: a restart must
not create a second deployment.

### 6. Write the worker

**Class:** `ShipWelcomeKitWorker` – deliberately empty. Make it a bean (`@Component`),
subscribe to the topic
(`@ExternalTaskSubscription(topicName = SendWelcomeKitProcessApi.ServiceTasks.SHIP_WELCOME_KIT)`),
have it extend `BaseExternalTaskWorker`, read the `name` variable, ship the kit
through the use case, and complete the task.

### 7. Drive the engine through the client

**Classes:** `EngineClientConfig` and `RemoteWelcomeKitProcessAdapter` – provide the
`ProcessDefinitionApi` bean and start the process via `startProcessInstanceByKey`.
This uses the manual Start Event and sits behind the `POST /api/welcome-kits` action.

## Constraints

- The host carries **no** `send-welcome-kit.bpmn`. If it ends up there, the whole point of the
  exercise is broken.
- CIB Seven still runs embedded in the host. "Remote" is the **client's** view; a
  true standalone engine (`cibseven/cibseven:run`) would give the same picture with the host swapped out.
- The logistics service runs on port `8090`, the host on `8080`.
- The setup follows the blueprint
  [`miragon-blueprints/cibseven-remote-example`](https://github.com/miragon-blueprints/cibseven-remote-example)
  (Kotlin/Gradle there, Java/Maven here).

## Expected result

### Automated, no running engine

The logistics service tests every seam itself – worker unit test, process test on the
in-memory engine (Signal Start → External Task as wait state → complete), deployment and
remote-adapter test against an HTTP stub (`MockRestServiceServer`):

```bash
./mvnw -pl solutions/exercise-10/logistics-service test
./mvnw -pl solutions/exercise-10/process-application test -Dtest=MembershipProcessTest
```

### End-to-end with both services

For the full run you need three terminals: one for the stack, one each for the two
applications. Work through the steps in order:

```bash
# 1. Start the stack and the engine host (:8080) — Docker or Podman
cd stack && docker compose up -d      # Podman: podman compose up -d
cd ../solutions/exercise-10/process-application && ../../../mvnw spring-boot:run

# 2. Logistics service (:8090) in a second terminal – it deploys its model at start-up
cd solutions/exercise-10/logistics-service && ../../../mvnw spring-boot:run

# 3. Proof that the remote service deployed the model:
curl http://localhost:8080/engine-rest/deployment

# 4. Create a member, complete the confirm task in Cockpit (http://localhost:8080/webapp/#/seven/auth/start,
#    admin/admin) → the signal fires → a sendWelcomeKit instance runs
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","name":"Jane","age":30}'

# 5. Resend the welcome kit (drives the engine through the generated client):
curl -X POST http://localhost:8090/api/welcome-kits \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane"}'
```

**The decisive proof:** Stop the logistics service and activate a member. In
Cockpit a `sendWelcomeKit` instance waits at the External Task. Start the logistics service –
it picks up the task and ships the kit.

## Self-check

- [ ] `send-welcome-kit.bpmn` lives **only** in the logistics service
- [ ] It appears in the engine's deployments **only** after the logistics service has been
      started (`GET /engine-rest/deployment` or Cockpit)
- [ ] Restarting the logistics service creates **no** second deployment
- [ ] An activated member triggers `Signal_MemberActivated`, a `sendWelcomeKit` instance
      runs, the worker ships the kit
- [ ] Membership activation doesn't wait on logistics
- [ ] The logistics service's tests are green **without** the engine host running

## Hints

**Signal broadcast is synchronous in the thrower.** In CIB Seven and Camunda 7, a signal is
delivered **in the thrower's transaction**. Without a marker, the Signal End Event would
create the `sendWelcomeKit` instance and run it synchronously up to the External Task – all in
the membership's activation transaction. An error there (process not yet deployed, a race
at start) would roll the activation back with it. That's why there are **two** boundaries:
`asyncBefore` on the Signal End Event `endEvent_membershipActivated` and `asyncBefore` on the
Signal Start Event `startEvent_memberActivated` in the logistics process. Only then does
"activation doesn't wait on logistics" hold **before** the External Task too.

**Transaction boundary at the External Task (tying back to Exercise 5):** The External Task is
the commit boundary between engine and worker. The engine commits as soon as it creates the
task and waits as a wait state. The worker picks it up via `fetchAndLock`, works in **its
own** transaction, and only then reports back `complete` or `handleFailure`. A
failed `shipWelcomeKit` rolls back **nothing** in the engine; whether and how often to retry
is a worker decision. This is the deliberate counterpart to the
`asyncBefore` pattern: there *the model* sets the boundary, here the mechanism brings it
along – and error handling moves to the owner.

**For trainers:** A signal means a 1:N broadcast. As an extension, a **second** department
(say analytics with `recordSignup`) can listen for the **same** signal – another
remote service that owns and deploys its own process. That's exactly what demonstrates "one
engine, many departments" live.

## Reference solution

- Engine host: `../../solutions/exercise-10/process-application/`
- Logistics service: `../../solutions/exercise-10/logistics-service/` (also contains the
  generated engine client – there is no separate client module)
- To run only the finished result in the working module:
  `./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=10`

## Next step

🎉 **Done!** You've built a process that a dedicated remote service owns and
deploys – experiencing the engine as reusable infrastructure along the way. Want more?
The extra task reworks the process to be engine-neutral.

➡️ [Next: Extra Task 1](extra-task-1.md)
