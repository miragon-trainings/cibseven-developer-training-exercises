# Exercise 6 – Compensation, Call Activity and DMN

> **Prerequisite:** Exercise 5 is complete – subprocess, boundary events and parallel branches are running.
> **Working directory:** `services/process-application`
> **New in this exercise:** compensation (Compensation Boundary Event, compensation handler, Compensating End Event), Call Activity with in-mapping, DMN decision table, Business Rule Task.

## What this is about

In Exercise 5, `revokeClaim` sits in the sequence flow of every abort path. With every new abort
path you would have to wire it in again. **BPMN compensation** instead declares once which
activity undoes which other one, and the engine invokes the handler on abort by itself.

On top of that, Miravelo doesn't want to let rejected applicants between 21 and 29 go without a
fight. The rejection handling therefore moves into its **own process**, invoked via a **Call
Activity**. Who belongs to the target group is decided by a **DMN decision table** that the
business side can maintain itself later.

## Learning goals

After this exercise you can

- declare a compensation handler and trigger it via a Compensating End Event,
- tell compensation apart from a transaction rollback,
- extract part of a process into its own process definition and invoke it via a Call Activity with in-mapping,
- evaluate a DMN decision table via a Business Rule Task.

## Target model

Main process:

![BPMN main process](../assets/exercise-06-main.svg)

Called process `handleRejection`:

![BPMN subprocess](../assets/exercise-06-sub.svg)

Reference models: `../../models/exercise-06/membership.bpmn`,
`../../models/exercise-06/membership-rejection.bpmn`,
`../../models/exercise-06/categorize-applicant.dmn`

## The task

### 1. Declare the compensation handler

Attach a Compensation Boundary Event to the reserving task and connect it via an Association to
the task that undoes the reservation:

| Element | Type | ID | Configuration |
|---|---|---|---|
| Compensation boundary | Compensation Boundary Event | `event_compensateClaim` | attached to `serviceTask_claimMembership` |
| Link | Association | `association_compensateClaim` | from the boundary to `serviceTask_revokeClaim` |
| Handler | Service Task | `serviceTask_revokeClaim` | `isForCompensation="true"`, delegate stays `#{revokeClaimDelegate}` |

The handler thus no longer has any incoming or outgoing sequence flow.

### 2. Turn the End Event into the trigger

Turn `endEvent_membershipDeclined` into a **Compensating End Event**. When an instance reaches
this End Event, the engine compensates all completed activities that have a handler, here
`claimMembership`.

### 3. Model the DMN decision table

In a DMN modeler, create `src/main/resources/dmn/categorize-applicant.dmn`. IDs and types have to
match exactly so that the Business Rule Task finds the decision:

| Property | Value |
|---|---|
| Decision ID | `categorizeApplicant` |
| Hit Policy | `FIRST` |
| Input | `age` (integer) |
| Output | `isHighValue` (boolean) |
| Rules | `[21..29]` → `true`; default `-` → `false` |

The FEEL range `[21..29]` includes both bounds. All `*.dmn` files under `src/main/resources` are
deployed at start-up. If you want to skip the DMN editor, copy
`models/exercise-06/categorize-applicant.dmn` into the module.

### 4. Model the process `handleRejection`

Create `src/main/resources/bpmn/membership-rejection.bpmn` with the process key `handleRejection`:

| Element | Type | ID | Name | Configuration |
|---|---|---|---|---|
| Start | None Start Event | `startEvent_confirmationRejected` | Confirmation rejected | – |
| Categorization | Business Rule Task | `businessRuleTask_categorizeApplicant` | Categorize applicant | Decision Ref `categorizeApplicant`, Result Variable `isHighValue`, Map Decision Result `singleEntry` |
| Branch | Exclusive Gateway | `gateway_highValue` | High value? | Default flow: no-path |
| Personal contact | User Task | `userTask_writeRegretMail` | Write an email expressing regret | `asyncAfter="true"` |
| End of yes-path | End Event | `endEvent_triedToReacquire` | Tried to reaquire applicant | – |
| End of no-path | End Event | `endEvent_acceptRejection` | Accept rejection | – |

The yes-path gets the condition `${isHighValue}`.

### 5. Use the Call Activity

In the main process, place a Call Activity between the two abort boundary events and the
Compensating End Event:

| Element | Type | ID | Name | Configuration |
|---|---|---|---|---|
| Rejection handling | Call Activity | `callActivity_handleRejection` | Handle rejection | Called Element: `handleRejection` |

Incoming flows come from `event_abortAfter3HalfDays` and `event_confirmationRejected`, the
outgoing flow goes to `endEvent_membershipDeclined`. Under **In Mapping**, add one pair each for
`membershipId` and `age`. Without `age` the decision table has no input. In the XML this produces:

```xml
<bpmn:extensionElements>
  <camunda:in source="membershipId" target="membershipId" />
  <camunda:in source="age" target="age" />
</bpmn:extensionElements>
```

### 6. Adapt the process test

After `generate-sources`, the timeout and withdrawal paths no longer compile, because
`serviceTaskRevokeClaim()` is no longer part of the sequence flow. The new path runs through the
Call Activity and ends at the Compensating End Event, which triggers the handler:

```java
var abortPath = PathWalk.from(FlowNodes.userTaskConfirmMembership())
        .interruptedBy(FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventAbortAfter3HalfDays())
        .then(next -> next.callActivityHandleRejection())
        .end(next -> next.endEventMembershipDeclined())
        .throwingCompensation(FlowNodes.eventCompensateClaim(), boundary -> boundary.serviceTaskRevokeClaim());
```

Check it with `hasPassed(abortPath.getIds())` instead of `hasPassedInOrder`: the engine sorts
passed activities by their end time, and the handler ends before the End Event that triggers it.
Then cover both DMN branches:

- **Timeout with age 40:** the Call Activity runs through without a wait state, then compensation
  kicks in.
- **Withdrawal with age 25:** the called process waits at `userTask_writeRegretMail`. The
  constant comes from the second generated API
  (`HandleRejectionProcessApi.FlowNodes.UserTaskWriteRegretMail.ELEMENT_ID`). Complete the task
  and check the same path with `eventConfirmationRejected()`.

The called process is a separate instance with its own jobs. Therefore call
`continueToNextWaitState` **without** an instance ID, otherwise the job behind
`userTask_writeRegretMail` stays put. In the happy path and when capacity is missing, you now
check `hasNotPassed` with `CallActivityHandleRejection` instead of `ServiceTaskRevokeClaim`.

## Constraints

- Nothing changes in the production Java code. `RevokeClaimDelegate` is bound to the element,
  not to its position in the flow.
- Assertions on the main instance see the Call Activity, not its internals.
- `mapDecisionResult=singleEntry` only works as long as exactly one rule with exactly one
  output matches.
- In the high-value branch, the spot is only released after the User Task in the called process
  has been completed.

## Expected result

Reject two sign-ups with different ages. `POST /api/memberships` returns the ID as plain
text:

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 35}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

In the Cockpit, a separate instance of `handleRejection` runs through and ends at *Accept
rejection*. Then the Compensating End Event fires in the main instance, and the log shows
`Revoking membership claim for …`.

Repeat this with Hanna (`hanna@miravelo.com`, age `25`). This time *Write an email expressing
regret* appears in the tasklist. After completing it, `handleRejection` ends at *Tried to
reaquire applicant*, and the main instance compensates.

## Self-check

- [ ] `serviceTask_revokeClaim` carries `isForCompensation="true"` and is no longer part of the sequence flow
- [ ] `endEvent_membershipDeclined` is a Compensating End Event
- [ ] `handleRejection` shows up in the Cockpit as its own process definition, the DMN is deployed
- [ ] The Call Activity passes `membershipId` and `age`
- [ ] Age 21–29 leads to the User Task, any other age goes straight to the End Event
- [ ] The process test is green, both DMN branches are covered

## Hints

**Compensation is not a rollback.** A rollback undoes an engine transaction that hasn't been
committed yet. Compensation undoes already committed work at the business level, in new
transactions and long after the wait state.

**SAGA pattern:** every step gets a compensation step. If the flow fails, the engine compensates
the successful steps in reverse order.

## Reference solution

`../../solutions/exercise-06/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=06
```

## Next step

In Exercise 7 you leave the single module behind: another department gets its own service and
its own process on the same engine.

➡️ [Next: Exercise 7](exercise-07.md)
