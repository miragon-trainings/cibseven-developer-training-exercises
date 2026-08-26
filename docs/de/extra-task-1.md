# Extra-Aufgabe 1 – Raus aus dem Engine-Lock-in

> **Voraussetzung:** Aufgabe 10 ist abgeschlossen – der Membership-Prozess läuft vollständig, inklusive Signal-End-Event „Membership activated".
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Process-Engine-API von bpm-crafters, Worker-Pattern statt JavaDelegate, engine-neutraler Outbound-Adapter, ArchUnit-Guardrail.

## Darum geht es

**Strategie-Meeting. Der Prozess läuft. Jemand stellt die unbequeme Frage.**

> *„Schön, dass alles läuft. Aber wir haben unsere komplette Fachlogik an eine Engine
> getackert. Was passiert, wenn wir in zwei Jahren wechseln müssen?"*
> — Die Person, die schon mal eine Migration mitgemacht hat.

Camunda 7 ist End-of-Life, wir sind auf **CIB Seven** umgestiegen – ein gepflegter Fork,
gute Wahl. Aber unser Code weiß das ein bisschen *zu* genau: Jeder Service Task hängt an
einem `JavaDelegate` mit `org.cibseven.bpm.engine.delegate.DelegateExecution`, der
Prozess-Adapter ruft den `RuntimeService` direkt auf. Ein Wechsel auf Camunda 8 oder
Operaton hieße: jeden Delegate und jeden Engine-Aufruf anfassen.

Die [**Process-Engine-API**](https://github.com/bpm-crafters/process-engine-api) von
bpm-crafters ist eine engine-neutrale Abstraktionsschicht – so, wie JPA die Datenbank
abstrahiert. Sie bringt Adapter für verschiedene BPMN-Engines mit (CIB Seven, Camunda 7,
Camunda 8, Operaton). Ein Engine-Wechsel wird damit – stark vereinfacht – zum **Tausch eines
Adapters**. In der Realität nie ganz so einfach, aber deutlich einfacher als sonst.

Das Beste daran: **Domain, Application-Services und Ports bleiben unangetastet.** Sie waren
schon immer engine-neutral – genau dafür gibt es die hexagonale Architektur. Du fasst nur
die Adapter-Schicht an.

## Lernziele

Nach dieser Aufgabe kannst du

- benennen, wo native Engine-Kopplung im Code entsteht (`JavaDelegate`, `DelegateExecution`,
  `RuntimeService`),
- Service Tasks über das Worker-Pattern (`@ProcessEngineWorker`) statt über
  `DelegateExpression` anbinden,
- Prozesse engine-neutral starten und Nachrichten korrelieren (`StartProcessApi`,
  `CorrelationApi`),
- begründen, warum External Tasks die `asyncBefore`-Marker überflüssig machen,
- per Architektur-Test garantieren, dass kein `org.cibseven.bpm`-Import mehr in den Code leakt.

## Ziel-Modell

Das Prozessmodell ändert sich **fachlich nicht**. Es ist exakt der Prozess aus Aufgabe 10 –
nur die technische Anbindung der Service Tasks wechselt. Das Signal-End-Event „Membership
activated" bleibt ein natives BPMN-Throw ohne Delegate und passt damit ohnehin in die
engine-neutrale Welt.

Hauptprozess:

![BPMN-Hauptprozess](../assets/extra-task-1-main.svg)

Aufgerufener Prozess `handleRejection`:

![BPMN-Subprozess](../assets/extra-task-1-sub.svg)

Was sich ändert – und was nicht:

| Schicht | Aufgabe 10 (nativ CIB Seven) | Extra-Aufgabe 1 |
|---|---|---|
| `domain/`, `application/` | unverändert | **unverändert** |
| Inbound Service Tasks | `JavaDelegate` + `DelegateExecution` | `@ProcessEngineWorker`-Worker |
| Outbound Prozess-Adapter | `RuntimeService.createMessageCorrelation(...)` | `StartProcessApi` / `CorrelationApi` |
| BPMN Service Tasks | `camunda:delegateExpression="#{xDelegate}"` | `camunda:type="external"` + `camunda:topic` |
| Bootstrap | `@EnableProcessApplication` | entfällt – der Adapter übernimmt Deployment und Worker |

Message Start, Boundary Events, Subprozess, Call Activity, DMN und Kompensation bleiben
strukturell gleich. DMN und User Tasks laufen weiterhin in der Engine; dafür braucht es
keine Worker.

## Aufgabe

> Am einfachsten kopierst du deine Aufgabe-9-Lösung und baust sie Schritt für Schritt um.
> Der Logistik-Service aus Aufgabe 10 bleibt dabei **unverändert** und ist nicht Teil dieser
> Aufgabe.

### 1. Dependencies einbinden

In die `pom.xml` des Moduls:

- `dev.bpm-crafters.process-engine-api:process-engine-api`
- `dev.bpm-crafters.process-engine-worker:process-engine-worker-spring-boot-starter`
- `dev.bpm-crafters.process-engine-adapters:process-engine-adapter-cib-seven-embedded-spring-boot-starter`

Der CIB-Seven-Embedded-Adapter ist genau die Abhängigkeit, die du beim Engine-Wechsel gegen
einen anderen Adapter tauschen würdest – Worker und Ports bleiben, wie sie sind.

### 2. Service Tasks auf External Tasks umstellen

Auch das ist Modellierungsarbeit im **Miragon BPMN Modeler**, nicht im XML: Service Task
auswählen → Properties Panel → **Implementation** auf **External** → Topic setzen → im
Abschnitt **Input/Output** ein Input-Mapping anlegen, damit der Worker die `membershipId`
bekommt. Aus

```xml
<bpmn:serviceTask id="serviceTask_sendConfirmationMail" name="Send confirmation mail"
                  camunda:delegateExpression="#{sendConfirmationMailDelegate}">
```

wird so ein External Task mit Topic und Input-Mapping – im XML:

```xml
<bpmn:serviceTask id="serviceTask_sendConfirmationMail" name="Send confirmation mail"
                  camunda:type="external" camunda:topic="sendConfirmationMail">
  <bpmn:extensionElements>
    <camunda:inputOutput>
      <camunda:inputParameter name="membershipId">${membershipId}</camunda:inputParameter>
    </camunda:inputOutput>
  </bpmn:extensionElements>
```

Stelle alle sieben Service Tasks um: `claimMembership`, `sendConfirmationMail`,
`sendWelcomeMail`, `sendRejectionMail`, `reSendConfirmationMail`, `revokeClaim` (auch als
Kompensations-Handler) und `notifyCommunity`.

### 3. Delegates durch Worker ersetzen

Aus dem `JavaDelegate`

```java
@Component
public class SendConfirmationMailDelegate extends BaseDelegate {
    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        useCase.sendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
```

wird ein engine-neutraler Worker – **ohne** `org.cibseven.bpm`-Import:

```java
@Component
public class SendConfirmationMailWorker {

    private final SendConfirmationMailUseCase useCase;

    public SendConfirmationMailWorker(SendConfirmationMailUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_CONFIRMATION_MAIL)
    public void sendConfirmationMail(@Variable(name = "membershipId") String membershipId) {
        useCase.sendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
```

Der `claimMembership`-Worker gibt – anders als die übrigen – ein Ergebnis zurück, das das
Gateway auswertet:

```java
@ProcessEngineWorker(topic = ServiceTasks.CLAIM_MEMBERSHIP)
public Map<String, Object> claimMembership(@Variable(name = "membershipId") String membershipId) {
    var hasEmptySpots = useCase.claimMembership(new MembershipId(UUID.fromString(membershipId)));
    return Map.of("hasEmptySpots", hasEmptySpots);
}
```

### 4. Outbound-Adapter umstellen

Statt des `RuntimeService` injizierst du `StartProcessApi` und `CorrelationApi`:

```java
@Override
public void startProcess(Membership membership) {
    var membershipId = membership.id().value().toString();
    startProcessApi.startProcess(new StartProcessByMessageCmd(
            Messages.MESSAGE_SUBSCRIPTION_REQUESTED.getValue(),
            Map.of(
                    "membershipId", membershipId,
                    "email", membership.email().value(),
                    "name", membership.name().value(),
                    "age", membership.age().value(),
                    CommonRestrictions.CORRELATION_KEY, membershipId
            )
    )).join();
}

@Override
public void rejectMembership(MembershipId membershipId) {
    var id = membershipId.value().toString();
    correlationApi.correlateMessage(new CorrelateMessageCmd(
            Messages.MESSAGE_CONFIRMATION_REJECTED.getValue(),
            Map.of("membershipId", id),
            Correlation.withKey(id),
            CommonRestrictions.builder().withRestriction("useGlobalCorrelationKey", "true").build()
    )).join();
}
```

### 5. Bootstrap und Konfiguration anpassen

Entferne `@EnableProcessApplication` aus der `TrainingApplication` – der Adapter übernimmt
Deployment und Worker-Registrierung.

Stelle einen `EngineCommandExecutor` als Bean bereit, damit Engine- und Business-Daten in
**einer** Transaktion committen:

```java
@Bean
public EngineCommandExecutor engineCommandExecutor() {
    return new EngineCommandExecutor(Runnable::run);
}
```

`Runnable::run` führt den Engine-Command synchron im aufrufenden Thread aus – Engine-Fortschritt
und Fachdaten committen oder rollen gemeinsam. Ein eigener Thread-Pool würde diese Grenze
zerschneiden. Das ist die direkte Fortsetzung des Themas aus [Aufgabe 6](exercise-06.md).

Ergänze in der `application.yaml` den Worker- und Adapter-Block:

```yaml
dev:
  bpm-crafters:
    process-api:
      worker:
        deployment:
          enabled: true
          bpmnResourcePattern: "classpath*:/**/*.bpmn"
          dmnResourcePattern: "classpath*:/**/*.dmn"
      adapter:
        cib-seven-embedded:
          enabled: true
          service-tasks:
            delivery-strategy: embedded_scheduled
            worker-id: extra-task-1-worker
            schedule-delivery-fixed-rate-in-seconds: 5
```

### 6. Guardrail setzen

Ein ArchUnit-Test macht die Kernaussage prüfbar: **Nirgends** im Java-Code darf noch ein
`org.cibseven.bpm`-Import stehen.

```java
@ArchTest
static final ArchRule no_class_should_depend_on_the_native_engine = noClasses()
        .should().dependOnClassesThat().resideInAPackage("org.cibseven.bpm..");
```

Ist der Test grün, lebt die Engine nur noch in `pom.xml` und `application.yaml` – genau
das, was bei einem Wechsel angefasst werden müsste.

## Randbedingungen

- **Die `asyncBefore`-Marker entfallen.** Ein External Task ist von Natur aus ein Wait
  State: Die Engine committet, sobald sie den Task anlegt, und wartet, bis ein Worker ihn
  fetcht und completet. Die Transaktionsgrenze, die du in Aufgabe 6 und 7 von Hand gesetzt
  hast, bringt der External Task eingebaut mit. Darin liegt ein Teil des Gewinns.
- Die Topic-Konstanten (`ServiceTasks.SEND_CONFIRMATION_MAIL`) stammen aus der generierten
  Process-API, die du seit [Aufgabe 6](exercise-06.md) kennst. Das Plugin ist bereits
  eingerichtet; zur Not gehen auch schlichte Strings.
- Die REST-Schnittstelle bleibt identisch zu Aufgabe 10 (Port `8080`). Service Tasks werden
  jetzt per Polling abgearbeitet (etwa alle 5 Sekunden) – es kann also einen Moment dauern.

## Erwartetes Ergebnis

Fahre dieselben beiden Szenarien wie in Aufgabe 9 – das Ergebnis muss identisch sein, nur
die Ausführung läuft jetzt über Worker statt über Delegates.

**Ablehnung außerhalb der Zielgruppe:**

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "grace@miravelo.com", "name": "Grace", "age": 35}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Im Cockpit (`http://localhost:8080/webapp/#/seven/auth/start`, admin/admin): Der Worker
`sendConfirmationMail` feuert, der User Task *Confirm membership* erscheint. Nach dem
Rückzug läuft die Call Activity `handleRejection`, danach feuert über die Kompensation der
Worker `revokeClaim`.

**Ablehnung innerhalb der Zielgruppe:**

```bash
MEMBERSHIP_ID=$(curl -s -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "hanna@miravelo.com", "name": "Hanna", "age": 25}')

curl -X POST http://localhost:8080/api/memberships/$MEMBERSHIP_ID/reject
```

Hier wartet der aufgerufene Prozess zusätzlich am User Task *Write an email expressing
regret*.

## Selbstcheck

- [ ] Alle sieben Service Tasks sind `camunda:type="external"` mit Topic
- [ ] Es gibt keine `JavaDelegate`-Klassen mehr, nur noch `@ProcessEngineWorker`-Worker
- [ ] Der Outbound-Adapter nutzt `StartProcessApi` / `CorrelationApi` statt `RuntimeService`
- [ ] `@EnableProcessApplication` ist entfernt, der `EngineCommandExecutor`-Bean ist gesetzt
- [ ] Der ArchUnit-Test meldet **null** Abhängigkeiten auf `org.cibseven.bpm`
- [ ] Das fachliche Verhalten ist identisch zu Aufgabe 10

## Hinweise

Als Vorlage dient das Repository [**engine-safari**](https://github.com/emaarco/engine-safari),
Modul `cib-seven-with-process-engine-api`.

## Referenzlösung

`../../solutions/extra-task-1/`

## Nächster Schritt

🦁 **Geschafft!** Dein Prozess ist engine-neutral. CIB Seven läuft weiter unter der Haube –
aber dein Code weiß nichts mehr davon. Ein Engine-Wechsel ist damit kein Code-Umbau mehr,
sondern ein Adapter-Tausch.
