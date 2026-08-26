# Aufgabe 5 – Kapazitätsprüfung mit Gateway

> **Voraussetzung:** Aufgabe 4 ist abgeschlossen – Double-Opt-In läuft, der Prozess startet per Nachricht.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Exclusive Gateway, alternativer Prozessausgang, Business Key, generiertes Task-Formular.

## Darum geht es

**Strategie-Meeting, Freitagnachmittag. Jemand hat exklusiven Matcha Latte mitgebracht.**

Der **Miravelo Inner Circle** bekommt seine harte Grenze: Tausend Plätze. Mehr nicht.

Warum tausend? Weil Knappheit Wert erzeugt. Weil FOMO ein Geschäftsmodell ist. Weil
irgendjemand ein Buch über Luxusmarken gelesen hat.

> *„Wir sind nicht exklusiv, weil wir gut sind. Wir sind exklusiv, weil der Counter in der
> Datenbank auf 1000 steht."*
> — Ehrlichster Kommentar im Sprint Planning

Aus Prozesssicht ist das ein **Gateway**: Platz bekommen? Weiter im Text. Kein Platz?
Ablehnungsmail. Und weil jede Anmeldung ein fachliches Objekt mit eigener ID ist,
bekommt jede Prozessinstanz einen **Business Key** – Schluss mit „welche der 40 laufenden
Instanzen war noch mal Carol?".

## Lernziele

Nach dieser Aufgabe kannst du

- ein Exclusive Gateway modellieren, seine Bedingungen setzen und einen Default-Flow wählen,
- einen alternativen Prozessausgang (Ablehnung) umsetzen,
- eine Entscheidung aus Java-Code als Prozessvariable an das Gateway übergeben,
- einer Prozessinstanz einen Business Key zuordnen,
- einem User Task ein generiertes Task-Formular für einen Freigabeschritt geben.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-05.svg)

Referenzmodell: `../../models/exercise-05/membership.bpmn`

## Aufgabe

### 1. Modell erweitern

Vor dem Versand der Bestätigungs-Mail kommen ein **Service Task** für die Reservierung und
ein **Exclusive Gateway** dazu, das den Sequenzfluss in zwei Pfade teilt. Insgesamt sind es
vier neue Elemente.

Elemente, Delegate Expressions und die Gateway-Bedingung legst du im **Miragon BPMN Modeler**
an (Element auswählen → Properties Panel), nicht im XML.

**Neue Elemente:**

| Element | Typ | ID | Name | Konfiguration |
|---|---|---|---|---|
| Platz reservieren | Service Task | `serviceTask_claimMembership` | Claim membership | Delegate Expression: `#{claimMembershipDelegate}` |
| Kapazitätsentscheidung | Exclusive Gateway | `gateway_hasEmptySpots` | Has empty spots | Default-Flow: Ja-Pfad |
| Ablehnungs-Mail | Service Task | `serviceTask_sendRejectionMail` | Send rejection mail | Delegate Expression: `#{sendRejectionMailDelegate}` |
| Ablehnung | End Event | `endEvent_membershipRejected` | Membership rejected | – |

**Bedingung am Nein-Pfad:** `${!hasEmptySpots}`. Der Ja-Pfad ist der Default-Flow und
braucht keine Bedingung.

### 2. Use Cases und Services ergänzen

Nach dem Muster aus Aufgabe 4:

- **`ClaimMembershipUseCase` / `ClaimMembershipService`** – prüft die Kapazität und gibt
  `true` zurück, wenn noch ein Platz frei war. Ein einfacher Zähler im Speicher genügt
  (maximal 1000 Plätze); eine Datenbank brauchst du dafür nicht.
- **`SendRejectionMailUseCase` / `SendRejectionMailService`** – lädt die Membership und
  logget die Ablehnung mit der E-Mail-Adresse.

> Die Kapazität ist bewusst schlicht gehalten. Die Referenzlösung nutzt einen
> `AtomicInteger` samt Konstante `MAX_SPOTS` direkt im `ClaimMembershipService`. Wenn du
> es sauberer magst, modelliere stattdessen ein Domain-Objekt `MembershipCapacity` mit
> `maxSpots`, `claimedSpots`, `hasEmptySpots()` und `claim()` – fachlich ist beides gleichwertig.

### 3. Delegates ergänzen

- **`ClaimMembershipDelegate`** – liest `membershipId`, ruft den Use Case auf und schreibt
  dessen Ergebnis als Prozessvariable `hasEmptySpots` auf die `DelegateExecution`.
- **`SendRejectionMailDelegate`** – liest `membershipId` und ruft den Use Case auf.

> Das Setzen der Prozessvariable gehört in den **Delegate**, nicht in den Service: Der
> Service kennt die Engine nicht und gibt nur ein `boolean` zurück. Genau diese Trennung
> prüft der `ArchitectureTest`.

### 4. Business Key setzen

Setze beim Start des Prozesses die `membershipId` als Business Key. Der Correlation Builder im
`MembershipProcessAdapter` (den du in Aufgabe 4 auf `createMessageCorrelation(...)` umgestellt
hast) bietet dafür `processInstanceBusinessKey(...)`. Häng den Aufruf mit der `membershipId` in
die bestehende Kette ein – die konkreten Argumente füllst du selbst:

```java
runtimeService.createMessageCorrelation(/* Message-Name */)
        .processInstanceBusinessKey(/* membershipId */)
        .setVariables(/* ... */)
        .correlateStartMessage();
```

Der Business Key verknüpft die Prozessinstanz mit dem fachlichen Objekt: Im Cockpit lässt
sich jede Instanz eindeutig einer Anmeldung zuordnen und gezielt suchen.

### 5. Task-Formular für die Freigabe

Der User Task `userTask_confirmMembership` hat bisher kein Formular – wer ihn in der Tasklist
öffnet, sieht keine einzige Prozessvariable und kann ihn nur blind abschließen. Gib ihm ein
**generiertes Task-Formular** (*Generated Task Form*, ein Bordmittel von Camunda 7 – keine
zusätzliche Datei, kein HTML), damit die freigebende Person die Anmeldedaten sieht:

| Feld-ID | Label | Typ | Zweck |
|---|---|---|---|
| `name` | Name | string | Kontext, wird aus der Prozessvariable vorbefüllt |
| `email` | E-Mail | string | Kontext, vorbefüllt |
| `age` | Age | long | Kontext, vorbefüllt |
| `confirmed` | Confirm membership | boolean | die eigentliche Freigabe (Checkbox) |

`name`, `email` und `age` tragen dieselben IDs wie die Prozessvariablen und werden dadurch
automatisch vorbefüllt. `confirmed` ist neu und wird beim Abschließen als boolesche
Prozessvariable gespeichert.

> **Hinweis: Warum hier noch eine Generated Form?** Generated Forms kennst du aus Aufgabe 1
> und 2 – als einfachen Einstieg. Für einen internen Freigabeschritt wie diesen reichen sie
> völlig und bleiben deshalb hier. Der eigentliche Prozess wird längst über fachliche
> REST-Endpunkte gesteuert (Start in Aufgabe 4, Ablehnung in Aufgabe 7); eine
> produktionsnahe Freigabeoberfläche wäre ein eigenes Frontend – im Training bleibt es
> bewusst bei der Generated Form.

Im Modeler: User Task auswählen → Properties Panel → Abschnitt **Forms** → Formularfelder
anlegen. Im XML entsteht dabei ein `extensionElements`-Block mit `camunda:formData` direkt
im User Task:

```xml
<bpmn:userTask id="userTask_confirmMembership" name="Confirm membership">
  <bpmn:extensionElements>
    <camunda:formData>
      <camunda:formField id="name" label="Name" type="string" />
      <camunda:formField id="email" label="E-Mail" type="string" />
      <camunda:formField id="age" label="Age" type="long" />
      <camunda:formField id="confirmed" label="Confirm membership" type="boolean" />
    </camunda:formData>
  </bpmn:extensionElements>
</bpmn:userTask>
```

## Randbedingungen

- Der Prozess-Key bleibt `subscribeNewsletter` und der Message-Name `Message_SubscriptionRequested`
  – historische Namen, die stabil bleiben, auch wenn der Prozess fachlich weiterwächst
  (in [Aufgabe 4](exercise-04.md) einmal erwähnt).
- Der Feldtyp muss zum Typ der Prozessvariable passen, sonst greift die Vorbefüllung nicht
  (`age` ist `long`, nicht `string`).
- `confirmed` steuert in dieser Aufgabe noch keinen Prozessfluss – es wird nur erfasst.
- Die Kapazität lebt im Arbeitsspeicher und ist nach einem Neustart wieder bei null. Das
  ist für das Training gewollt.
- Element-IDs und Variablennamen kannst du jederzeit aus dem Referenzmodell übernehmen.

## Erwartetes Ergebnis

Der Prozess hat jetzt zwei Ausgänge – prüfe beide. Zuerst den Weg, den fast alle nehmen:

**Freier Platz vorhanden:**

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "carol@miravelo.com", "name": "Carol", "age": 27}'
```

Der Prozess reserviert einen Platz, nimmt den Ja-Pfad und wartet am User Task
`Confirm membership`. Öffne ihn in der Tasklist (`http://localhost:8080/webapp/#/seven/auth/start`,
admin/admin): Name, E-Mail und Alter sind vorbefüllt. Setze den Haken bei *Confirm
membership* und schließe den Task ab – die Instanz läuft über `Send Welcome Mail` bis
`Membership confirmed`, und `confirmed` steht in der History auf `true`.

**Kein Platz mehr frei:** Setze die maximale Platzzahl vorübergehend auf `0` (in der
Referenzlösung die Konstante `MAX_SPOTS` in `ClaimMembershipService`), starte die Anwendung
neu und schicke:

```bash
curl -X POST http://localhost:8080/api/memberships \
  -H "Content-Type: application/json" \
  -d '{"email": "dave@miravelo.com", "name": "Dave", "age": 30}'
```

Erwartetes Log: `Sending rejection mail to dave@miravelo.com`. Die Instanz endet an
`Membership rejected`, ohne je an einem User Task zu warten.

## Selbstcheck

- [ ] Das Gateway hat einen Default-Flow und genau eine Bedingung (`${!hasEmptySpots}`)
- [ ] Der Ja-Pfad endet an `Membership confirmed`, der Nein-Pfad an `Membership rejected`
- [ ] Im Cockpit trägt die Instanz die `membershipId` als Business Key
- [ ] Das Task-Formular zeigt die vorbefüllten Felder plus die Checkbox `confirmed`

## Referenzlösung

`../../solutions/exercise-05/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=05
```

## Nächster Schritt

Der Prozess hat jetzt zwei Ausgänge – und niemand prüft automatisch, ob er den richtigen
nimmt. In Aufgabe 6 sicherst du ihn mit einem Prozess-Test ab.

➡️ [Weiter zu Aufgabe 6](exercise-06.md)
