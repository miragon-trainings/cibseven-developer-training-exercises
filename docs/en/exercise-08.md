# Exercise 8 – Compensation (SAGA pattern)

> **Prerequisite:** Exercise 7 is complete – the subprocess, boundary events, and parallel branches are running.
> **Working directory:** `services/process-application`
> **New in this exercise:** Compensation Boundary Event, compensation handler, Compensating End Event, SAGA mindset.

## What this is about

Remember `revokeClaim`? In Exercise 7 that service task sits in the **sequence flow** of every
abort path and releases the reserved seat again. That was pragmatic – and it scales poorly.

As soon as several activities have to be undone (the reservation, the confirmation mail, calls
to third-party services), this undo path grows along with **every** sequence flow that leads to
an abort. You copy the same chain of service tasks onto every abort end event – and forget it on
the next new path.

**BPMN compensation** turns this around: the process declares **once** which activity
(`revokeClaim`) undoes which other activity (`claimMembership`). When a Compensating End Event is
reached, the engine calls this compensation handler on its own – with no sequence flow at all.

## Learning goals

After this exercise you can

- attach a Compensation Boundary Event to a service task,
- mark a task as a compensation handler (`isForCompensation`) and wire it up via an association,
- convert an end event into a Compensating End Event,
- tell compensation apart from a transaction rollback,
- recognize the SAGA pattern in a process model.

## Target model

![BPMN model of the exercise](../assets/exercise-08.svg)

Reference model: `../../models/exercise-08/membership.bpmn`

## The task

### 1. Declare the compensation handler

First you tell the model *what* undoes the reservation. That takes three things: a Compensation
Boundary Event on the reserving task, the handler itself, and the association that connects the
two.

You model and configure all three in the **Miragon BPMN Modeler** (select the element →
Properties Panel), not in the XML.

| Element | Type | ID | Configuration |
|---|---|---|---|
| Compensation boundary | Compensation Boundary Event | `event_compensateClaim` | attaches to `serviceTask_claimMembership` |
| Link | Association | `association_compensateClaim` | from the boundary to `serviceTask_revokeClaim` |
| Handler | Service Task | `serviceTask_revokeClaim` | `isForCompensation="true"`, delegate stays `#{revokeClaimDelegate}` |

The handler therefore sits **outside** the sequence flow: no incoming flow, no outgoing flow.

### 2. Decouple the abort paths

Connect `event_abortAfter3HalfDays` and `event_confirmationRejected` **directly** to
`endEvent_membershipDeclined` — with no detour through a service task. The service task
`serviceTask_revokeClaim` thereby drops out of both sequence flows.

### 3. Turn the end event into a trigger

Convert `endEvent_membershipDeclined` into a **Compensating End Event**. Only then does the abort
trigger compensation.

## Constraints

- **Nothing changes in the Java code.** `RevokeClaimDelegate` stays unchanged – it is just
  called differently: by the engine as a compensation handler instead of via a sequence flow.
- All other elements (subprocess, timer, parallel branches, rejection due to missing capacity)
  stay untouched.
- For the manual test it is worth temporarily setting the timer duration to `PT30S`.

## Expected result

**Abort via timeout:**

1. `POST /api/memberships` – a process instance starts and reserves a seat.
2. Wait until the Timer Boundary Event fires.
3. The log shows the release (`Revoking membership claim for …`) – even though there is no
   explicit task left in the path.
4. In the Cockpit the instance ends at "Membership declined".

**Abort via withdrawal:**

1. `POST /api/memberships`, then wait until the user task `Confirm membership` is active.
2. `POST /api/memberships/{membershipId}/reject` – the Message Boundary Event fires.
3. The path goes straight to the Compensating End Event, and the engine calls `revokeClaim`.

## Self-check

- [ ] `serviceTask_revokeClaim` is **nowhere** in the sequence flow anymore
- [ ] The task carries `isForCompensation="true"` and is wired via an association to the boundary
      event on `claimMembership`
- [ ] `endEvent_membershipDeclined` is a Compensating End Event
- [ ] The release is triggered on timeout **and** on withdrawal
- [ ] In the Cockpit the compensation handler is visible in the process history
- [ ] The process test from Exercise 7 still passes unchanged

## Hints

**Check question:** Why does `RevokeClaimDelegate` keep working without changes even though it is
no longer in the sequence flow? (Answer: the delegate is bound to the *element*, not to its
position in the flow. The engine creates a dedicated execution for the handler with the same
process variables.)

**Compensation is not a rollback.** The technical rollback from the training chapter
*Async & Transaction Boundaries* undoes a *single, not-yet-committed* engine transaction –
automatically and invisibly. Compensation is the business counterpart: it undoes *already
committed* work through **new** transactions, long after the wait state has passed. In short:
rollback acts *before* the commit, compensation *after* it.

**Why your process test stays unchanged:** functionally the outcome does not change –
`serviceTask_revokeClaim` still runs, just as a handler. Your assertions
`hasPassed(FlowNodes.ServiceTaskRevokeClaim.ELEMENT_ID, FlowNodes.EndEventMembershipDeclined.ELEMENT_ID)`
and `verify(revokeClaimUseCase).revokeClaim(id)` still hold. That is exactly a good sign: a
remodeling change that does not alter behavior must not break the test.

**Going further:** compensation is the BPMN tool for **SAGA patterns** in distributed systems –
each step gets a compensation step, and on failure the engine compensates the successful steps in
reverse order. In CIB Seven this also works across subprocess boundaries.

## Reference solution

`../../solutions/exercise-08/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=08
```

## Next step

In Exercise 9 the entire rejection handling moves into its own process – invoked via a Call
Activity and driven by a DMN decision table.

➡️ [Next: Exercise 9](exercise-09.md)
