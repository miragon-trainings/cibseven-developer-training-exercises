# Aufgabe 3 – Die Anwendung übernimmt

> **Voraussetzung:** Aufgabe 2 ist abgeschlossen – User Task und Service Task laufen, gestartet wird im Cockpit.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Message Start Event, Korrelation, `RuntimeService`, Business Key, REST-Endpunkt, Persistenz, Bestätigung per REST (`TaskService`), Exclusive Gateway mit Default-Flow.

## Darum geht es

Miravelo launcht das neue **Backroad AL**, über Nacht kommen 500 Registrierungen rein. Niemand
startet 500 Instanzen von Hand im Cockpit: Ab jetzt startet **die Anwendung** den Prozess, sobald
eine Registrierung per REST eingeht. Wer sich anmeldet, bestätigt per Link in einer Mail
(Double-Opt-In), und auch dieser Klick kommt als REST-Aufruf an. Außerdem ist der Inner Circle auf
**1000 Plätze** begrenzt: Wer keinen Platz mehr bekommt, erhält eine Absage.

> *„500 Sign-ups. Das ist entweder viral oder ein Bot-Angriff."* – CTO, beim zweiten Kaffee

## Lernziele

Nach dieser Aufgabe kannst du

- eine Prozessinstanz aus Java über ein **Message Start Event** starten und ihr einen **Business Key** geben,
- einen wartenden User Task über den **`TaskService`** per REST abschließen,
- ein **Exclusive Gateway** mit Default-Flow modellieren und mit einer Prozessvariable steuern,
- fachliche Daten persistieren und über die `membershipId` aus dem Prozess heraus referenzieren.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-03.svg)

Referenzmodell: `../../models/exercise-03/membership.bpmn`

## Aufgabe

### 1. Die Anwendungsschicht einkommentieren

Kommentiere alle Dateien mit `TODO Exercise 3` ein. REST, Persistenz und Services sind danach
fertig. Die Stellen mit Engine-Anbindung tragen weiterhin ein `TODO`, die schreibst du in den
Schritten 3 bis 5 selbst:

| Schicht | Dateien |
|---|---|
| REST | `adapter/inbound/rest/MembershipController.java` (`POST /api/memberships`, `POST /api/memberships/{id}/confirm`) |
| Persistenz | `adapter/outbound/db/*` |
| Use Cases + Services | `RegisterMembership*`, `ConfirmMembership*`, `SendConfirmationMail*`, `ClaimMembership*`, `SendRejectionMail*` |
| Engine (mit `TODO`) | `MembershipProcessAdapter`, `SendConfirmationMailDelegate`, `ClaimMembershipDelegate`, `SendRejectionMailDelegate` |

### 2. Das Modell umbauen

Ändere `membership.bpmn` im Modeler. Das Start-Formular und das Formular am User Task entfallen:
Die Daten kommen per REST herein, bestätigt wird per REST.

| Änderung | Element-ID | Name | Konfiguration |
|---|---|---|---|
| Start Event → **Message Start Event** | `startEvent_submitRegistration` | Submit registration form | Message Name `Message_SubscriptionRequested` |
| Neuer Service Task | `serviceTask_claimMembership` | Claim membership | `#{claimMembershipDelegate}` |
| Neues **Exclusive Gateway** | `gateway_hasEmptySpots` | Free seat available? | Default-Flow: Ja-Pfad |
| Ja-Pfad: neuer Service Task vor dem User Task | `serviceTask_sendConfirmationMail` | Send confirmation mail | `#{sendConfirmationMailDelegate}` |
| Nein-Pfad: neuer Service Task | `serviceTask_sendRejectionMail` | Send rejection mail | `#{sendRejectionMailDelegate}` |
| Nein-Pfad: neues End Event | `endEvent_membershipRejected` | Membership rejected | – |
| End Event umbenennen | `endEvent_membershipConfirmed` | Membership confirmed | – |

Der Nein-Pfad bekommt die Bedingung `${!hasEmptySpots}`. Der Ja-Pfad ist der **Default-Flow**: Die
Engine nimmt ihn, wenn keine andere Bedingung zutrifft, er braucht also selbst keine.

### 3. Den Prozess per Korrelation starten

`POST /api/memberships` speichert die Membership und ruft `MembershipProcessAdapter.startProcess(...)`
auf. Implementiere die Methode: Ein Message Start Event startest du, indem du die Nachricht über den
`RuntimeService` korrelierst. Setze die `membershipId` als **Business Key**, damit sich jede Instanz
im Cockpit einer Anmeldung zuordnen lässt, und übergib die vier Prozessvariablen aus dem `TODO`:

```java
runtimeService.createMessageCorrelation(/* Message-Name */)
        .processInstanceBusinessKey(/* membershipId */)
        .setVariables(/* membershipId, email, name, age */)
        .correlateStartMessage();
```

Der Message-Name `Message_SubscriptionRequested` ist wie der Prozess-Key ein historischer Name. Ein
Tippfehler führt zur `MismatchingMessageCorrelationException`.

### 4. Die Delegates auf die `membershipId` umstellen

Ab jetzt referenziert der Prozess die gespeicherte Membership über die Prozessvariable
`membershipId`, nicht mehr über die rohe `email`.

- Stelle `SendWelcomeMailUseCase` auf einen `MembershipId`-Parameter um. `SendWelcomeMailService`
  lädt die Membership über das `MembershipRepository` und loggt die E-Mail-Adresse, wie
  `SendConfirmationMailService` es vormacht.
- Implementiere `SendWelcomeMailDelegate`, `SendConfirmationMailDelegate` und
  `SendRejectionMailDelegate`: `membershipId` lesen, in eine `MembershipId` umwandeln, Use Case
  aufrufen.

### 5. Den Claim-Delegate schreiben

Implementiere `ClaimMembershipDelegate`: Er ruft `ClaimMembershipUseCase` auf und schreibt das
Ergebnis als Prozessvariable `hasEmptySpots` zurück. Das Gateway entscheidet anhand dieser
Variable. Das Setzen gehört in den Delegate, weil der Service die Engine nicht kennt und nur ein
`boolean` zurückgibt.

### 6. Die Bestätigung per REST abschließen

Der Bestätigungslink landet als `POST /api/memberships/{membershipId}/confirm` in der Anwendung, der
Controller reicht bis `MembershipProcessAdapter.confirm(...)` durch. Implementiere die Methode: Suche
über den `TaskService` den offenen Task `userTask_confirmMembership`, dessen Instanz die
`membershipId` als Business Key trägt, und schließe ihn ab. Gibt es keinen, wirf eine Exception.

```java
var task = taskService.createTaskQuery()
        /* Task-Definition-Key, Business Key */
        .singleResult();
taskService.complete(/* Task-ID */);
```

## Randbedingungen

- Die Kapazität ist ein Zähler im Arbeitsspeicher (`MAX_SPOTS = 1000` in `ClaimMembershipService`)
  und steht nach einem Neustart wieder bei null.
- `domain/` und die Outbound-Ports sind schon aktiv, du änderst sie nicht.

## Erwartetes Ergebnis

Starte die Anwendung neu und registriere Bob:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "bob@miravelo.com", "name": "Bob", "age": 25}'
```

Der Aufruf liefert die `membershipId`. Im Log stehen `Claiming membership for bob@miravelo.com`
und `Sending confirmation mail to bob@miravelo.com`. Im Cockpit wartet die Instanz am User Task und
trägt die `membershipId` als Business Key. Bestätige mit der ID:

```bash
curl -X POST http://localhost:8080/api/memberships/<membershipId>/confirm
```

Im Log folgt `Sending welcome mail to bob@miravelo.com`, die Instanz endet an `Membership confirmed`.

Prüfe danach den Nein-Pfad: Setze `MAX_SPOTS` vorübergehend auf `0`, starte neu und registriere
Dave (`dave@miravelo.com`). Im Log steht `Sending rejection mail to dave@miravelo.com`, die Instanz
endet an `Membership rejected`, ohne am User Task zu warten.

## Selbstcheck

- [ ] Das Start Event ist ein Message Start Event mit `Message_SubscriptionRequested`
- [ ] `POST /api/memberships` startet eine Instanz mit der `membershipId` als Business Key
- [ ] `POST /api/memberships/{id}/confirm` schließt den wartenden User Task über den `TaskService` ab
- [ ] Das Gateway hat einen Default-Flow und genau eine Bedingung (`${!hasEmptySpots}`)
- [ ] Alle Delegates lesen `membershipId`, nur der Claim-Delegate setzt eine Variable
- [ ] `./mvnw -pl services/process-application test -Dtest=ArchitectureTest` ist grün

## Hinweise

Warum ein Message Start Event? Es benennt den fachlichen Auslöser (*eine Registrierung ist
eingegangen*) im Modell. Dieselbe Korrelations-API brauchst du in [Aufgabe 5](exercise-05.md) für
Nachrichten an **laufende** Instanzen.

## Referenzlösung

`../../solutions/exercise-03/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=03
```

## Nächster Schritt

Der Prozess hat jetzt zwei Ausgänge, und niemand prüft automatisch, ob er den richtigen nimmt. In
Aufgabe 4 sicherst du ihn mit einem Prozess-Test ab.

➡️ [Weiter zu Aufgabe 4](exercise-04.md)
