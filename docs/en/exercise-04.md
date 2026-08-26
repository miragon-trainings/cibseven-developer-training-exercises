# Exercise 4 – The application takes over

> **Prerequisite:** Exercise 3 is complete – the Service Task runs through a JavaDelegate, and the process is still started by hand in the Cockpit.
> **Working directory:** `services/process-application`
> **New in this exercise:** Message Start Event, message correlation, `RuntimeService`, REST endpoint, persistence, confirmation via REST (`TaskService`).

## What this is about

Rose has launched the new **Backroad AL**, and Miravelo is selling it exclusively in the Store.
Social media goes wild, and overnight 500 registrations pour in.

> *"500 sign-ups. That's either viral or a bot attack."*
> — CTO, on the second coffee

Two things become suddenly clear. First: nobody starts 500 process instances by hand in the
Cockpit – **the application** has to start the process as soon as a registration comes in via
REST. Second: are these real people? The answer is a **double opt-in** – confirm first via a
confirmation link, then welcome. That click on the link also lands as a REST call in your
application, not as a click in the Tasklist.

From this exercise on it's the application that drives the process: it creates the Membership,
starts the instance via a **message**, and completes the confirmation step via a REST endpoint.

## Learning goals

After this exercise you can

- replace a None Start Event with a **Message Start Event** and explain why,
- start a process instance from Java via `createMessageCorrelation(...).correlateStartMessage()`,
- persist the business data and reference it via the `membershipId` as a process variable,
- implement REST endpoints that start the process and complete a wait state,
- complete a waiting User Task via the `TaskService` over REST (instead of in the Tasklist).

## Target model

![BPMN model of the exercise](../assets/exercise-04.svg)

Reference model: `../../models/exercise-04/membership.bpmn`

**Three changes compared to Exercise 3:**

1. The Start Event becomes a **Message Start Event** `startEvent_submitRegistration`
   (`Message_SubscriptionRequested`). The start form goes away – the data comes in via the REST
   call.
2. Before the confirmation, a **Service Task** `serviceTask_sendConfirmationMail` is added.
3. The Service Task path from Exercise 3 stays, but the delegates now read the `membershipId`
   instead of the raw email address.

## The task

### 1. Enable the application's business layer

The REST and persistence classes are commented out with `TODO Exercise 4`. Uncomment them – this
is plumbing, not engine binding:

- `adapter/inbound/rest/MembershipController.java`
- `application/service/RegisterMembershipService.java`
- `adapter/outbound/cibseven/MembershipProcessAdapter.java`
- `adapter/outbound/db/*` (Entity, Mapper, JpaRepository, PersistenceAdapter)
- the use-case interfaces in `application/port/inbound/*` (`domain/` and the outbound ports are
  already part of the active skeleton)
- the confirmation and confirm classes (`SendConfirmationMail*`, `ConfirmMembership*`)

The three spots with real engine binding (`MembershipProcessAdapter`, the two delegates) still
carry a `TODO` – you write those yourself.

### 2. Rebuild the model

Take the model from Exercise 3 and change it in the Miragon BPMN Modeler:

| Change | Value |
|---|---|
| Start Event → **Message Start Event** | ID `startEvent_submitRegistration`, name "Submit registration form", Message Name `Message_SubscriptionRequested` |
| Remove the start form | delete the `email`/`name`/`age` fields on the Start Event |
| New **Service Task** before the confirmation | ID `serviceTask_sendConfirmationMail`, name "Send confirmation mail", Delegate Expression `#{sendConfirmationMailDelegate}` |

The flow afterwards is: Message Start → `Send confirmation mail` → `Confirm membership` (User
Task, wait state) → `Send Welcome Mail` → End.

### 3. Persist the registration and start the process

**File:** `application/service/RegisterMembershipService.java`

The REST endpoint `POST /api/memberships` calls `RegisterMembershipUseCase.register(...)`.
Implement the logic: build a `Membership` object from the command, save it via the repository,
start the process via the process port, return `membership.id()`.

### 4. Start the process via correlation

**File:** `adapter/outbound/cibseven/MembershipProcessAdapter.java` – **write it yourself.**

A Message Start Event can't be triggered via `startProcessInstanceByKey`. Switch `startProcess(...)`
to correlating the message `Message_SubscriptionRequested`. The `RuntimeService` gives you a
correlation builder via `createMessageCorrelation(...)`; set the four process variables
`membershipId`, `email`, `name`, `age`. You fill in the arguments yourself:

```java
runtimeService.createMessageCorrelation(/* message name */)
        .setVariables(/* membershipId, email, name, age */)
        .correlateStartMessage();
```

### 5. Switch the delegates to the `membershipId`

Until now the `SendWelcomeMailDelegate` read the raw `email`. Now the process references the
business data via the persisted Membership:

- **`SendWelcomeMailDelegate`** and **`SendConfirmationMailDelegate`** read the process variable
  `membershipId` from the `DelegateExecution`, turn it into a `MembershipId`, and call the
  respective use case – **you write this yourself.**
- **`SendWelcomeMailService`** and **`SendConfirmationMailService`** load the Membership via the
  repository and log the email address.

### 6. Complete the confirmation via a REST endpoint

The confirmation link from the mail lands as `POST /api/memberships/{membershipId}/confirm` in
the application. This endpoint completes the waiting User Task – not the Tasklist.

- `MembershipController` gets a method `confirm(...)` that calls `ConfirmMembershipUseCase`.
- `ConfirmMembershipService` forwards to the process port.
- **`MembershipProcessAdapter.confirm(...)` – write it yourself:** find, via the `TaskService`,
  the open task `userTask_confirmMembership` for the matching `membershipId` and complete it. The
  API chain is `taskService.createTaskQuery()...singleResult()` followed by
  `taskService.complete(...)`; you fill in the query conditions (task definition key, process
  variable) yourself.

## Constraints

- The process key stays `subscribeNewsletter` and the message name exactly
  `Message_SubscriptionRequested` – historical names that stay stable. A typo leads to a
  `MismatchingMessageCorrelationException`.
- The `membershipId` is from now on the reference between application and process instance; it is
  set as a process variable at start.
- The User Task `userTask_confirmMembership` has **no** form in this exercise – it is completed
  via REST. A tasklist form for the approval step is added in [Exercise 5](exercise-05.md).

## Expected result

Restart the application and register a person:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "bob@miravelo.com", "name": "Bob", "age": 25}'
```

1. The call returns the `membershipId`. `Send confirmation mail` runs through – the log shows
   `Sending confirmation mail to bob@miravelo.com`.
2. The process instance waits at the User Task `Confirm membership` (visible in `act_ru_task`).
3. Confirm via the endpoint – with the ID from step 1:

   ```bash
   curl -X POST http://localhost:8080/api/memberships/<membershipId>/confirm
   ```

4. `Send Welcome Mail` runs through (`Sending welcome mail to bob@miravelo.com`), the instance ends.

## Self-check

- [ ] The Start Event is a Message Start Event with the name `Message_SubscriptionRequested`
- [ ] A `POST /api/memberships` creates a Membership, starts the instance via correlation, and
      returns the `membershipId`
- [ ] Both delegates read `membershipId`; the services load the Membership from the repository
- [ ] A `POST /api/memberships/{id}/confirm` completes the waiting `userTask_confirmMembership`
      via the `TaskService`
- [ ] The log lines (confirmation, then welcome) appear in the right order
- [ ] `./mvnw -pl services/process-application test -Dtest=ArchitectureTest` is green

## Hints

**Why a Message Start Event?** A None Start Event says "someone starts this somehow". A Message
Start Event names the business trigger – *a registration has come in* – and makes it visible in
the model. You'll need the same correlation API from [Exercise 7](exercise-07.md) on for messages
**to running instances** too.

**Why the confirmation via REST instead of the Tasklist?** In Exercise 2 the tasklist form was the
simple way in. In a production-like setting, though, the confirmation comes from a dedicated UI or –
as here – from a confirmation link; the click lands as a REST call, and the `TaskService` completes
the wait state. Business interaction runs from now on through dedicated endpoints, no longer through
the generic Tasklist.

## Reference solution

`../../solutions/exercise-04/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

## Next step

In Exercise 5 the Inner Circle gets its exclusivity – with a capacity check and a gateway.

➡️ [Next: Exercise 5](exercise-05.md)
