# Aufgabe 6 · Add-on – Von Strings zu typsicheren Konstanten

> **Voraussetzung:** [Aufgabe 6](exercise-06.md) ist abgeschlossen – beide Prozess-Tests sind grün.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** das Maven-Plugin `bpmn-to-code`, generierte Process-API, Konstanten statt String-Literale.

## Darum geht es

Dein Test ist grün – und trotzdem tickt in ihm eine Zeitbombe. Zähl die String-Literale:
`"userTask_confirmMembership"`, `"serviceTask_sendWelcomeMail"`,
`"endEvent_membershipConfirmed"` … jede dieser IDs ist eine **handgetippte Kopie** einer ID
aus dem Modell.

Benennt nächste Woche jemand `userTask_confirmMembership` im Modeler um, merkt das
**niemand**: Der Compiler ist zufrieden, der Test prüft klaglos gegen eine ID, die es nicht
mehr gibt – und wird grün oder rot aus dem falschen Grund. Genau der stille Fehler, den der
Test eigentlich ausrotten sollte.

[**bpmn-to-code**](https://github.com/Miragon/bpmn-to-code) ist ein Maven-Plugin, das beim
Build aus deinen BPMN-Dateien eine **typsichere Process-API** generiert: eine Java-Klasse
pro Prozess, in der jede Element-ID, jeder Message-Name und der Prozess-Key als Konstante
steht. Umbenennen im Modeler → nächster Build → aus dem stillen Laufzeitfehler wird ein
**Compilerfehler**.

## Lernziele

Nach diesem Add-on kannst du

- `bpmn-to-code` als Maven-Plugin einbinden und die Process-API generieren,
- Element-IDs, Message-Namen und den Prozess-Key über generierte Konstanten referenzieren,
- begründen, warum handgetippte IDs in Tests eine Fehlerquelle sind,
- die Gegenprobe fahren: eine Umbenennung im Modell muss den Build brechen.

## Ziel-Modell

Es kommt **kein neues Modell** dazu – du härtest den Test aus Aufgabe 6.

## Aufgabe

### 1. Plugin und Runtime in die `pom.xml` eintragen

Die Version kommt zentral aus der Root-`pom.xml`, im Modul also ohne `<version>`:

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

### 2. Process-API generieren

Das Plugin hängt in der Maven-Phase `generate-sources`. Stoß sie einmal an, damit die
Klassen entstehen – ab dann passiert das bei jedem Build automatisch:

```bash
./mvnw -pl services/process-application generate-sources
```

Danach liegt `io.miragon.training.adapter.process.SubscribeNewsletterProcessApi` unter
`src/main/java`.

### 3. Strings im Test ersetzen

Jedes BPMN-Element bekommt unter `FlowNodes` eine eigene Klasse; deren `ELEMENT_ID` ist eine
normale String-Konstante:

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

### 4. Prozess-Key und Message-Namen ersetzen

Nicht nur der Test hat handgetippte Strings: Der Test-Helfer sucht Instanzen über den
Prozess-Key, der Outbound-Adapter korreliert über den Message-Namen. Ersetze beide –
`ProcessId` und `MessageName` sind Wrapper-Typen, in String-Kontexten rufst du `.getValue()` auf.
Die Message-Namen liegen in einer eigenen generierten Klasse `Messages` neben der Process-API:

```java
// ProcessEngineTestUtils: statt "subscribeNewsletter"
private static final String PROCESS_DEFINITION_KEY = SubscribeNewsletterProcessApi.PROCESS_ID.getValue();

// MembershipProcessAdapter: statt "Message_SubscriptionRequested"
runtimeService.createMessageCorrelation(Messages.SUBSCRIPTION_REQUESTED.getValue()) ...
```

## Randbedingungen

- **Ab hier nutzen alle Lösungen die generierte Process-API.** Jede weitere Stufe
  (Boundary Events, Kompensation, Call Activity) referenziert ihre neuen Elemente über
  Konstanten statt über Strings.
- Variablennamen wie `"membershipId"` oder `"hasEmptySpots"` bleiben bewusst Strings – die
  Process-API kann sie zwar auch typisieren, hier geht es aber um die Element-IDs.
- Das Plugin läuft in der Phase `generate-sources`; ein normaler Build genügt, ein
  gesonderter Aufruf ist nur beim ersten Mal nötig.

## Erwartetes Ergebnis

Lass die Tests aus Aufgabe 6 erneut laufen – am Verhalten darf sich nichts geändert haben:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

Die Tests sind weiterhin grün, enthalten aber kein einziges Element-ID-Literal mehr.

**Gegenprobe:** Benenne testweise ein Element im `membership.bpmn` um und führe
`generate-sources` erneut aus – die zugehörige Konstante verschwindet und dein Test
**kompiliert nicht mehr**. Genau das war das Ziel.

## Selbstcheck

- [ ] `SubscribeNewsletterProcessApi` wird beim Build generiert
- [ ] Im Prozess-Test steht kein Element-ID-String mehr
- [ ] `ProcessEngineTestUtils` nutzt `PROCESS_ID`, der Outbound-Adapter nutzt `Messages.*`
- [ ] Die Gegenprobe erzeugt einen Compilerfehler statt eines stillen Fehlschlags

## Hinweise

Gelohnt hätte sich das schon vorher: Der Outbound-Adapter korreliert seine Nachricht per
`"Message_SubscriptionRequested"` – auch ein handgetippter String. Beim **Testen** zahlt es
sich am meisten aus, weil kein anderer Code so viele Element-IDs auf einmal referenziert.

**Ausblick:** In der [Extra-Aufgabe](extra-task-1.md) geht die Process-API einen Schritt
weiter – dort binden sich engine-neutrale Worker über `ServiceTasks`-Konstanten aus genau
dieser API an die Service Tasks.

## Referenzlösung

`../../solutions/exercise-06/`

## Nächster Schritt

In Aufgabe 7 wird der Prozess deutlich reicher: Subprozess, Timer, Message Boundary Events
und ein Parallel Gateway.

➡️ [Weiter zu Aufgabe 7](exercise-07.md)
