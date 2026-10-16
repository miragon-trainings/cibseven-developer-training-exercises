# Exercise 5 – Subprocess, Boundary Events and Parallelism

> **Prerequisite:** Exercise 4 including the add-on is complete – the process tests run against the generated Process API.
> **Working directory:** `services/process-application`
> **New in this exercise:** embedded subprocess, Timer Boundary Events (interrupting and non-interrupting), Message Boundary Event, Parallel Gateway, an outbound adapter to Microsoft Teams.

## What this is about

Many applicants never confirm and thereby block spots. Miravelo therefore wants to send a daily
reminder, cancel automatically after three and a half days without confirmation, and allow a
withdrawal via REST. Whoever makes it to activation gets welcomed in a community **Microsoft Teams
channel**, in parallel with the welcome mail.

## Learning goals

After this exercise you can

- group activities into an embedded subprocess and attach boundary events to it,
- use interrupting and non-interrupting timers as well as a Message Boundary Event,
- span two independent branches with Parallel Gateways and merge them again,
- set transaction boundaries at boundary events and parallel branches and test the new paths.

## Target model

![BPMN model of the exercise](../assets/exercise-05.svg)

Reference model: `../../models/exercise-05/membership.bpmn`

## The task

### 1. Create the subprocess

Group the confirmation mail and the confirmation into the embedded subprocess
`subProcess_confirmMembership` ("Confirm membership"):

| Element | Type | Element ID | Name |
|---|---|---|---|
| Start | None Start Event | `startEvent_confirmationRequired` | – |
| Confirmation mail | Service Task | `serviceTask_sendConfirmationMail` | Send confirmation mail |
| Confirmation | User Task | `userTask_confirmMembership` | Confirm membership |
| End | None End Event | `endEvent_membershipConfirmed` | Membership confirmed |

An interrupting boundary event cancels the activity it is attached to. What should be cancelled is
the entire confirmation, so the boundary events attach to the subprocess and not to a task.

### 2. Attach boundary events

Attach all three to `subProcess_confirmMembership`:

| Element | Type | Element ID | Name | Configuration |
|---|---|---|---|---|
| Reminder | Timer, **non**-interrupting | `event_resendEveryDay` | Every day | **Cycle** `R/P1D` |
| Timeout | Timer, interrupting | `event_abortAfter3HalfDays` | After 3½ days | **Duration** `P3DT12H` |
| Withdrawal | Message, interrupting | `event_confirmationRejected` | Confirmation rejected | Message: `Message_ConfirmationRejected` |

The reminder needs a cycle (`R/…`), a duration would fire only once.

### 3. Model the new tasks and end events

Add the paths behind the boundary events:

| Element | Type | Element ID | Name | Configuration |
|---|---|---|---|---|
| Reminder mail | Service Task | `serviceTask_reSendConfirmationMail` | Re-Send confirmation mail | `#{reSendConfirmationMailDelegate}` |
| Release spot | Service Task | `serviceTask_revokeClaim` | Revoke claim | `#{revokeClaimDelegate}` |
| End reminder | End Event | `endEvent_mailSentAgain` | Mail sent again | end of the reminder branch |
| End cancellation | End Event | `endEvent_membershipDeclined` | Membership declined | after `Revoke claim` |
| End activation | End Event | `endEvent_membershipActivated` | Membership activated | after the join |

Both interrupting boundary events lead to `serviceTask_revokeClaim` and from there to
`endEvent_membershipDeclined`.

### 4. Add the Parallel Gateway

Place two Parallel Gateways between the end of the subprocess and `endEvent_membershipActivated`:

| Element | Type | Element ID | Branches |
|---|---|---|---|
| Fork | Parallel Gateway | `gateway_notifyFork` | → `serviceTask_sendWelcomeMail`, → `serviceTask_notifyCommunity` |
| Join | Parallel Gateway | `gateway_notifyJoin` | ← both branches, → `endEvent_membershipActivated` |

The new Service Task `serviceTask_notifyCommunity` ("Notify community") uses
`#{notifyCommunityDelegate}`. The join waits for both branches.

### 5. Add transaction boundaries

Set the markers following the same principle as in [Exercise 4](exercise-04.md):

| Marker | Element | Why |
|---|---|---|
| `asyncAfter` | `event_resendEveryDay` | The reminder runs in its own transaction, the subprocess keeps waiting untouched |
| `asyncAfter` | `event_abortAfter3HalfDays` | clean boundary before the cancellation |
| `asyncAfter` | `event_confirmationRejected` | likewise for the withdrawal |
| `asyncBefore` | `serviceTask_reSendConfirmationMail` | external effect, like all mail tasks |
| `asyncBefore` | `serviceTask_sendWelcomeMail`, `serviceTask_notifyCommunity` | Without its own boundary, a failed Teams call would also repeat the welcome mail already sent |

### 6. Add use cases and delegates

Create, following the familiar pattern:

- `ReSendConfirmationMailUseCase` / `ReSendConfirmationMailService`: logs the re-sending.
- `RevokeClaimUseCase` / `RevokeClaimService`: logs the release and frees up the spot again.
- `ReSendConfirmationMailDelegate` / `RevokeClaimDelegate`: read `membershipId` and call the use case.

### 7. Trigger the withdrawal via REST

Add the endpoint `POST /api/memberships/{membershipId}/reject`. In the `MembershipProcessAdapter`
it correlates the message `Message_ConfirmationRejected` via
`runtimeService.createMessageCorrelation(...)`, filtered on the process variable `membershipId`.

### 8. Wire up the community notification

Take the following classes from the reference solution. Building the Adaptive Card and the REST
call are infrastructure:

- `adapter/inbound/cibseven/NotifyCommunityDelegate`
- `application/port/inbound/NotifyCommunityUseCase`, `application/service/NotifyCommunityService`
- `application/port/outbound/NotificationPublisherOutPort`, `adapter/outbound/teams/MicrosoftTeamsMessagePublisher`
- `adapter/config/RestClientConfig`, `domain/Notification`

The webhook URL comes from the `application.yaml`:

```yaml
notification:
  teams:
    # Real URL via the environment variable TEAMS_WEBHOOK_URL – no secret in the repository.
    webhook-url: ${TEAMS_WEBHOOK_URL:https://CHANGE-ME}
```

### 9. Extend the process test

After `generate-sources` the happy path no longer compiles: behind the gateway there is now the
subprocess, behind the user task the fork. Navigate it anew:

```java
var happyPath = PathWalk.from(FlowNodes.startEventSubmitRegistration())
        .then(next -> next.serviceTaskClaimMembership())
        .then(next -> next.gatewayHasEmptySpots())
        .onto(next -> next.subProcessConfirmMembership())
        .inside(FlowNodes.subProcessConfirmMembership(), start -> PathWalk.from(start.startEventConfirmationRequired())
                .then(next -> next.serviceTaskSendConfirmationMail())
                .then(next -> next.userTaskConfirmMembership())
                .end(next -> next.endEventMembershipConfirmed()))
        .then(next -> next.gatewayNotifyFork())
        .then(next -> next.serviceTaskSendWelcomeMail())
        .then(next -> next.gatewayNotifyJoin())
        .end(next -> next.endEventMembershipActivated());
```

`onto(...)` steps onto the subprocess without adding it to the path, `inside(...)` walks its
interior. An ordered path follows only one branch. Check the parallel branch unordered with
`hasPassed(FlowNodes.ServiceTaskNotifyCommunity.ELEMENT_ID)`.

Add three tests and mock `RevokeClaimUseCase`, `ReSendConfirmationMailUseCase`, and
`NotifyCommunityUseCase` for them:

- **Timeout:** Fire the timer with `fireTimer(processEngine, FlowNodes.EventAbortAfter3HalfDays.ELEMENT_ID)`
  from `ProcessEngineTestUtils`, execute the open jobs, and check the path with `hasPassedInOrder`:

  ```java
  var abortPath = PathWalk.from(FlowNodes.userTaskConfirmMembership())
          .interruptedBy(FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventAbortAfter3HalfDays())
          .then(next -> next.serviceTaskRevokeClaim())
          .end(next -> next.endEventMembershipDeclined());
  ```

- **Withdrawal:** Instead of the timer, call `membershipProcess.rejectMembership(id)`. The path runs
  via `eventConfirmationRejected()`.
- **Reminder:** Fire `FlowNodes.EventResendEveryDay.ELEMENT_ID` and check that
  `reSendConfirmationMailUseCase` was called and the instance is still waiting at the user task.

## Constraints

- The timers carry the business values. To observe them manually, temporarily set them to
  `R/PT1M` and `PT3M`.
- Never commit a real webhook URL.

## Expected result

Create a membership and withdraw it:

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "eve@miravelo.com", "name": "Eve", "age": 26}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

`Revoke claim` runs, the instance ends at `Membership declined`. With shortened timers, the reminder
mail appears in the log every minute before that. If you confirm instead with
`POST /api/memberships/$MEMBERSHIP_ID/confirm`, both parallel branches run and the instance ends
at `Membership activated`.

## Self-check

- [ ] All three boundary events attach to the subprocess, and the interruption semantics are correct
- [ ] Both interrupting paths lead through `Revoke claim` to `Membership declined`
- [ ] Fork and join are Parallel Gateways, both branches carry `asyncBefore`
- [ ] `POST /api/memberships/{id}/reject` cancels a waiting instance
- [ ] The three new process tests are green

## Reference solution

`../../solutions/exercise-05/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=05
```

## Next step

`revokeClaim` currently hangs as an explicit task on every cancellation path. In Exercise 6 you
leave that to the engine.

➡️ [Next: Exercise 6](exercise-06.md)
