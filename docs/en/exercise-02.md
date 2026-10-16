# Exercise 2 – User Task and Service Task

> **Prerequisite:** Exercise 1 is complete – the engine starts and deploys `membership.bpmn`.
> **Working directory:** `services/process-application`
> **New in this exercise:** process key, element ID, User Task, wait state, Generated Form on the User Task, Service Task, JavaDelegate, Delegate Expression.

## What this is about

The process from Exercise 1 runs through without anyone doing anything: "Confirm" waits for nobody,
"Send welcome mail" sends nothing. You turn one placeholder into a **User Task**, where the
instance waits for a human, and the other into a **Service Task**, which your Java code
executes.

> *"A task that doesn't wait isn't a task – it's through traffic."*

## Learning goals

After this exercise you can

- convert a Manual Task into a **User Task** with a self-made Generated Form,
- find a **wait state** in the runtime tables (`act_ru_execution`, `act_ru_task`),
- bind a Service Task to a **JavaDelegate** via a **Delegate Expression**,
- name the layers of an execution: Delegate → Use Case → Service.

## Target model

![BPMN model of the exercise](../assets/exercise-02.svg)

Reference model: `../../models/exercise-02/membership.bpmn`

## The task

### 1. Uncomment the delegate layer

Uncomment the classes marked `TODO Exercise 2`:

- `application/port/inbound/SendWelcomeMailUseCase.java`
- `application/service/SendWelcomeMailService.java`
- `adapter/inbound/cibseven/BaseDelegate.java`
- `adapter/inbound/cibseven/SendWelcomeMailDelegate.java`

Use case, service and `BaseDelegate` are complete afterwards. The delegate still carries a `TODO`;
you write the engine binding yourself in step 4.

### 2. Convert "Confirm" into a User Task

Open `src/main/resources/bpmn/membership.bpmn` in the Miragon BPMN Modeler. You make all changes
there in the Properties Panel, not in the XML. First take a look at the process properties: the
**process key** `subscribeNewsletter` is the name under which the engine knows the process (a
historical name, it stays as it is). `Executable` is enabled, `History Time To Live` is set to `180`.

Change the type of "Confirm" to **User Task** and create a Generated Form under **Forms**:

| Element | Type | Element ID | Name |
|---|---|---|---|
| Confirmation | User Task | `userTask_confirmMembership` | Confirm membership |

| Field ID | Label | Type |
|---|---|---|
| `email` | E-Mail | string |
| `confirmed` | Confirm membership | boolean |

`email` is pre-filled from the process variable of the same name from the start form. `confirmed`
is stored as a new process variable on completion.

### 3. Convert "Send welcome mail" into a Service Task

Change the type of "Send welcome mail" to **Service Task** and bind it to the delegate:

| Element | Type | Element ID | Name | Delegate Expression |
|---|---|---|---|---|
| Welcome mail | Service Task | `serviceTask_sendWelcomeMail` | Send Welcome Mail | `#{sendWelcomeMailDelegate}` |

The Delegate Expression names the Spring bean the engine calls when the instance reaches this
task.

### 4. Implement `SendWelcomeMailDelegate`

Replace the `TODO` in `executeTask(execution)` in
`adapter/inbound/cibseven/SendWelcomeMailDelegate.java`:

- Read the process variable `email` from the `DelegateExecution`.
- Use it to call `useCase.sendWelcomeMail(...)`.

### 5. Look at the wait state

Restart the application so the changed model gets deployed:

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

In the Tasklist (`Start process` → `Join Inner Circle`), start an instance and fill in the start
form. The instance now stops at the User Task. Open the H2 console with the same credentials as in
Exercise 1 and query the runtime tables:

```sql
SELECT id_, proc_def_id_ FROM act_ru_execution;
SELECT id_, name_ FROM act_ru_task;
```

Both return one row: the waiting instance and the open task `Confirm membership`.

> **Term: wait state.** A point where the process instance stops and waits for an event from
> outside, here the completion of the User Task. The engine stores the state in the `act_ru_*`
> tables and releases the thread, which is why a waiting instance even survives a restart. A
> Manual Task is not a wait state.

### 6. Complete the User Task

Open `Confirm membership` in the Tasklist: `email` is pre-filled. Tick `confirmed` and complete
the task.

## Constraints

- **Element ID convention**, binding from now on: `startEvent_`, `endEvent_`, `userTask_`,
  `serviceTask_`, `manualTask_`, `gateway_`, `subProcess_`, `boundaryEvent_`.
- The delegate is an adapter: it reads process variables and calls a use case. Business logic
  belongs in the service.
- Starting and confirming still happen via the Cockpit.

## Expected result

After you complete the User Task, the Service Task runs, and the log shows:

```
Sending welcome mail to alice@miravelo.com
```

The instance ends at `Member joined`; the two queries from step 5 no longer return a row.

## Self-check

- [ ] `userTask_confirmMembership` is a User Task with a Generated Form (`email`, `confirmed`)
- [ ] A started instance waits at the User Task, `act_ru_task` contains one row
- [ ] `serviceTask_sendWelcomeMail` uses `#{sendWelcomeMailDelegate}`
- [ ] The delegate reads `email` from the `DelegateExecution` and calls the use case
- [ ] After completion the log line appears in the log, and the instance ends at `Member joined`

## Hints

Why separate delegate and service? The delegate knows the engine (`DelegateExecution`, process
variables), the service only the business logic. That keeps the business logic testable without an
engine, which you make use of in the process test in [Exercise 4](exercise-04.md).

## Reference solution

`../../solutions/exercise-02/` – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

## Next step

In Exercise 3 the application takes over: registration and confirmation via REST, start via a
message, and a capacity check with a gateway.

➡️ [Next: Exercise 3](exercise-03.md)
