# Aufgabe 10 – Die Engine als geteilte Infrastruktur

> **Voraussetzung:** Aufgabe 9 ist abgeschlossen – der vollständige Membership-Prozess läuft, inklusive des `notifyCommunity`-Zweigs am Parallel Gateway aus Aufgabe 7.
> **Arbeitsverzeichnis:** `services/process-application` (Teil A) und `services/logistics-service` (Teil B, wird in dieser Aufgabe angelegt)
> **Neu in dieser Aufgabe:** Signal-End-Event und Signal-Start-Event, External Task, ein zweiter Service als Prozess-Owner, generierter Engine-Client aus der OpenAPI-Spec.

## Darum geht es

Die Engine hat sich im Unternehmen etabliert. Immer mehr Abteilungen wollen sie nutzen –
aber niemand will eine **eigene** Engine betreiben.

Also zeigen wir, dass die Engine eine wiederverwendbare Infrastruktur-Komponente ist: Der
Membership-Prozess wirft beim Aktivieren ein **Signal**.

> **Begriff: Signal und Broadcast.** Ein Signal ist ein **Broadcast** – anders als eine
> Nachricht (die du seit [Aufgabe 4](exercise-04.md) kennst) richtet es sich nicht an eine
> bestimmte Prozessinstanz, sondern an alle, die darauf hören. Der Werfer kennt seine
> Empfänger nicht und wartet auf keine Antwort: 1 Sender, n Empfänger.

Die **Logistik-Abteilung** betreibt einen eigenen Service mit einem eigenen Prozess
`sendWelcomeKit`. Dieser Service **besitzt das Modell**, **deployt es selbst** in die
geteilte Engine, wird über ein **Signal-Start-Event** vom Broadcast gestartet und verschickt
ein Welcome-Kit.

**Der Kernpunkt ist nicht der Mechanismus, sondern die Eigentümerschaft.** External Task
heißt nur: Die Engine legt einen Task ab, ein Worker holt ihn per REST und meldet zurück.
Das sagt nichts darüber, **wem der Prozess gehört**. Hier gehört er vollständig dem
Logistik-Service – Modell, Worker, Deployment und Tests liegen bei ihm. Die geteilte Engine
führt ihn nur aus.

## Lernziele

Nach dieser Aufgabe kannst du

- ein End Event zu einem Signal-End-Event machen und ein Signal mit Payload werfen,
- einen Prozess über ein Signal-Start-Event auf ein Broadcast reagieren lassen,
- einen Service Task als External Task auslegen und mit einem Worker erfüllen,
- einen typisierten Engine-Client aus der offiziellen OpenAPI-Spec generieren und die Engine
  damit über `/engine-rest` ansteuern,
- ein Modell aus einem fremden Service heraus idempotent deployen,
- die Frage „wem gehört der Prozess?" vom Mechanismus External Task trennen.

## Ziel-Architektur

Ab dieser Aufgabe laufen **zwei** Anwendungen: der bisherige Engine-Host und ein zweiter
Service, der einer anderen Abteilung gehört. Die Engine bleibt dabei genau eine:

```
process-application  (generischer Engine-Host — eingebettete Engine + /engine-rest + Cockpit, :8080)
  • besitzt den Membership-Prozess; der In-Engine-Zweig "Notify community" (Teams) bleibt
  • neu (additiv): das terminale End Event "Membership activated" wirft Signal_MemberActivated {name}
  • kennt die Logistik nicht und trägt kein send-welcome-kit.bpmn

logistics-service  (Remote-Owner — eigene JVM, :8090)
  • generierter, typisierter Client (openapi-generator aus cibseven-engine-rest-openapi)
  • deployt send-welcome-kit.bpmn beim Start per REST in die Engine (idempotent)
  • erfüllt den Service Task shipWelcomeKit als External Task   (Richtung 1: Engine → Worker)
  • steuert die Engine über den generierten Client an             (Richtung 2: Worker → Engine)
  • besitzt seine eigenen Tests (In-Memory-Engine nur im Test-Scope)
```

## Ziel-Modell

Zwei Prozessmodelle, die sich nur über ein Signal kennen – der Host weiß nicht, dass es die
Logistik gibt, und die Logistik kennt den Membership-Prozess nicht:

Membership-Prozess (`subscribeNewsletter`, Engine-Host) – das terminale End Event „Membership
activated" wirft `Signal_MemberActivated`:

![BPMN Membership-Prozess](../assets/exercise-10-main.svg)

Logistik-Prozess (`sendWelcomeKit`, im logistics-service modelliert und deployt) – Signal-Start
plus manueller Start, dann der External Task `shipWelcomeKit`:

![BPMN send-welcome-kit](../assets/exercise-10-sub.svg)

Referenzmodelle: `../../models/exercise-10/membership.bpmn`,
`../../models/exercise-10/send-welcome-kit.bpmn`

Das Parallel Gateway aus Aufgabe 7 bleibt unverändert – nur das terminale End Event
„Membership activated" wird zum Signal-End-Event. Es beendet den Prozess **und** wirft das
Signal; ein neuer Zweig entsteht nicht.

## Aufgabe

### 0. Baseline sicherstellen

Aufgabe 10 baut auf dem fertigen Membership-Prozess auf. Hast du die Aufgaben 1–9
durchgearbeitet, ist `services/process-application` bereits im richtigen Zustand. Steigst du
direkt hier ein, hol dir zuerst die Baseline:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=09
```

### 1. Neue Abteilung anlegen

Bis hierher gab es in `services/` nur die `process-application`. Jetzt kommt der Service der
Logistik dazu:

```bash
cp -R templates/exercise-10/logistics-service services/logistics-service
```

Trage das Modul in der Root-`pom.xml` unter `<modules>` ein:

```xml
<module>services/logistics-service</module>
```

Ab jetzt baut `./mvnw` den neuen Service mit. Er kompiliert schon im Ausgangszustand – der
Client-Teil ist noch auskommentiert.

### Teil A – Engine-Host

### 2. End Event zum Signalwerfer machen

Im Host ist genau eine Änderung nötig. Mache das terminale End Event
`endEvent_membershipActivated` (nach dem Join) zu einem **Signal-End-Event**. `Send Welcome
Mail` und `Notify community` bleiben unverändert, es kommt **kein** neues Element dazu.

Alle Änderungen machst du im **Miragon BPMN Modeler**, nicht im XML: End Event auswählen →
in ein **Signal-End-Event** umwandeln → Signal `Signal_MemberActivated` anlegen/auswählen →
`asyncBefore` setzen. Die Payload (`name`) gibt das End Event über ein **In Mapping**
(`camunda:in`) mit. Im XML entsteht dabei:

```xml
<bpmn:endEvent id="endEvent_membershipActivated" name="Membership activated" camunda:asyncBefore="true">
  <bpmn:signalEventDefinition signalRef="Signal_MemberActivated">
    <bpmn:extensionElements>
      <camunda:in source="name" target="name" />
    </bpmn:extensionElements>
  </bpmn:signalEventDefinition>
</bpmn:endEvent>
```

Legst du das Signal im Modeler an, entsteht die Deklaration auf Definitions-Ebene automatisch:

```xml
<bpmn:signal id="Signal_MemberActivated" name="Signal_MemberActivated" />
```

Kein `RuntimeService`, kein Delegate – die Engine wirft das Signal nativ. Der Host ruft nur
„neues Mitglied aktiviert" in den Raum; wer darauf reagiert, ist ihm egal.

### Teil B – Logistik-Service

Hier steckt die eigentliche Arbeit. Die Reihenfolge ist bewusst gewählt: erst den Prozess
modellieren, dann die APIs generieren, dann den Code schreiben. Arbeite die
`TODO Exercise 10`-Stellen der Reihe nach ab.

### 3. Prozess modellieren

**Datei:** `src/main/resources/bpmn/send-welcome-kit.bpmn` – sie enthält bewusst nur ein
leeres Modell mit einem Start Event. Modelliere den Prozess selbst; das ist dein
Abschlusstest, ob das Gelernte sitzt.

Alle Attribute in der Tabelle setzt du im **Miragon BPMN Modeler** (Element auswählen →
Properties Panel), nicht im XML.

| Element | Typ | ID | Konfiguration |
|---|---|---|---|
| Prozess | – | `sendWelcomeKit` | `isExecutable="true"`, `historyTimeToLive` gesetzt |
| Produktions-Start | Signal Start Event | `startEvent_memberActivated` | Signal `Signal_MemberActivated`, `asyncBefore="true"` |
| Manueller Start | None Start Event | `startEvent_manualStart` | für Test und erneuten Versand |
| Zusammenführung | Exclusive Gateway | `gateway_start` | führt beide Starts zusammen |
| Versand | Service Task | `serviceTask_shipWelcomeKit` | Implementation **External**, Topic `shipWelcomeKit` |
| Ende | End Event | `endEvent_welcomeKitShipped` | – |

**Warum zwei Start Events?** Das Signal-Start-Event ist der Produktions-Trigger. Das leere
Start Event erlaubt einen Start per `startProcessInstanceByKey` über die REST-API – etwa um
ein Kit erneut zu verschicken oder wenn das Signal einmal nicht durchkommt.

### 4. APIs generieren

Aktiviere in der `pom.xml` die beiden auskommentierten Generator-Blöcke:

- **Process-API** (`bpmn-to-code`) – erzeugt aus deinem External Task die Konstante
  `SendWelcomeKitProcessApi.ServiceTasks.SHIP_WELCOME_KIT`.
- **Engine-Client** (`openapi-generator`) – erzeugt aus der offiziellen OpenAPI-Spec von
  CIB Seven einen typisierten `/engine-rest`-Client statt handgeschriebener REST-Aufrufe.
  Setze die beiden `TODO`-Werte: `generatorName` = `java`, `library` = `restclient`.

```bash
./mvnw -pl services/logistics-service generate-sources
```

Danach liegen `org.cibseven.rest.client.api` / `.model` unter `target/…` und
`SendWelcomeKitProcessApi` unter `src`.

### 5. Modell deployen

**Klasse:** `EngineDeploymentAdapter` – schickt beim Start das eigene
`send-welcome-kit.bpmn` per REST in die Engine. **Idempotent**: Ein Neustart darf kein
zweites Deployment erzeugen.

### 6. Worker schreiben

**Klasse:** `ShipWelcomeKitWorker` – absichtlich leer. Mach sie zur Bean (`@Component`),
abonniere den Topic
(`@ExternalTaskSubscription(topicName = SendWelcomeKitProcessApi.ServiceTasks.SHIP_WELCOME_KIT)`),
lass sie von `BaseExternalTaskWorker` erben, lies die Variable `name`, verschicke das Kit
über den Use Case und schließe den Task ab.

### 7. Engine über den Client ansteuern

**Klassen:** `EngineClientConfig` und `RemoteWelcomeKitProcessAdapter` – stelle die
`ProcessDefinitionApi`-Bean bereit und starte den Prozess per `startProcessInstanceByKey`.
Das nutzt das manuelle Start Event und steckt hinter der Aktion `POST /api/welcome-kits`.

## Randbedingungen

- Der Host trägt **kein** `send-welcome-kit.bpmn`. Wenn es dort landet, ist die Aussage der
  Aufgabe kaputt.
- CIB Seven läuft weiterhin eingebettet im Host. „Remote" ist die Sicht des **Clients**; eine
  echte Standalone-Engine (`cibseven/cibseven:run`) ergäbe dasselbe Bild mit ausgetauschtem Host.
- Der Logistik-Service läuft auf Port `8090`, der Host auf `8080`.
- Der Aufbau orientiert sich am Blueprint
  [`miragon-blueprints/cibseven-remote-example`](https://github.com/miragon-blueprints/cibseven-remote-example)
  (dort Kotlin/Gradle, hier Java/Maven).

## Erwartetes Ergebnis

### Automatisiert, ohne laufende Engine

Der Logistik-Service testet jede Naht selbst – Worker-Unit-Test, Prozesstest auf der
In-Memory-Engine (Signal-Start → External Task als Wait State → complete), Deployment- und
Remote-Adapter-Test gegen einen HTTP-Stub (`MockRestServiceServer`):

```bash
./mvnw -pl solutions/exercise-10/logistics-service test
./mvnw -pl solutions/exercise-10/process-application test -Dtest=MembershipProcessTest
```

### End-to-End mit beiden Services

Für den vollständigen Durchlauf brauchst du drei Terminals: eines für den Stack, je eines
für die beiden Anwendungen. Arbeite die Schritte der Reihe nach ab:

```bash
# 1. Stack und Engine-Host (:8080) starten — Docker oder Podman
cd stack && docker compose up -d      # Podman: podman compose up -d
cd ../solutions/exercise-10/process-application && ../../../mvnw spring-boot:run

# 2. Logistik-Service (:8090) in einem zweiten Terminal – er deployt sein Modell beim Start
cd solutions/exercise-10/logistics-service && ../../../mvnw spring-boot:run

# 3. Beweis, dass der Remote-Service das Modell deployt hat:
curl http://localhost:8080/engine-rest/deployment

# 4. Mitglied anlegen, im Cockpit (http://localhost:8080/webapp/#/seven/auth/start, admin/admin) die
#    Confirm-Aufgabe abschließen → das Signal feuert → eine sendWelcomeKit-Instanz läuft
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","name":"Jane","age":30}'

# 5. Welcome-Kit erneut senden (steuert die Engine über den generierten Client an):
curl -X POST http://localhost:8090/api/welcome-kits \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane"}'
```

**Der entscheidende Beweis:** Stoppe den Logistik-Service und aktiviere ein Mitglied. Im
Cockpit wartet eine `sendWelcomeKit`-Instanz am External Task. Starte den Logistik-Service –
er holt den Task ab und verschickt das Kit.

## Selbstcheck

- [ ] `send-welcome-kit.bpmn` liegt **nur** im Logistik-Service
- [ ] Es erscheint in den Deployments der Engine **erst**, nachdem der Logistik-Service
      gestartet wurde (`GET /engine-rest/deployment` oder Cockpit)
- [ ] Ein Neustart des Logistik-Services erzeugt **kein** zweites Deployment
- [ ] Ein aktiviertes Mitglied löst `Signal_MemberActivated` aus, eine `sendWelcomeKit`-Instanz
      läuft, der Worker verschickt das Kit
- [ ] Die Membership-Aktivierung wartet nicht auf die Logistik
- [ ] Die Tests des Logistik-Services sind grün, **ohne** dass der Engine-Host läuft

## Hinweise

**Signal-Broadcast ist im Werfer synchron.** In CIB Seven und Camunda 7 wird ein Signal **in
der Transaktion des Werfers** zugestellt. Ohne Marker würde das Signal-End-Event die
`sendWelcomeKit`-Instanz anlegen und synchron bis zum External Task ausführen – alles in der
Aktivierungstransaktion der Membership. Ein Fehler dort (Prozess noch nicht deployt, Race
beim Start) würde die Aktivierung mit zurückrollen. Deshalb stehen **zwei** Grenzen:
`asyncBefore` am Signal-End-Event `endEvent_membershipActivated` und `asyncBefore` am
Signal-Start-Event `startEvent_memberActivated` im Logistik-Prozess. Erst damit gilt „die
Aktivierung wartet nicht auf die Logistik" auch **vor** dem External Task.

**Transaktionsgrenze am External Task (Anknüpfung an Aufgabe 5):** Der External Task ist die
Commit-Grenze zwischen Engine und Worker. Die Engine committet, sobald sie den Task anlegt,
und wartet als Wait State. Der Worker holt ihn per `fetchAndLock`, arbeitet in **seiner
eigenen** Transaktion und meldet erst `complete` oder `handleFailure` zurück. Ein
fehlgeschlagener `shipWelcomeKit` rollt in der Engine **nichts** zurück; ob und wie oft neu
versucht wird, ist eine Worker-Entscheidung. Das ist der bewusste Gegenpol zum
`asyncBefore`-Muster: Dort setzt *das Modell* die Grenze, hier bringt der Mechanismus sie
mit – und die Fehlerbehandlung wandert zum Owner.

**Für Trainer:** Signal heißt 1:N-Broadcast. Als Erweiterung kann eine **zweite** Abteilung
(etwa Analytics mit `recordSignup`) auf **dasselbe** Signal hören – ein weiterer
Remote-Service, der seinen eigenen Prozess besitzt und deployt. Genau das führt „eine
Engine, viele Abteilungen" live vor.

## Referenzlösung

- Engine-Host: `../../solutions/exercise-10/process-application/`
- Logistik-Service: `../../solutions/exercise-10/logistics-service/` (enthält auch den
  generierten Engine-Client – ein separates Client-Modul gibt es nicht)
- Nur das fertige Ergebnis im Arbeitsmodul laufen lassen:
  `./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=10`

## Nächster Schritt

🎉 **Geschafft!** Du hast einen Prozess gebaut, den ein eigener Remote-Service besitzt und
deployt – und die Engine dabei als wiederverwendbare Infrastruktur erlebt. Lust auf mehr?
Die Extra-Aufgabe baut den Prozess engine-neutral um.

➡️ [Weiter zur Extra-Aufgabe 1](extra-task-1.md)
