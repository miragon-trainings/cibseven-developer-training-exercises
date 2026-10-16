# Aufgabe 4 · Add-on – Von Strings zu typsicheren Konstanten

> **Voraussetzung:** [Aufgabe 4](exercise-04.md) ist abgeschlossen – beide Prozess-Tests sind grün.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** das Maven-Plugin `bpmn-to-code`, generierte Process-API, Konstanten statt String-Literale, compile-sichere Pfad-Navigation mit `PathWalk`.

## Darum geht es

Jede Element-ID in deinem Test ist eine handgetippte Kopie aus dem Modell. Benennt jemand
`userTask_confirmMembership` im Modeler um, merkt das der Compiler nicht, und der Test prüft gegen
eine ID, die es nicht mehr gibt. [**bpmn-to-code**](https://github.com/Miragon/bpmn-to-code)
generiert beim Build aus den BPMN-Dateien eine **typsichere Process-API** mit allen Element-IDs,
Message-Namen und dem Prozess-Key als Konstanten. Sie kennt auch den Fluss: Ein Pfadschritt, den es
im Modell nicht gibt, kompiliert nicht.

## Lernziele

Nach diesem Add-on kannst du

- `bpmn-to-code` als Maven-Plugin einbinden und die Process-API generieren,
- Element-IDs, Message-Namen und den Prozess-Key über generierte Konstanten referenzieren,
- den erwarteten Pfad eines Tests mit `PathWalk` compile-sicher durch das Modell navigieren.

## Ziel-Modell

Es kommt kein neues Modell dazu, du härtest den Test aus Aufgabe 4.

## Aufgabe

### 1. Plugin und Runtime in der `pom.xml` aktivieren

Kommentiere in `services/process-application/pom.xml` die beiden Blöcke `TODO Exercise 4 (add-on)`
ein, genauso wie in Aufgabe 4. Der erste Block bringt die Runtime-Typen, auf die die generierte API
verweist:

```xml
<dependency>
    <groupId>io.miragon</groupId>
    <artifactId>bpmn-to-code-runtime</artifactId>
</dependency>
```

Der zweite Block ist das Plugin. Es liest alle BPMN-Dateien unter `src/main/resources/bpmn/` und
generiert die Process-API ins Package `io.miragon.training.adapter.process`:

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

### 2. Process-API generieren

Das Plugin läuft in der Phase `generate-sources`, also bei jedem Build. Stoß sie einmal von Hand an:

```bash
./mvnw -pl services/process-application generate-sources
```

Danach liegt `io.miragon.training.adapter.process.SubscribeNewsletterProcessApi` unter `src/main/java`.

### 3. Einzelne Element-IDs im Test ersetzen

Jedes Element bekommt unter `FlowNodes` eine Klasse mit der Konstante `ELEMENT_ID`. Nutze sie
überall, wo der Test ein einzelnes Element nennt, also in `isWaitingAt(...)` und `hasNotPassed(...)`:

```java
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;

assertThat(instance).isWaitingAt(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);
```

### 4. Den erwarteten Pfad durch das Modell navigieren

Eine Liste von Konstanten kompiliert auch dann, wenn die Reihenfolge im Modell längst eine andere
ist. Beschreibe den Pfad für `hasPassedInOrder(...)` deshalb mit `PathWalk`:

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

Jedes `then(...)` bekommt das `Next` des aktuellen Elements, und dort stehen nur dessen echte
Nachfolger. Am Gateway stehen beide Zweige zur Wahl. `end(...)` schließt am End Event ab,
`getIds()` liefert die IDs in Laufreihenfolge. Navigiere den Ablehnungspfad im zweiten Test genauso.

### 5. Prozess-Key und Message-Namen ersetzen

Ersetze auch die Strings im Test-Helfer und im Outbound-Adapter. `ProcessId` und `MessageName` sind
Wrapper-Typen, in String-Kontexten rufst du `.getValue()` auf. Die Message-Namen liegen in der
generierten Klasse `Messages`:

```java
// ProcessEngineTestUtils: statt "subscribeNewsletter"
private static final String PROCESS_DEFINITION_KEY = SubscribeNewsletterProcessApi.PROCESS_ID.getValue();

// MembershipProcessAdapter: statt "Message_SubscriptionRequested"
runtimeService.createMessageCorrelation(Messages.SUBSCRIPTION_REQUESTED.getValue()) ...
```

## Randbedingungen

- Ab hier nutzen alle Lösungen die generierte Process-API und navigieren ihre Pfade mit `PathWalk`.
- `PathWalk` prüft die Struktur des Modells. Ob die Engine den Pfad zur Laufzeit nimmt, prüft
  weiterhin `hasPassedInOrder`.
- Variablennamen wie `"membershipId"` bleiben Strings.

## Erwartetes Ergebnis

Lass die Tests erneut laufen. Sie sind weiterhin grün, enthalten aber kein Element-ID-Literal mehr:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

Mach zwei Gegenproben, jeweils gefolgt von `generate-sources`:

1. Benenne ein Element im Modell um. Die Konstante verschwindet, der Test kompiliert nicht mehr.
2. Mach das rückgängig und hänge einen Sequenzfluss um, etwa vom User Task direkt zum End Event.
   `next.serviceTaskSendWelcomeMail()` kompiliert nicht mehr, obwohl sich keine ID geändert hat.

## Selbstcheck

- [ ] `SubscribeNewsletterProcessApi` wird beim Build generiert
- [ ] Im Prozess-Test steht kein Element-ID-String mehr
- [ ] Beide Tests beschreiben ihren Pfad mit `PathWalk`
- [ ] `ProcessEngineTestUtils` nutzt `PROCESS_ID`, der Outbound-Adapter nutzt `Messages.*`
- [ ] Beide Gegenproben erzeugen einen Compilerfehler

## Referenzlösung

`../../solutions/exercise-04/` (Aufgabe 4 inklusive Add-on) – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

## Nächster Schritt

In Aufgabe 5 kommen Subprozess, Timer, Message Boundary Events und ein Parallel Gateway dazu.

➡️ [Weiter zu Aufgabe 5](exercise-05.md)
