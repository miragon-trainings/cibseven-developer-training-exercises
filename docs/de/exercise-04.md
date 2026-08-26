# Aufgabe 4 – Die Anwendung übernimmt

> **Voraussetzung:** Aufgabe 3 ist abgeschlossen – der Service Task läuft über einen JavaDelegate, gestartet wird der Prozess bisher von Hand im Cockpit.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Message Start Event, Nachrichten-Korrelation, `RuntimeService`, REST-Endpunkt, Persistenz, Bestätigung per REST (`TaskService`).

## Darum geht es

Rose hat das neue **Backroad AL** gelauncht, Miravelo verkauft es exklusiv im Store. Social
Media dreht durch, über Nacht kommen 500 Registrierungen rein.

> *„500 Sign-ups. Das ist entweder viral oder ein Bot-Angriff."*
> — CTO, beim zweiten Kaffee

Zwei Dinge werden schlagartig klar. Erstens: Niemand startet 500 Prozessinstanzen von Hand im
Cockpit – **die Anwendung** muss den Prozess starten, sobald eine Registrierung per REST
hereinkommt. Zweitens: Sind das echte Leute? Die Antwort ist ein **Double-Opt-In** – erst per
Bestätigungslink bestätigen, dann willkommen heißen. Auch dieser Klick auf den Link landet als
REST-Aufruf in deiner Anwendung, nicht als Klick in der Tasklist.

Ab dieser Aufgabe treibt also die Anwendung den Prozess: Sie legt die Membership an, startet
die Instanz über eine **Nachricht** und schließt den Bestätigungsschritt über einen
REST-Endpunkt ab.

## Lernziele

Nach dieser Aufgabe kannst du

- ein None Start Event durch ein **Message Start Event** ersetzen und begründen, warum,
- eine Prozessinstanz aus Java über `createMessageCorrelation(...).correlateStartMessage()` starten,
- die fachlichen Daten persistieren und über die `membershipId` als Prozessvariable referenzieren,
- REST-Endpunkte implementieren, die den Prozess starten und einen Wait State abschließen,
- einen wartenden User Task über den `TaskService` per REST abschließen (statt in der Tasklist).

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-04.svg)

Referenzmodell: `../../models/exercise-04/membership.bpmn`

**Drei Änderungen gegenüber Aufgabe 3:**

1. Das Start Event wird zum **Message Start Event** `startEvent_submitRegistration`
   (`Message_SubscriptionRequested`). Das Start-Formular entfällt – die Daten kommen über den
   REST-Aufruf herein.
2. Vor der Bestätigung kommt ein **Service Task** `serviceTask_sendConfirmationMail` dazu.
3. Der Manual/Service-Weg aus Aufgabe 3 bleibt, aber die Delegates lesen ab jetzt die
   `membershipId` statt der rohen E-Mail-Adresse.

## Aufgabe

### 1. Business-Schicht der Anwendung aktivieren

Die REST- und Persistenz-Klassen sind mit `TODO Exercise 4` auskommentiert. Kommentiere sie ein –
das ist Plumbing, keine Engine-Anbindung:

- `adapter/inbound/rest/MembershipController.java`
- `application/service/RegisterMembershipService.java`
- `adapter/outbound/cibseven/MembershipProcessAdapter.java`
- `adapter/outbound/db/*` (Entity, Mapper, JpaRepository, PersistenceAdapter)
- die Use-Case-Interfaces in `application/port/inbound/*` (`domain/` und die Outbound-Ports sind
  bereits Teil des aktiven Skeletts)
- die Bestätigungs- und Confirm-Klassen (`SendConfirmationMail*`, `ConfirmMembership*`)

Die drei Stellen mit echter Engine-Anbindung (`MembershipProcessAdapter`, die beiden Delegates)
tragen weiterhin ein `TODO` – die schreibst du selbst.

### 2. Modell umbauen

Nimm das Modell aus Aufgabe 3 und ändere es im Miragon BPMN Modeler:

| Änderung | Wert |
|---|---|
| Start Event → **Message Start Event** | ID `startEvent_submitRegistration`, Name „Submit registration form", Message Name `Message_SubscriptionRequested` |
| Start-Formular entfernen | die `email`/`name`/`age`-Felder am Start Event löschen |
| Neuer **Service Task** vor der Bestätigung | ID `serviceTask_sendConfirmationMail`, Name „Send confirmation mail", Delegate Expression `#{sendConfirmationMailDelegate}` |

Der Ablauf ist danach: Message Start → `Send confirmation mail` → `Confirm membership` (User
Task, Wait State) → `Send Welcome Mail` → Ende.

### 3. Registrierung persistieren und Prozess starten

**Datei:** `application/service/RegisterMembershipService.java`

Der REST-Endpunkt `POST /api/memberships` ruft `RegisterMembershipUseCase.register(...)` auf.
Implementiere die Logik: ein `Membership`-Objekt aus dem Command bauen, über das Repository
speichern, den Prozess über den Process-Port starten, `membership.id()` zurückgeben.

### 4. Prozessstart per Korrelation

**Datei:** `adapter/outbound/cibseven/MembershipProcessAdapter.java` – **selbst schreiben.**

Ein Message Start Event lässt sich nicht über `startProcessInstanceByKey` auslösen. Stelle
`startProcess(...)` auf die Korrelation der Nachricht `Message_SubscriptionRequested` um. Der
`RuntimeService` liefert über `createMessageCorrelation(...)` einen Correlation Builder; setze
die vier Prozessvariablen `membershipId`, `email`, `name`, `age`. Die Argumente füllst du selbst:

```java
runtimeService.createMessageCorrelation(/* Message-Name */)
        .setVariables(/* membershipId, email, name, age */)
        .correlateStartMessage();
```

### 5. Delegates auf die `membershipId` umstellen

Bisher las der `SendWelcomeMailDelegate` die rohe `email`. Jetzt referenziert der Prozess die
fachlichen Daten über die persistierte Membership:

- **`SendWelcomeMailDelegate`** und **`SendConfirmationMailDelegate`** lesen die Prozessvariable
  `membershipId` aus der `DelegateExecution`, wandeln sie in eine `MembershipId` und rufen den
  jeweiligen Use Case auf – **das schreibst du selbst.**
- **`SendWelcomeMailService`** und **`SendConfirmationMailService`** laden die Membership über das
  Repository und loggen die E-Mail-Adresse.

### 6. Bestätigung per REST-Endpunkt abschließen

Der Bestätigungslink aus der Mail landet als `POST /api/memberships/{membershipId}/confirm` in
der Anwendung. Dieser Endpunkt schließt den wartenden User Task ab – nicht die Tasklist.

- `MembershipController` bekommt eine Methode `confirm(...)`, die `ConfirmMembershipUseCase` aufruft.
- `ConfirmMembershipService` reicht an den Process-Port weiter.
- **`MembershipProcessAdapter.confirm(...)` – selbst schreiben:** Finde über den `TaskService` den
  offenen Task `userTask_confirmMembership` zur passenden `membershipId` und schließe ihn ab. Die
  API-Kette lautet `taskService.createTaskQuery()...singleResult()` gefolgt von `taskService.complete(...)`;
  die Query-Bedingungen (Task-Definition-Key, Prozessvariable) füllst du selbst.

## Randbedingungen

- Der Prozess-Key bleibt `subscribeNewsletter`, der Message-Name exakt `Message_SubscriptionRequested`
  – historische Namen, die stabil bleiben. Ein Tippfehler führt zur `MismatchingMessageCorrelationException`.
- Die `membershipId` ist ab jetzt die Referenz zwischen Anwendung und Prozessinstanz; sie wird beim
  Start als Prozessvariable gesetzt.
- Der User Task `userTask_confirmMembership` hat in dieser Aufgabe **kein** Formular – er wird per
  REST abgeschlossen. Ein Tasklist-Formular für den Freigabeschritt kommt in [Aufgabe 5](exercise-05.md) dazu.

## Erwartetes Ergebnis

Starte die Anwendung neu und registriere eine Person:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "bob@miravelo.com", "name": "Bob", "age": 25}'
```

1. Der Aufruf liefert die `membershipId` zurück. `Send confirmation mail` läuft durch – im Log
   steht `Sending confirmation mail to bob@miravelo.com`.
2. Die Prozessinstanz wartet am User Task `Confirm membership` (in `act_ru_task` sichtbar).
3. Bestätige über den Endpunkt – mit der ID aus Schritt 1:

   ```bash
   curl -X POST http://localhost:8080/api/memberships/<membershipId>/confirm
   ```

4. `Send Welcome Mail` läuft durch (`Sending welcome mail to bob@miravelo.com`), die Instanz endet.

## Selbstcheck

- [ ] Das Start Event ist ein Message Start Event mit dem Namen `Message_SubscriptionRequested`
- [ ] Ein `POST /api/memberships` legt eine Membership an, startet die Instanz per Korrelation
      und liefert die `membershipId` zurück
- [ ] Beide Delegates lesen `membershipId`; die Services laden die Membership aus dem Repository
- [ ] Ein `POST /api/memberships/{id}/confirm` schließt den wartenden `userTask_confirmMembership`
      über den `TaskService` ab
- [ ] Die Log-Zeilen (Bestätigung, dann Willkommen) erscheinen in der richtigen Reihenfolge
- [ ] `./mvnw -pl services/process-application test -Dtest=ArchitectureTest` ist grün

## Hinweise

**Warum ein Message Start Event?** Ein None Start Event sagt „irgendwer startet hier irgendwie".
Ein Message Start Event benennt den fachlichen Auslöser – *eine Registrierung ist eingegangen* –
und macht ihn im Modell sichtbar. Dieselbe Korrelations-API brauchst du ab [Aufgabe 7](exercise-07.md)
auch für Nachrichten **an laufende Instanzen**.

**Warum die Bestätigung per REST statt per Tasklist?** In Aufgabe 2 war das Tasklist-Formular der
einfache Einstieg. Produktionsnah kommt die Bestätigung aber aus einer eigenen Oberfläche oder –
wie hier – aus einem Bestätigungslink; der Klick landet als REST-Aufruf, und der `TaskService`
schließt den Wait State ab. Fachliche Interaktion läuft ab jetzt über eigene Endpunkte, nicht mehr
über die generische Tasklist.

## Referenzlösung

`../../solutions/exercise-04/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=04
```

## Nächster Schritt

In Aufgabe 5 bekommt der Inner Circle seine Exklusivität – mit Kapazitätsprüfung und Gateway.

➡️ [Weiter zu Aufgabe 5](exercise-05.md)
