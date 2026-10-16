# Exercise 4 · Add-on – From strings to type-safe constants

> **Prerequisite:** [Exercise 4](exercise-04.md) is complete – both process tests are green.
> **Working directory:** `services/process-application`
> **New in this exercise:** the Maven plugin `bpmn-to-code`, a generated Process-API, constants instead of string literals, compile-safe path navigation with `PathWalk`.

## What this is about

Every element ID in your test is a hand-typed copy from the model. If someone renames
`userTask_confirmMembership` in the modeler, the compiler doesn't notice, and the test checks
against an ID that no longer exists. [**bpmn-to-code**](https://github.com/Miragon/bpmn-to-code)
generates a **type-safe Process-API** from the BPMN files at build time, with all element IDs,
message names, and the process key as constants. It also knows the flow: a path step that doesn't
exist in the model doesn't compile.

## Learning goals

After this add-on you can

- wire in `bpmn-to-code` as a Maven plugin and generate the Process-API,
- reference element IDs, message names, and the process key via generated constants,
- navigate a test's expected path through the model in a compile-safe way with `PathWalk`.

## Target model

There is no new model, you are hardening the test from Exercise 4.

## The task

### 1. Enable the plugin and the runtime in `pom.xml`

In `services/process-application/pom.xml`, uncomment the two `TODO Exercise 4 (add-on)` blocks,
the same way as in Exercise 4. The first block brings the runtime types the generated API refers
to:

```xml
<dependency>
    <groupId>io.miragon</groupId>
    <artifactId>bpmn-to-code-runtime</artifactId>
</dependency>
```

The second block is the plugin. It reads all BPMN files under `src/main/resources/bpmn/` and
generates the Process-API into the package `io.miragon.training.adapter.process`:

```xml
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

The plugin runs in the `generate-sources` phase, so on every build. Trigger it once by hand:

```bash
./mvnw -pl services/process-application generate-sources
```

Afterwards `io.miragon.training.adapter.process.SubscribeNewsletterProcessApi` sits under `src/main/java`.

### 3. Replace the single element IDs in the test

Every element gets a class under `FlowNodes` with the constant `ELEMENT_ID`. Use it wherever the
test names a single element, that is, in `isWaitingAt(...)` and `hasNotPassed(...)`:

```java
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;

assertThat(instance).isWaitingAt(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);
```

### 4. Navigate the expected path through the model

A list of constants still compiles when the order in the model has long since changed. So
describe the path for `hasPassedInOrder(...)` with `PathWalk`:

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

Every `then(...)` receives the `Next` of the current element, which offers only its real
successors. At the gateway both branches are on offer. `end(...)` closes the path at the end event,
`getIds()` returns the IDs in walk order. Navigate the rejection path in the second test the same way.

### 5. Replace the process key and the message names

Also replace the strings in the test helper and in the outbound adapter. `ProcessId` and
`MessageName` are wrapper types, in string contexts you call `.getValue()`. The message names live
in the generated class `Messages`:

```java
// ProcessEngineTestUtils: instead of "subscribeNewsletter"
private static final String PROCESS_DEFINITION_KEY = SubscribeNewsletterProcessApi.PROCESS_ID.getValue();

// MembershipProcessAdapter: instead of "Message_SubscriptionRequested"
runtimeService.createMessageCorrelation(Messages.SUBSCRIPTION_REQUESTED.getValue()) ...
```

## Constraints

- From here on all solutions use the generated Process-API and navigate their paths with `PathWalk`.
- `PathWalk` checks the structure of the model. Whether the engine takes the path at runtime is
  still checked by `hasPassedInOrder`.
- Variable names like `"membershipId"` stay strings.

## Expected result

Run the tests again. They are still green, but no longer contain any element-ID literal:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

Do two counter-checks, each followed by `generate-sources`:

1. Rename an element in the model. The constant disappears, the test no longer compiles.
2. Undo that and reroute a sequence flow, for example from the user task straight to the end event.
   `next.serviceTaskSendWelcomeMail()` no longer compiles, although no ID has changed.

## Self-check

- [ ] `SubscribeNewsletterProcessApi` is generated at build time
- [ ] The process test no longer contains any element-ID string
- [ ] Both tests describe their path with `PathWalk`
- [ ] `ProcessEngineTestUtils` uses `PROCESS_ID`, the outbound adapter uses `Messages.*`
- [ ] Both counter-checks produce a compiler error

## Reference solution

`../../solutions/exercise-04/` (Exercise 4 including the add-on) – or load it directly:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

## Next step

In Exercise 5 a subprocess, timers, message boundary events, and a parallel gateway are added.

➡️ [Next: Exercise 5](exercise-05.md)
