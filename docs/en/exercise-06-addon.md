# Exercise 6 · Add-on – From strings to type-safe constants

> **Prerequisite:** [Exercise 6](exercise-06.md) is complete – both process tests are green.
> **Working directory:** `services/process-application`
> **New in this exercise:** the Maven plugin `bpmn-to-code`, a generated Process-API, constants instead of string literals, compile-safe path navigation with `PathWalk`.

## What this is about

Your test is green – and yet a time bomb is ticking inside it. Count the string literals:
`"userTask_confirmMembership"`, `"serviceTask_sendWelcomeMail"`,
`"endEvent_membershipConfirmed"` … every one of these IDs is a **hand-typed copy** of an ID
from the model.

If someone renames `userTask_confirmMembership` in the modeler next week, **nobody**
notices: the compiler is happy, the test cheerfully checks against an ID that no longer
exists – and goes green or red for the wrong reason. Exactly the silent bug the test was
supposed to stamp out in the first place.

[**bpmn-to-code**](https://github.com/Miragon/bpmn-to-code) is a Maven plugin that generates
a **type-safe Process-API** from your BPMN files at build time: one Java class per process,
in which every element ID, every message name, and the process key becomes a constant.
Rename in the modeler → next build → the silent runtime error becomes a **compiler error**.

The Process-API knows more than the IDs, though: it also knows the **flow**. Every element
knows which elements follow it in the model. So instead of listing IDs, you **navigate** the
expected path through the model – a step that does not exist in the model does not compile.

## Learning goals

After this add-on you can

- wire in `bpmn-to-code` as a Maven plugin and generate the Process-API,
- reference element IDs, message names, and the process key via generated constants,
- navigate a test's expected path through the model in a compile-safe way with `PathWalk`,
- explain why hand-typed IDs in tests are a source of errors,
- run the counter-check: a rename or a rerouted flow in the model must break the build.

## Target model

There is **no new model** – you are hardening the test from Exercise 6.

## The task

### 1. Add the plugin and the runtime to `pom.xml`

The version comes centrally from the root `pom.xml`, so in the module it goes without `<version>`:

```xml
<!-- dependencies -->
<dependency>
    <groupId>io.miragon</groupId>
    <artifactId>bpmn-to-code-runtime</artifactId>
</dependency>
```

```xml
<!-- build > plugins -->
<plugin>
    <groupId>io.miragon</groupId>
    <artifactId>bpmn-to-code-maven</artifactId>
    <executions>
        <execution>
            <id>generate-process-api</id>
            <phase>generate-sources</phase>
            <goals><goal>generate-bpmn-api</goal></goals>
        </execution>
    </executions>
    <configuration>
        <baseDir>${project.basedir}</baseDir>
        <filePattern>src/main/resources/bpmn/*.bpmn</filePattern>
        <outputFolderPath>${project.basedir}/src/main/java</outputFolderPath>
        <packagePath>io.miragon.training.adapter.process</packagePath>
        <outputLanguage>JAVA</outputLanguage>
        <processEngine>CAMUNDA_7</processEngine>
    </configuration>
</plugin>
```

### 2. Generate the Process-API

The plugin is bound to the Maven phase `generate-sources`. Trigger it once so the classes
come into being – from then on it happens automatically on every build:

```bash
./mvnw -pl services/process-application generate-sources
```

Afterwards `io.miragon.training.adapter.process.SubscribeNewsletterProcessApi` sits under
`src/main/java`.

### 3. Replace the single element IDs in the test

Every BPMN element gets its own class under `FlowNodes`; its `ELEMENT_ID` is a plain
string constant. Use it wherever your test names **a single** element – that is, in
`isWaitingAt(...)` and `hasNotPassed(...)`:

```java
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;

assertThat(instance).isWaitingAt(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);
```

### 4. Navigate the expected path through the model

For `hasPassedInOrder(...)` constants alone are not enough: a list of constants still
compiles when the order in the model has long since changed. Describe the path with
`PathWalk` from `bpmn-to-code-runtime` instead:

```java
import io.miragon.bpmn.runtime.path.PathWalk;

var happyPath = PathWalk.from(FlowNodes.startEventSubmitRegistration())
        .then(next -> next.serviceTaskClaimMembership())
        .then(next -> next.gatewayHasEmptySpots())
        .then(next -> next.serviceTaskSendConfirmationMail())
        .then(next -> next.userTaskConfirmMembership())
        .then(next -> next.serviceTaskSendWelcomeMail())
        .end(next -> next.endEventMembershipConfirmed());

assertThat(instance)
        .isEnded()
        .hasPassedInOrder(happyPath.getIds())
        .hasNotPassed(
                FlowNodes.ServiceTaskSendRejectionMail.ELEMENT_ID,
                FlowNodes.EndEventMembershipRejected.ELEMENT_ID);
```

- `PathWalk.from(...)` starts at the start event. Next to its class, every element has a
  method of the same name in camelCase under `FlowNodes` that returns its instance.
- Every `then(...)` receives the `Next` of the current element. It offers **only that
  element's real successors** – so autocompletion shows you exactly the sequence flows of
  the model. At the gateway `gateway_hasEmptySpots` both branches are on offer; the test
  picks the one it expects.
- `end(...)` closes the path at the end event, and `getIds()` returns the element IDs in
  walk order as a `String[]` – exactly what `hasPassedInOrder` needs.

Navigate the rejection path in the second test the same way: from the start event via the
gateway to `serviceTask_sendRejectionMail` and `endEvent_membershipRejected`.

### 5. Replace the process key and the message names

It is not only the test that has hand-typed strings: the test helper looks up instances via
the process key, and the outbound adapter correlates via the message name. Replace both –
`ProcessId` and `MessageName` are wrapper types, so in string contexts you call `.getValue()`.
The message names live in their own generated class `Messages`, next to the Process-API:

```java
// ProcessEngineTestUtils: instead of "subscribeNewsletter"
private static final String PROCESS_DEFINITION_KEY = SubscribeNewsletterProcessApi.PROCESS_ID.getValue();

// MembershipProcessAdapter: instead of "Message_SubscriptionRequested"
runtimeService.createMessageCorrelation(Messages.SUBSCRIPTION_REQUESTED.getValue()) ...
```

## Constraints

- **From here on all solutions use the generated Process-API.** Every further stage
  (boundary events, compensation, call activity) references its new elements via
  constants instead of strings and navigates its paths with `PathWalk`.
- `PathWalk` checks the **structure**: every step is a successor the model allows. Whether
  the engine really takes it at runtime is still checked by `hasPassedInOrder`.
- Variable names like `"membershipId"` or `"hasEmptySpots"` deliberately stay strings – the
  Process-API could type them too, but here it is about the element IDs.
- The plugin runs in the `generate-sources` phase; a normal build is enough, a separate
  call is only needed the first time.

## Expected result

Run the tests from Exercise 6 again – the behavior must not have changed at all:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

The tests are still green, but no longer contain a single element-ID literal.

**Counter-check 1 – rename an ID:** As an experiment, rename an element in `membership.bpmn`
and run `generate-sources` again – the corresponding constant disappears and your test **no
longer compiles**. That was exactly the goal.

**Counter-check 2 – reroute a flow:** Undo the rename and reroute a sequence flow instead,
for example from the user task straight to the end event. After `generate-sources`,
`next.serviceTaskSendWelcomeMail()` no longer compiles, although not a single ID has changed.
A plain list of constants would only have noticed that at runtime.

## Self-check

- [ ] `SubscribeNewsletterProcessApi` is generated at build time
- [ ] The process test no longer contains any element-ID string
- [ ] Both tests describe their path with `PathWalk` instead of an ID list
- [ ] `ProcessEngineTestUtils` uses `PROCESS_ID`, the outbound adapter uses `Messages.*`
- [ ] Both counter-checks produce a compiler error instead of a silent failure

## Hints

It would have paid off even earlier: the outbound adapter correlates its message via
`"Message_SubscriptionRequested"` – also a hand-typed string. In **testing** it pays off the
most, because no other code references so many element IDs at once.

**Outlook:** In the [extra task](extra-task-1.md) the Process-API goes one step
further – there, engine-neutral workers bind to the service tasks via `ServiceTasks`
constants from exactly this API.

## Reference solution

`../../solutions/exercise-06/`

## Next step

In Exercise 7 the process gets considerably richer: a subprocess, a timer, message boundary
events, and a parallel gateway.

➡️ [Next: Exercise 7](exercise-07.md)
