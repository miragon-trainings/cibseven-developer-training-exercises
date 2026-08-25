# Exercise 2 – Building the first User Task

> **Prerequisite:** Exercise 1 is complete – the engine starts and deploys `membership.bpmn`.
> **Working directory:** `services/process-application`
> **New in this exercise:** technical modeling (element ID, process key, `isExecutable`, `historyTimeToLive`), User Task, wait state, creating a Generated Form yourself.

## What this is about

The process from Exercise 1 runs from start to finish – including the "Confirm" step. From a
business point of view that's wrong: the confirmation should wait for a **human**, not rush
through.

That's exactly what the **User Task** is for: a step where the process instance stops until
someone completes it. In this exercise you turn the placeholder "Confirm" into a real User Task –
and give it a **form** that you create yourself. The second placeholder ("Send welcome mail")
stays a Manual Task for now.

> *"A task that doesn't wait isn't a task – it's through traffic."*

## Learning goals

After this exercise you can

- give a business BPMN a technical foundation (element ID, process key, `isExecutable`, `historyTimeToLive`),
- convert a Manual Task into a **User Task**,
- explain what a **wait state** is and find it again in the runtime tables (`act_ru_execution`, `act_ru_task`),
- create a **Generated Form** in the modeler yourself and link it to the User Task,
- complete the User Task via the Tasklist.

## Target model

![BPMN model of the exercise](../assets/exercise-02.svg)

Reference model: `../../models/exercise-02/membership.bpmn`

Compared to Exercise 1 exactly one element changes: the Manual Task "Confirm" becomes the User
Task `userTask_confirmMembership` with a form. "Send welcome mail" stays a Manual Task.

## The task

### 1. Give the model a technical foundation

Open `src/main/resources/bpmn/membership.bpmn` in the Miragon BPMN Modeler and make sure the
process properties are right – from now on they are mandatory:

**Process properties:** process key `subscribeNewsletter` · `Executable` enabled ·
`History Time To Live` = `180`

### 2. Convert "Confirm" into a User Task

Change the element's type from **Manual Task** to **User Task** and assign an ID and name:

| Element | Type | ID | Name |
|---|---|---|---|
| Confirmation | User Task | `userTask_confirmMembership` | Confirm membership |

Because a User Task is a **wait state**, the instance will stop here from now on until someone
completes the task.

### 3. Create the Generated Form yourself

The User Task should show the confirming person the data and capture the confirmation. Select the
User Task in the modeler and add a Generated Form with these fields in the Properties Panel under
**Forms**:

| Field ID | Label | Type |
|---|---|---|
| `email` | E-Mail | string |
| `confirmed` | Confirm membership | boolean |

`email` is pre-filled from the process variable of the same name (from the start form).
`confirmed` is new and is stored as a process variable on completion.

### 4. Deploy and test

Restart the application so the changed model is deployed:

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

Start an instance via the Tasklist (`Start process` → `Join Inner Circle`) and fill in the start
form. This time it does **not** run through: it stops at the User Task `Confirm membership`.

### 5. Look at the runtime data

While CIB Seven is running and the instance is parked at the User Task, connect to the database
(same connection as in Exercise 1: host `localhost`, port `5432`, database `cibseven-training`,
user / password `admin`) and run these two queries against the runtime tables:

```sql
SELECT id_, proc_def_id_ FROM exercise.act_ru_execution;
SELECT id_, name_ FROM exercise.act_ru_task;
```

What do they show? `act_ru_execution` has one row for your **still-running** instance – its
`proc_def_id_` is the deployed `subscribeNewsletter` definition. `act_ru_task` has one row for the
open task, with `name_` = `Confirm membership`. That is the wait state made visible: in Exercise 1
both tables ended up empty because the instance ran straight through; now it stops, so its state
stays parked in `act_ru_*` until someone completes the task. Complete it via the Tasklist and
re-run both queries – the rows are gone.

## Constraints

- **Element ID convention** – mandatory from now on: `startEvent_`, `endEvent_`, `userTask_`,
  `serviceTask_`, `manualTask_`, `gateway_`, `subProcess_`, `boundaryEvent_`.
- Still **no Java** is written: start and completion run through the Cockpit.
- The field type must match the type of the process variable, otherwise pre-filling won't take
  effect.

## Expected result

After starting via the start form, the instance waits at the User Task – in `act_ru_task` there
is one row, and in the Cockpit under **Tasklist** `Confirm membership` appears. Open the task:
`email` is pre-filled. Tick `confirmed` and complete – the instance runs through the (still manual)
"Send welcome mail" to `Member joined`.

> **Term: wait state.** A point at which the process instance **stops and waits for an event from
> outside** – here the User Task, which waits for its own completion. The engine writes the state
> into the `act_ru_*` tables and releases the thread; that's why a waiting instance survives a
> restart. A Manual Task is **not** a wait state.

## Self-check

- [ ] `userTask_confirmMembership` is a User Task (no longer a Manual Task)
- [ ] It carries a self-created Generated Form with `email` and `confirmed`
- [ ] A started instance waits at the User Task (`act_ru_task` contains one row)
- [ ] The two runtime queries return one row each while the instance waits, and none after completion
- [ ] After completing it via the Tasklist, the instance ends at `Member joined`
- [ ] The process key is `subscribeNewsletter`, `Executable` is enabled, `historyTimeToLive` = 180

## Hints

The start form (from Exercise 1) and the form on the User Task are both **Generated Forms** – the
same simple mechanism, once on the Start Event, once on the User Task. That's the convenient way in
for forms. From Exercise 4 on the business interaction moves out of the Tasklist and into dedicated
REST endpoints.

## Reference solution

`../../solutions/exercise-02/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

## Next step

In Exercise 3 the second placeholder "Send welcome mail" becomes a real **Service Task** – and
you get to know the **JavaDelegate** that executes it.

➡️ [Next: Exercise 3](exercise-03.md)
