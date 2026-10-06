# Exercise 6 · Add-on – From strings to type-safe constants

> **Prerequisite:** [Exercise 6](exercise-06.md) is complete – both process tests are green.
> **Working directory:** `services/process-application`
> **New in this exercise:** the Maven plugin `bpmn-to-code`, a generated Process-API, constants instead of string literals.

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

## Learning goals

After this add-on you can

- wire in `bpmn-to-code` as a Maven plugin and generate the Process-API,
- reference element IDs, message names, and the process key via generated constants,
- explain why hand-typed IDs in tests are a source of errors,
- run the counter-check: a rename in the model must break the build.

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

### 3. Replace the strings in the test

Every BPMN element gets its own class under `FlowNodes`; its `ELEMENT_ID` is a plain
string constant:

```java
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;

assertThat(instance)
        .isEnded()
        .hasPassedInOrder(
                FlowNodes.StartEventSubmitRegistration.ELEMENT_ID,
                FlowNodes.ServiceTaskClaimMembership.ELEMENT_ID,
                FlowNodes.GatewayHasEmptySpots.ELEMENT_ID,
                FlowNodes.ServiceTaskSendConfirmationMail.ELEMENT_ID,
                FlowNodes.UserTaskConfirmMembership.ELEMENT_ID,
                FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID,
                FlowNodes.EndEventMembershipConfirmed.ELEMENT_ID)
        .hasNotPassed(
                FlowNodes.ServiceTaskSendRejectionMail.ELEMENT_ID,
                FlowNodes.EndEventMembershipRejected.ELEMENT_ID);
```

### 4. Replace the process key and the message names

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
  constants instead of strings.
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

**Counter-check:** As an experiment, rename an element in `membership.bpmn` and run
`generate-sources` again – the corresponding constant disappears and your test **no longer
compiles**. That was exactly the goal.

## Self-check

- [ ] `SubscribeNewsletterProcessApi` is generated at build time
- [ ] The process test no longer contains any element-ID string
- [ ] `ProcessEngineTestUtils` uses `PROCESS_ID`, the outbound adapter uses `Messages.*`
- [ ] The counter-check produces a compiler error instead of a silent failure

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
