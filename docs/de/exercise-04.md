# Aufgabe 4 – Den Prozess mit Tests absichern

> **Voraussetzung:** Aufgabe 3 ist abgeschlossen – Gateway, Kapazitätsprüfung und beide Prozessausgänge laufen.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Transaktionsgrenzen (`asyncBefore`/`asyncAfter`), In-Memory-Engine mit H2, abgeschalteter Job Executor, `@MockitoBean`, Assertions mit `BpmnAwareTests`, Coverage-Report (Process Test Coverage).
> **Dauer:** 45–60 Min.

## Darum geht es

In der Cockpit-Demo hat der Prozess einmal funktioniert. Ob er noch funktioniert, nachdem jemand
einen Sequenzfluss umbiegt, weiß niemand. Ein **Prozess-Test** startet den echten Prozess in einer
In-Memory-Engine, lässt die echten Delegates laufen und mockt nur die Fachlogik dahinter. Der Weg,
den die Instanz nehmen muss, wird zur Assertion und bei jedem Build geprüft. Vorher machst du den
Prozess mit Transaktionsgrenzen robust.

> *„Works on my machine" ist kein Testkonzept. Es ist eine Ausrede mit besserer PR.*

## Lernziele

Nach dieser Aufgabe kannst du

- Transaktionsgrenzen setzen und begründen, warum ein nicht wiederholbarer Schritt vor einem externen Effekt committen muss,
- einen Prozess als Unit-Test auf einer In-Memory-Engine mit abgeschaltetem Job Executor absichern,
- die Use Cases hinter den Delegates mit `@MockitoBean` mocken,
- Prozesspfade mit `BpmnAwareTests` prüfen (`isWaitingAt`, `hasPassedInOrder`, `isEnded`) und ungetestete Pfade im Coverage-Report finden.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-04.svg)

Es kommen keine neuen Elemente dazu, nur Transaktionsgrenzen am Prozess aus Aufgabe 3.

Referenzmodell: `../../models/exercise-04/membership.bpmn`

## Aufgabe

### 1. Transaktionsgrenzen setzen

Bisher läuft der Prozess synchron, und die Engine committet nur an Wait States. Eine **asynchrone
Continuation** (`asyncBefore` oder `asyncAfter`, Properties Panel → *Asynchronous Before/After*)
lässt die Engine committen, einen Job anlegen und in einer neuen Transaktion weitermachen. Theorie:
Trainingskapitel **„Async & Transaction Boundaries"**.

Zwischen Message Start und User Task liegt kein Wait State. Ohne Marker laufen `claimMembership` und
`sendConfirmationMail` in einer Transaktion. Wirft der Mailversand, rollt die Engine alles zurück und
wiederholt den Job, und der Platz wird doppelt reserviert. Die Regel: Trenne nicht wiederholbare
Arbeit durch eine eigene Grenze vom externen Effekt.

| Marker | Element | Warum |
|---|---|---|
| `asyncBefore` | `startEvent_submitRegistration` | Die Korrelation legt nur die Instanz an und kehrt zurück |
| `asyncAfter` | `userTask_confirmMembership` | Die Completion committet sofort, statt bei einem Fehler im nächsten Task mit zurückzurollen |
| `asyncBefore` | `serviceTask_sendConfirmationMail` | Die Reservierung committet zuerst, ein Mail-Fehler wiederholt nur den Versand |
| `asyncBefore` | `serviceTask_sendRejectionMail` | liegt sonst in derselben Transaktion wie `claimMembership` |
| `asyncBefore` | `serviceTask_sendWelcomeMail` | externer Effekt, wie alle Mail-Tasks |

`claimMembership` bekommt keinen Marker: Er committet gemeinsam mit dem **Token**, der gedachten
Spielfigur, die den aktuellen Stand einer Instanz im Modell markiert.

### 2. Test-Dependencies aktivieren

Kommentiere in `services/process-application/pom.xml` den Block `TODO Exercise 4` ein. Wie in
Aufgabe 1 löschst du nur die Zeilen `<!--` und `-->` zwischen `@exercise-4:begin` und
`@exercise-4:end`. Danach sind diese Dependencies aktiv:

```xml
<dependency>
    <groupId>org.cibseven.bpm</groupId>
    <artifactId>cibseven-bpm-assert</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.cibseven.community.process_test_coverage</groupId>
    <artifactId>cibseven-process-test-coverage-spring-test-platform-7</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-reflect</artifactId>
    <scope>test</scope>
</dependency>
```

Process Test Coverage, eine Community-Erweiterung von CIB seven, schreibt pro Testklasse einen Report,
welche Elemente die Tests durchlaufen haben. Sie braucht `kotlin-reflect`, bringt es aber nicht mit.

### 3. Testprofil anlegen

Lege `src/test/resources/application-test.yaml` an. Das Profil `test` lässt die Engine auf einer
eigenen In-Memory-H2 laufen und schaltet den Job Executor ab:

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    url: jdbc:h2:mem:cibseven-test;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

camunda:
  bpm:
    admin-user:
      id: admin
      password: admin
    database:
      type: h2
      schema-update: true
    job-execution:
      enabled: false   # die Async-Continuations führt der Test selbst aus
    webapp:
      enabled: false

# Die Webapp validiert dieses Secret beim Start, auch wenn sie abgeschaltet ist
cibseven:
  webclient:
    authentication:
      jwtSecret: M9nU3ORo3s+gK23D9mO5I2h+EIqnosCFDCJi+2bKoulKqZkeQT8pGYg5RhuORlf/fWhLu5meC/SPZCv9NNuj6SK/vE5Sid04UQGrnyh04EpBdiAosAO91xezjgmbSeALUtneibseGpS0tNE4RvLIl+gXiAKqNXyO
```

> **Begriff: Job Executor.** Der Hintergrund-Thread der Engine, der die Jobs aus asynchronen
> Continuations abarbeitet. Im Test wüsstest du nie, wie weit die Instanz ist, deshalb führt der
> Test die Jobs selbst aus.

### 4. Happy-Path-Test schreiben

Ersetze den Platzhalter in `src/test/java/io/miragon/training/process/MembershipProcessTest.java`
(markiert mit `TODO Exercise 4`). Für die Ausführung der Jobs nutzt du den vorgegebenen Helfer
`process/util/ProcessEngineTestUtils`:

- `continueToNextWaitState(processEngine)` führt die offenen Jobs aus, bis die Instanz den nächsten
  Wait State oder ihr Ende erreicht.
- `findProcessInstance(runtimeService, membershipId)` sucht die Instanz zur `membershipId`.

Das Gerüst ist für jeden Prozess-Test gleich:

```java
@SpringBootTest
@ActiveProfiles("test")
class MembershipProcessTest {

    @Autowired private MembershipProcess membershipProcess;
    @Autowired private RuntimeService runtimeService;
    @Autowired private TaskService taskService;
    @Autowired private ProcessEngine processEngine;

    @MockitoBean private ClaimMembershipUseCase claimMembershipUseCase;
    @MockitoBean private SendConfirmationMailUseCase sendConfirmationMailUseCase;
    @MockitoBean private SendRejectionMailUseCase sendRejectionMailUseCase;
    @MockitoBean private SendWelcomeMailUseCase sendWelcomeMailUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine); // BpmnAwareTests.init(...)
    }
}
```

Der Happy Path folgt der Form Given – When – Then. Ersetze jeden Platzhalter `"<…>"` durch die
Element-ID aus deinem Modell:

```java
@Test
void happyPath_membershipIsConfirmedAndWelcomeMailIsSent() {
    // Given: die Kapazitätsprüfung gewährt einen Platz
    when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);

    // When: die Instanz startet und läuft bis zum ersten Wait State
    Membership membership = new Membership(new Email("jane@example.com"), new Name("Jane"), new Age(30));
    membershipProcess.startProcess(membership);
    ProcessInstance instance = findProcessInstance(runtimeService, membership.id().value().toString());
    continueToNextWaitState(processEngine);

    // Then: die Instanz wartet am User Task
    assertThat(instance).isWaitingAt("<user-task-id>");

    // When: der User Task wird abgeschlossen
    String taskId = taskService.createTaskQuery()
            .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
    taskService.complete(taskId);
    continueToNextWaitState(processEngine);

    // Then: die Instanz endet über den Bestätigungspfad
    assertThat(instance)
            .isEnded()
            .hasPassedInOrder("<start>", "<…Aktivitäten des Bestätigungspfads, in Reihenfolge…>", "<confirmed-end>")
            .hasNotPassed("<reject-activity>", "<rejected-end>");

    verify(sendWelcomeMailUseCase).sendWelcomeMail(membership.id());
}
```

### 5. Sehen, was dein Test abdeckt

Lege `src/test/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
mit dieser Zeile an. Sie schaltet die Coverage für jeden Spring-Boot-Test ein; die Testklasse bleibt, wie sie ist:

```text
org.cibseven.community.process_test_coverage.spring_test.platform7.ProcessEngineCoverageConfiguration
```

Führe den Test aus (Befehl unter *Erwartetes Ergebnis*) und öffne
`target/process-test-coverage/io.miragon.training.process.MembershipProcessTest/report.html`.
Grüne Elemente hat ein Test erreicht, weiße nie. Der Ablehnungspfad ist noch weiß: 13 von
17 Elementen (Sequenzflüsse zählen mit), etwa 76 %.

### 6. Ablehnungspfad testen

Schreibe den zweiten Test `noCapacity_membershipIsRejected`: `claimMembership` liefert `false`, die
Instanz läuft ohne Wait State bis `endEvent_membershipRejected`. Prüfe, dass die Ablehnungs-Mail
durchlaufen wurde, Bestätigung und Willkommens-Mail nicht, und dass `sendWelcomeMailUseCase` nie
aufgerufen wurde (`verify(..., never())`). Führe die Testklasse erneut aus und lade den Report neu.

## Randbedingungen

- Der Test braucht keine laufende Anwendung. Die In-Memory-H2 ist pro Lauf frisch, unabhängig von
  `~/.cibseven-training`.
- Gemockt werden nur die Use Cases. Delegates, Modell und Engine laufen echt.

## Erwartetes Ergebnis

Führe die Testklasse aus dem Wurzelverzeichnis aus:

```bash
./mvnw -pl services/process-application test -Dtest=MembershipProcessTest
```

Beide Tests laufen in wenigen Sekunden durch. Schlägt einer fehl, zeigt die Assertion, an welcher
Aktivität die Instanz tatsächlich stand. Der Coverage-Report zeigt das Modell komplett grün.

## Selbstcheck

- [ ] `asyncBefore` steht am Message Start Event und an den drei Mail-Tasks, `asyncAfter` am User Task, `claimMembership` hat keinen Marker
- [ ] Im Testprofil ist der Job Executor abgeschaltet
- [ ] Der Happy-Path-Test prüft die Reihenfolge und die nicht genommenen Pfade
- [ ] Der Ablehnungstest prüft, dass die Willkommens-Mail nie aufgerufen wurde
- [ ] Beide Tests laufen grün, ohne dass die Anwendung gestartet ist
- [ ] Der Coverage-Report zeigt jedes Element des Modells grün

## Hinweise

**Idempotenz:** Ein Retry darf einen Service Task erneut ausführen. Darf eine Aktion nur einmal
passieren (Reservierung, Zahlung), muss sie vor der Grenze committen oder idempotent sein.

**Coverage als Quality Gate (optional):** Eine statische, verschachtelte `@TestConfiguration` mit
einer `ProcessEngineCoverageProperties`-Bean und `assertClassCoverageAtLeast(1.0)` lässt den Build
unter 100 % scheitern. Kommentiere einen Test aus: Der Build schlägt fehl.

## Referenzlösung

`../../solutions/exercise-04/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

Die Lösung enthält beide Varianten: `MembershipProcessPlainTest` mit ID-Strings und
`MembershipProcessTest` mit der generierten Process-API aus dem Add-on.

## Nächster Schritt

Die Element-IDs stehen noch als handgetippte Strings im Test. Das Add-on macht daraus geprüfte
Konstanten.

➡️ [Weiter zum Add-on: bpmn-to-code](exercise-04-addon.md)
