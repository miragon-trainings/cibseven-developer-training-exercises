# Example: Compensate-then-retry ("roll back & redo")

A self-contained CIB Seven / Spring Boot example showing a process that **validates its work and,
when the check fails, rolls back only the committed steps and redoes them — without touching the
earlier plain steps.**

It is the Miravelo Inner Circle analog of the bike-leasing sketch this example was built from
(`Order bike` / `Issue insurance policy` → check → roll back & retry).

> This example lives outside the graded exercise sequence and is **not** part of the Maven reactor or
> CI. Run it on its own, like a reference solution.

## Why this is different from Exercise 8

Exercise 8 already teaches BPMN **compensation** — but in the *compensate-then-**end*** shape: the
compensation is thrown from an **end event**, so after the rollback the token dies (membership
declined). This example is the *compensate-then-**retry*** shape: compensation is thrown from an
**intermediate throwing event**, which has an outgoing sequence flow, so after the rollback the token
**loops back** and re-executes the committed steps.

| | Exercise 8 (compensate-then-end) | This example (compensate-then-retry) |
|---|---|---|
| Compensation thrown from | an **end** event | an **intermediate throw** event |
| After rollback | instance ends | token loops back and **redoes** the steps |
| Outgoing flow on the throw | none | one — the loop-back |

## The model

`src/main/resources/bpmn/provision-membership.bpmn`, process id `provisionMembership`:

```
                                       ┌───────────────── loop back on failure ─────────────────┐
                                       │                                                         │
 (start) → Create member → Assign      ▼                                                         │
           profile        number → Reserve → Charge → Verify → <Provisioning OK?> ──yes──▶ (Membership active)
                          (plain)    welcome   fee    provis.        │
                                      kit ⊙     ⊙                     └──no (default)──▶ ⟲ throw compensation
                                       │        │
                                    Cancel    Refund          (⊙ = compensation boundary event,
                                  reservation  fee             its handler sits below the task)
```

- **Create member profile** and **Assign membership number** are *plain* steps: they carry **no**
  compensation boundary event, so the retry loop never rolls them back.
- **Reserve welcome kit** and **Charge membership fee** are *compensable*: each has a compensation
  boundary event wired (via an `association`) to a handler task (`Cancel welcome-kit reservation`,
  `Refund membership fee`) marked `isForCompensation="true"`.
- **Verify provisioning** sets `provisioningOk`. The gateway sends `true` to *Membership active* and
  makes the retry edge the **default**, so the instance can never deadlock there.
- The **intermediate throw compensation event** has no `activityRef`, so it compensates the whole
  process scope — which is exactly the two compensable tasks (the plain steps have no handler). Its
  outgoing flow loops back to **Reserve welcome kit**.

### Deterministic by design

`VerifyProvisioningService` returns `false` on attempt 1 and `true` from attempt 2. The `attempt`
counter is a persisted process variable that only ever increments, so the demo shows **exactly one**
rollback + redo and the loop **cannot** run forever.

### Compensation runs in reverse order

The engine compensates completed activities in **reverse** completion order. The last committed step
was *Charge membership fee*, so **Refund fee runs before Cancel reservation**.

## Run it (no Docker/Postgres needed)

The example uses an in-memory H2 database, so a single command starts everything:

```bash
cd examples/compensate-and-retry
../../mvnw spring-boot:run
```

Start an instance:

```bash
curl -s -X POST http://localhost:8080/api/provisioning \
  -H 'Content-Type: application/json' \
  -d '{"email":"neo@miravelo.io","name":"Neo","age":33}'
```

Expected log sequence (one rollback + redo; compensation is reverse order):

```
Create member profile ... (kept even across retries)
Assign membership number IC-... (kept even across retries)
Reserved welcome kit ...
Charged membership fee ...
Verify provisioning ... on attempt 1 -> ok=false
Compensation: refunded membership fee ...        <- rollback, reverse order
Compensation: cancelled welcome-kit reservation ...
Reserved welcome kit ...                          <- redo
Charged membership fee ...
Verify provisioning ... on attempt 2 -> ok=true
```

In **Cockpit** (`http://localhost:8080/webapp/#/seven/auth/start`, admin/admin) the three service
tasks are `asyncBefore`, so the token visibly steps reserve → charge → verify → (throw) → back to
reserve, then ends at *Membership active*. History shows two activity instances each for reserve/charge
and one each for the two compensation handlers.

## Test

`src/test/java/io/miragon/training/process/ProvisionMembershipProcessTest.java` drives the instance
with the job executor disabled and asserts the reserve/charge steps run twice, each compensation
handler once, the plain steps once, and the instance ends at `endEvent_membershipActive`.

```bash
cd examples/compensate-and-retry
../../mvnw test
```

## Design note — flat vs. embedded subprocess

This model is **flat**, mirroring the source sketch: whole-scope compensation rolls back exactly the
two compensable tasks because they are the only activities with a compensation boundary event. The
"plain steps are never rolled back" guarantee therefore rests on *not* attaching a boundary event to
them. If the process later gained compensable activities **outside** the retry region, you would wrap
*Reserve welcome kit* + *Charge membership fee* (plus *Verify*, the gateway and the throw) in an
embedded `subProcess` so the throw's scope is just that region. For this example, where the whole set
of compensable activities *is* the retry region, flat is both correct and faithful.

## Structure

Standard hexagonal layout (`io.miragon.training`), ArchUnit-guarded like the training solutions:

- `adapter/inbound/cibseven/` — `BaseDelegate` + one `JavaDelegate` per step (`#{...}` beans).
- `adapter/inbound/rest/ProvisioningController` — `POST /api/provisioning`.
- `adapter/outbound/cibseven/ProvisioningProcessAdapter` — starts the instance by process key.
- `adapter/outbound/memory/` — in-memory welcome-kit inventory + payment ledger, so the reservation
  and charge (and their compensation) have a visible effect.
- `application/service/` + `application/port/**` — use cases behind ports; no engine imports.
- `domain/MembershipId` — framework-free.
```
