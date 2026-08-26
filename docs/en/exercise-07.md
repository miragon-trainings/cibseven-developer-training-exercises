# Exercise 7 – Subprocess, Boundary Events and Parallelism

> **Prerequisite:** Exercise 6 including the add-on is complete – the process tests run against the generated Process API.
> **Working directory:** `services/process-application`
> **New in this exercise:** embedded subprocess, Timer Boundary Events (interrupting and non-interrupting), Message Boundary Event, Parallel Gateway, an outbound adapter to Microsoft Teams.

## What this is about

Miravelo notices a pattern: many applicants never confirm their membership – and in doing so
block spots that others would love to have. This turns into three requirements:

1. Send a reminder mail **every day** as long as nobody confirms.
2. Automatically cancel the membership after **three and a half days** without confirmation.
3. Applicants can **withdraw** their application themselves.

And whoever makes it all the way to activation should be celebrated: in parallel with the
welcome mail, a notification goes out to the community's **Microsoft Teams channel**. The two
steps don't depend on each other – a case for a **Parallel Gateway**.

## Learning goals

After this exercise you can

- model an embedded subprocess and justify which activities it groups together,
- use a non-interrupting Timer Boundary Event for recurring reminders,
- use an interrupting Timer Boundary Event as a timeout,
- trigger a Message Boundary Event from the outside via correlation,
- span two independent branches with Parallel Gateways and merge them again,
- place transaction boundaries correctly at boundary events and parallel branches,
- secure the new paths in the process test.

## Target model

![BPMN model of the exercise](../assets/exercise-07.svg)

Reference model: `../../models/exercise-07/membership.bpmn`

## The task

### 1. Create the subprocess

You build and configure the entire model in this exercise in the **Miragon BPMN Modeler**
(select the element → Properties Panel), not in the XML.

Group the confirmation mail and the confirmation into an embedded subprocess
`subProcess_confirmMembership` ("Confirm membership"). An embedded subprocess
has its own start and its own end and here contains four elements:

| Element | Type | ID | Name |
|---|---|---|---|
| Start | None Start Event | `startEvent_confirmationRequired` | – |
| Confirmation mail | Service Task | `serviceTask_sendConfirmationMail` | Send confirmation mail |
| Confirmation | User Task | `userTask_confirmMembership` | Confirm membership |
| End | None End Event | `endEvent_membershipConfirmed` | Membership confirmed |

The subprocess is the element to which you attach the boundary events in the next step.
That's exactly why you need it: an interrupting boundary event always cancels the activity
it is attached to – and what should be cancelled is the **entire** confirmation, not just a
single task.

### 2. Attach boundary events

All three attach to `subProcess_confirmMembership`:

| Element | Type | ID | Name | Configuration |
|---|---|---|---|---|
| Reminder | Timer, **non**-interrupting | `event_resendEveryDay` | Every day | **Cycle** `R/P1D` (repeats daily) |
| Timeout | Timer, interrupting | `event_abortAfter3HalfDays` | After 3½ days | **Duration** `P3DT12H` (3½ days) |
| Withdrawal | Message, interrupting | `event_confirmationRejected` | Confirmation rejected | Message: `Message_ConfirmationRejected` |

Mind the difference: the reminder needs a **Cycle** (`R/…`) so that it repeats. A duration
would only fire once.

### 3. Model the new tasks and end events

Every boundary event needs a path that ends somewhere. The reminder gets its own short
branch, the two cancellation paths share one:

| Element | Type | ID | Name | Configuration |
|---|---|---|---|---|
| Reminder mail | Service Task | `serviceTask_reSendConfirmationMail` | Re-Send confirmation mail | `#{reSendConfirmationMailDelegate}` |
| Release spot | Service Task | `serviceTask_revokeClaim` | Revoke claim | `#{revokeClaimDelegate}` |
| End reminder | End Event | `endEvent_mailSentAgain` | Mail sent again | end of the reminder branch |
| End cancellation | End Event | `endEvent_membershipDeclined` | Membership declined | after `Revoke claim` |
| End activation | End Event | `endEvent_membershipActivated` | Membership activated | after the join |

Both interrupting boundary events (`event_abortAfter3HalfDays` and
`event_confirmationRejected`) lead to `serviceTask_revokeClaim` and from there to
`endEvent_membershipDeclined`.

### 4. Add the Parallel Gateway

Between the end of the subprocess and the activation end event come two Parallel
Gateways:

| Element | Type | ID | Branches |
|---|---|---|---|
| Fork | Parallel Gateway | `gateway_notifyFork` | → `serviceTask_sendWelcomeMail`, → `serviceTask_notifyCommunity` |
| Join | Parallel Gateway | `gateway_notifyJoin` | ← both branches, → `endEvent_membershipActivated` |

The new Service Task `serviceTask_notifyCommunity` ("Notify community") binds the
delegate `#{notifyCommunityDelegate}`. Because the join waits for **both** branches, the
membership only counts as activated once mail **and** notification are done.

### 5. Add transaction boundaries

Following the same principle as in [Exercise 6](exercise-06.md):

| Marker | Element | Why |
|---|---|---|
| `asyncAfter` | `event_resendEveryDay` | the reminder runs in its own transaction and repeats without touching the waiting subprocess |
| `asyncAfter` | `event_abortAfter3HalfDays` | a clean boundary **before** the cancellation (and, from Exercise 8 on, before the compensation) |
| `asyncAfter` | `event_confirmationRejected` | likewise for the user-side withdrawal |
| `asyncBefore` | `serviceTask_reSendConfirmationMail` | external effect – like all mail tasks |
| `asyncBefore` | `serviceTask_sendWelcomeMail` | its own commit per parallel branch |
| `asyncBefore` | `serviceTask_notifyCommunity` | its own commit per parallel branch |

**Why a separate boundary per branch?** Without markers, `sendWelcomeMail` and
`notifyCommunity` sit in **one** transaction. If the Teams call fails, the engine rolls
back the job and retries it – the welcome mail would already be out and would go out
**again** to the same address.

### 6. Add use cases and delegates

- **`ReSendConfirmationMailUseCase` / `ReSendConfirmationMailService`** – logs the
  re-sending of the confirmation mail.
- **`RevokeClaimUseCase` / `RevokeClaimService`** – logs the release and frees up the
  capacity spot again.
- **`ReSendConfirmationMailDelegate` / `RevokeClaimDelegate`** – analogous to the existing
  delegates.

### 7. Trigger the withdrawal via REST

The Message Boundary Event is triggered from the outside. Add the endpoint

```
POST /api/memberships/{membershipId}/reject
```

and correlate in the `MembershipProcessAdapter` with
`runtimeService.createMessageCorrelation(...)`: message name from the model, filtered on
the process variable `membershipId`.

### 8. Wire up the community notification

The notification runs entirely inside the engine – an ordinary delegate:

- `adapter/inbound/cibseven/NotifyCommunityDelegate` – reads `membershipId`, calls the use case.
- `application/port/inbound/NotifyCommunityUseCase` + `application/service/NotifyCommunityService` –
  loads the membership, builds a `Notification` (title and text) and hands it to the out-port.
- `application/port/outbound/NotificationPublisherOutPort` +
  `adapter/outbound/teams/MicrosoftTeamsMessagePublisher` – posts the notification as an
  **Adaptive Card** into a Teams channel (webhook from Power Automate *Workflows*).
- `domain/Notification` – a record with `title` and `text`.

Building the Adaptive Card and the REST call are infrastructure: take them from the
reference solution. The `RestClient` is provided by `adapter/config/RestClientConfig`. The
target URL lives in the `application.yaml`:

```yaml
notification:
  teams:
    # Real URL via the environment variable TEAMS_WEBHOOK_URL – no secret in the repository.
    webhook-url: ${TEAMS_WEBHOOK_URL:https://CHANGE-ME}
```

### 9. Extend the process test

So far your test covers the happy path and the rejection due to missing capacity. Add three
tests:

- **Timeout (interrupting):** Wait at the user task, fire the timer with the helper
  `fireTimer(processEngine, Elements.TIMER_ABORT_AFTER_3_HALF_DAYS.getValue())`, execute the
  open jobs and check
  `hasPassed(Elements.SERVICE_TASK_REVOKE_CLAIM.getValue(), Elements.END_EVENT_MEMBERSHIP_DECLINED.getValue())`.
  Mock `RevokeClaimUseCase` for this.
- **Withdrawal via message:** Instead of the timer, call `membershipProcess.rejectMembership(id)`
  – same outcome.
- **Reminder (non-interrupting):**
  `fireTimer(..., Elements.TIMER_RESEND_EVERY_DAY.getValue())`, then check that
  `reSendConfirmationMailUseCase` was called a **second** time and the process is
  still waiting at the user task. Mock `ReSendConfirmationMailUseCase`.

You'll find the `fireTimer` helper (executes a timer job regardless of its due date) in
`ProcessEngineTestUtils`.

## Constraints

- The timers in the reference model carry the **business** values (`R/P1D` and `P3DT12H`). If
  you want to observe the behavior manually, temporarily set them to `R/PT1M` and
  `PT3M` – you don't need this in the process test, where you trigger the timers directly.
- The element IDs of the boundary events follow the grown convention `timer_` and
  `event_` instead of `boundaryEvent_` – that's how it stands in the reference model, and
  that's how it stays.
- New element IDs automatically appear as `Elements.*` constants after the next `generate-sources`.
- In the test, also mock `NotifyCommunityUseCase` so that no real Teams call goes out.
- Never commit a real webhook URL – it comes from `TEAMS_WEBHOOK_URL`.

## Expected result

Create a membership and note the returned ID – you'll use it right away to trigger the
withdrawal:

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "eve@miravelo.com", "name": "Eve", "age": 26}')

# With shortened timers: after a minute the reminder mail appears in the log,
# the user task keeps waiting unchanged

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
# → Revoke claim runs, the instance ends at "Membership declined"
```

Without a withdrawal, the process cancels itself once the timeout timer elapses. If, on the
other hand, the user task is confirmed, both parallel branches run and the instance ends at
`Membership activated`.

## Self-check

- [ ] The subprocess contains a start event, both tasks and an end event
- [ ] All three boundary events attach to the subprocess, and the interruption semantics are correct
- [ ] Both interrupting paths lead through `Revoke claim` to `Membership declined`
- [ ] Fork and join are Parallel Gateways, both branches carry `asyncBefore`
- [ ] `POST /api/memberships/{id}/reject` cancels a waiting instance
- [ ] The three new process tests are green

## Hints

The fact that the Teams integration sits right in the middle of the process application is
deliberately not yet ideal. In [Exercise 10](exercise-10.md) you'll see the counter-model: a
dedicated service that owns its process – including the isolation of its secrets. For now the
delegate is enough.

## Reference solution

`../../solutions/exercise-07/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=07
```

## Next step

`revokeClaim` currently hangs as an explicit task on every cancellation path. In Exercise 8
you'll leave that to the engine.

➡️ [Next: Exercise 8](exercise-08.md)
