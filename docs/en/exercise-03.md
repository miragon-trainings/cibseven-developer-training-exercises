# Exercise 3 – The application takes over

> **Prerequisite:** Exercise 2 is complete – User Task and Service Task are working, instances are started in the Cockpit.
> **Working directory:** `services/process-application`
> **New in this exercise:** Message Start Event, correlation, `RuntimeService`, business key, REST endpoint, persistence, confirmation via REST (`TaskService`), Exclusive Gateway with default flow.

## What this is about

Miravelo launches the new **Backroad AL**, and 500 registrations come in overnight. Nobody starts
500 instances by hand in the Cockpit: from now on **the application** starts the process as soon as
a registration arrives via REST. Whoever signs up confirms via a link in a mail (double opt-in), and
this click also arrives as a REST call. On top of that, the Inner Circle is limited to
**1000 seats**: whoever doesn't get a seat anymore receives a rejection.

> *"500 sign-ups. That's either viral or a bot attack."* – CTO, over the second coffee

## Learning goals

After this exercise you can

- start a process instance from Java via a **Message Start Event** and give it a **business key**,
- complete a waiting User Task via REST using the **`TaskService`**,
- model an **Exclusive Gateway** with a default flow and control it with a process variable,
- persist business data and reference it from the process via the `membershipId`.

## Target model

![BPMN model of the exercise](../assets/exercise-03.svg)

Reference model: `../../models/exercise-03/membership.bpmn`

## The task

### 1. Uncomment the application layer

Uncomment all files marked `TODO Exercise 3`. REST, persistence and services are complete
afterwards. The places with engine wiring still carry a `TODO`; you write those yourself in
steps 3 to 5:

| Layer | Files |
|---|---|
| REST | `adapter/inbound/rest/MembershipController.java` (`POST /api/memberships`, `POST /api/memberships/{id}/confirm`) |
| Persistence | `adapter/outbound/db/*` |
| Use cases + services | `RegisterMembership*`, `ConfirmMembership*`, `SendConfirmationMail*`, `ClaimMembership*`, `SendRejectionMail*` |
| Engine (with `TODO`) | `MembershipProcessAdapter`, `SendConfirmationMailDelegate`, `ClaimMembershipDelegate`, `SendRejectionMailDelegate` |

### 2. Rework the model

Change `membership.bpmn` in the modeler. The start form and the form on the User Task are dropped:
the data comes in via REST, and confirmation happens via REST.

| Change | Element ID | Name | Configuration |
|---|---|---|---|
| Start Event → **Message Start Event** | `startEvent_submitRegistration` | Submit registration form | Message Name `Message_SubscriptionRequested` |
| New Service Task | `serviceTask_claimMembership` | Claim membership | `#{claimMembershipDelegate}` |
| New **Exclusive Gateway** | `gateway_hasEmptySpots` | Free seat available? | Default flow: yes path |
| Yes path: new Service Task before the User Task | `serviceTask_sendConfirmationMail` | Send confirmation mail | `#{sendConfirmationMailDelegate}` |
| No path: new Service Task | `serviceTask_sendRejectionMail` | Send rejection mail | `#{sendRejectionMailDelegate}` |
| No path: new End Event | `endEvent_membershipRejected` | Membership rejected | – |
| Rename End Event | `endEvent_membershipConfirmed` | Membership confirmed | – |

The no path gets the condition `${!hasEmptySpots}`. The yes path is the **default flow**: the
engine takes it when no other condition applies, so it needs none itself.

### 3. Start the process via correlation

`POST /api/memberships` saves the membership and calls `MembershipProcessAdapter.startProcess(...)`.
Implement the method: you start a Message Start Event by correlating the message via the
`RuntimeService`. Set the `membershipId` as the **business key**, so each instance in the Cockpit can
be mapped to a registration, and pass the four process variables from the `TODO`:

```java
runtimeService.createMessageCorrelation(/* message name */)
        .processInstanceBusinessKey(/* membershipId */)
        .setVariables(/* membershipId, email, name, age */)
        .correlateStartMessage();
```

Like the process key, the message name `Message_SubscriptionRequested` is a historical name. A
typo leads to a `MismatchingMessageCorrelationException`.

### 4. Switch the delegates to the `membershipId`

From now on the process references the stored membership via the process variable
`membershipId`, no longer via the raw `email`.

- Switch `SendWelcomeMailUseCase` to a `MembershipId` parameter. `SendWelcomeMailService`
  loads the membership via the `MembershipRepository` and logs the email address, as
  `SendConfirmationMailService` demonstrates.
- Implement `SendWelcomeMailDelegate`, `SendConfirmationMailDelegate` and
  `SendRejectionMailDelegate`: read `membershipId`, convert it into a `MembershipId`, call the use
  case.

### 5. Write the claim delegate

Implement `ClaimMembershipDelegate`: it calls `ClaimMembershipUseCase` and writes the result back
as the process variable `hasEmptySpots`. The gateway decides based on this variable. Setting it
belongs in the delegate, because the service doesn't know the engine and only returns a
`boolean`.

### 6. Complete the confirmation via REST

The confirmation link arrives in the application as `POST /api/memberships/{membershipId}/confirm`;
the controller passes it through to `MembershipProcessAdapter.confirm(...)`. Implement the method:
via the `TaskService`, find the open task `userTask_confirmMembership` whose instance carries the
`membershipId` as its business key, and complete it. If there is none, throw an exception.

```java
var task = taskService.createTaskQuery()
        /* task definition key, business key */
        .singleResult();
taskService.complete(/* task ID */);
```

## Constraints

- The capacity is an in-memory counter (`MAX_SPOTS = 1000` in `ClaimMembershipService`) and is
  back at zero after a restart.
- `domain/` and the outbound ports are already active; you don't change them.

## Expected result

Restart the application and register Bob:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "bob@miravelo.com", "name": "Bob", "age": 25}'
```

The call returns the `membershipId`. The log shows `Claiming membership for bob@miravelo.com`
and `Sending confirmation mail to bob@miravelo.com`. In the Cockpit the instance waits at the User
Task and carries the `membershipId` as its business key. Confirm with the ID:

```bash
curl -X POST http://localhost:8080/api/memberships/<membershipId>/confirm
```

The log follows with `Sending welcome mail to bob@miravelo.com`; the instance ends at `Membership confirmed`.

Then check the no path: temporarily set `MAX_SPOTS` to `0`, restart and register Dave
(`dave@miravelo.com`). The log shows `Sending rejection mail to dave@miravelo.com`; the instance
ends at `Membership rejected` without waiting at the User Task.

## Self-check

- [ ] The Start Event is a Message Start Event with `Message_SubscriptionRequested`
- [ ] `POST /api/memberships` starts an instance with the `membershipId` as its business key
- [ ] `POST /api/memberships/{id}/confirm` completes the waiting User Task via the `TaskService`
- [ ] The gateway has a default flow and exactly one condition (`${!hasEmptySpots}`)
- [ ] All delegates read `membershipId`, only the claim delegate sets a variable
- [ ] `./mvnw -pl services/process-application test -Dtest=ArchitectureTest` is green

## Hints

Why a Message Start Event? It names the business trigger (*a registration has come in*) in the
model. You need the same correlation API in [Exercise 5](exercise-05.md) for messages to
**running** instances.

## Reference solution

`../../solutions/exercise-03/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=03
```

## Next step

The process now has two outcomes, and nobody automatically checks that it takes the right one. In
Exercise 4 you secure it with a process test.

➡️ [Next: Exercise 4](exercise-04.md)
