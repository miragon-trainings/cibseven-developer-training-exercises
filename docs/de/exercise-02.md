# Aufgabe 2 – Den ersten User Task einbauen

> **Voraussetzung:** Aufgabe 1 ist abgeschlossen – die Engine startet und deployt `membership.bpmn`.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** technische Modellierung (Element-ID, Prozess-Key, `isExecutable`, `historyTimeToLive`), User Task, Wait State, Generated Form selbst erstellen.

## Darum geht es

Der Prozess aus Aufgabe 1 läuft von vorne bis hinten durch – auch der Schritt „Confirm". Fachlich
ist das falsch: Die Bestätigung soll auf einen **Menschen** warten, nicht durchrauschen.

Genau dafür gibt es den **User Task**: einen Schritt, an dem die Prozessinstanz stehen bleibt, bis
jemand sie abschließt. In dieser Aufgabe machst du aus dem Platzhalter „Confirm" einen echten User
Task – und gibst ihm ein **Formular**, das du selbst erstellst. Der zweite Platzhalter („Send
welcome mail") bleibt vorerst ein Manual Task.

> *„Ein Task, der nicht wartet, ist kein Task – der ist Durchgangsverkehr."*

## Lernziele

Nach dieser Aufgabe kannst du

- ein fachliches BPMN technisch grundieren (Element-ID, Prozess-Key, `isExecutable`, `historyTimeToLive`),
- einen Manual Task in einen **User Task** umwandeln,
- erklären, was ein **Wait State** ist und ihn in den Runtime-Tabellen (`act_ru_execution`, `act_ru_task`) wiederfinden,
- eine **Generated Form** im Modeler selbst erstellen und mit dem User Task verknüpfen,
- den User Task über die Tasklist abschließen.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-02.svg)

Referenzmodell: `../../models/exercise-02/membership.bpmn`

Gegenüber Aufgabe 1 ändert sich genau ein Element: aus dem Manual Task „Confirm" wird der User
Task `userTask_confirmMembership` mit einem Formular. „Send welcome mail" bleibt ein Manual Task.

## Aufgabe

### 1. Modell technisch grundieren

Öffne `src/main/resources/bpmn/membership.bpmn` im Miragon BPMN Modeler und stelle sicher, dass die
Prozess-Eigenschaften stimmen – ab jetzt sind sie verbindlich:

**Prozess-Eigenschaften:** Prozess-Key `subscribeNewsletter` · `Executable` aktiviert ·
`History Time To Live` = `180`

### 2. „Confirm" in einen User Task umwandeln

Ändere den Typ des Elements von **Manual Task** auf **User Task** und vergib ID und Namen:

| Element | Typ | ID | Name |
|---|---|---|---|
| Bestätigung | User Task | `userTask_confirmMembership` | Confirm membership |

Weil ein User Task ein **Wait State** ist, hält die Instanz hier künftig an, bis jemand den Task
abschließt.

### 3. Die Generated Form selbst erstellen

Der User Task soll der bestätigenden Person die Daten zeigen und die Bestätigung aufnehmen. Wähl
im Modeler den User Task aus und lege im Properties Panel unter **Forms** eine Generated Form mit
diesen Feldern an:

| Feld-ID | Label | Typ |
|---|---|---|
| `email` | E-Mail | string |
| `confirmed` | Confirm membership | boolean |

`email` wird aus der gleichnamigen Prozessvariable (aus dem Start-Formular) vorbefüllt. `confirmed`
ist neu und wird beim Abschließen als Prozessvariable gespeichert.

### 4. Deployen und testen

Starte die Anwendung neu, damit das geänderte Modell deployt wird:

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

Starte über die Tasklist (`Start process` → `Join Inner Circle`) eine Instanz und fülle das
Start-Formular aus. Diesmal läuft sie **nicht** durch: Sie bleibt am User Task `Confirm membership`
stehen.

### 5. Die Laufzeitdaten ansehen

Während CIB Seven läuft und die Instanz am User Task steht, binde die Datenbank an (dieselbe
Verbindung wie in Aufgabe 1: Host `localhost`, Port `5432`, Datenbank `cibseven-training`, Benutzer
/ Passwort `admin`) und führe diese zwei Abfragen gegen die Runtime-Tabellen aus:

```sql
SELECT id_, proc_def_id_ FROM exercise.act_ru_execution;
SELECT id_, name_ FROM exercise.act_ru_task;
```

Was zeigen sie? `act_ru_execution` enthält eine Zeile für deine **noch laufende** Instanz – ihr
`proc_def_id_` verweist auf die deployte Definition `subscribeNewsletter`. `act_ru_task` enthält
eine Zeile für den offenen Task, mit `name_` = `Confirm membership`. Das ist der Wait State,
sichtbar gemacht: In Aufgabe 1 waren beide Tabellen am Ende leer, weil die Instanz durchlief; jetzt
hält sie an, und ihr Zustand bleibt in `act_ru_*` geparkt, bis jemand den Task abschließt. Schließe
ihn über die Tasklist ab und führe beide Abfragen erneut aus – die Zeilen sind weg.

## Randbedingungen

- **Element-ID-Konvention** – ab jetzt verbindlich: `startEvent_`, `endEvent_`, `userTask_`,
  `serviceTask_`, `manualTask_`, `gateway_`, `subProcess_`, `boundaryEvent_`.
- Es wird weiterhin **kein Java** geschrieben: Start und Abschluss laufen über das Cockpit.
- Der Feldtyp muss zum Typ der Prozessvariable passen, sonst greift die Vorbefüllung nicht.

## Erwartetes Ergebnis

Nach dem Start über das Start-Formular wartet die Instanz am User Task – in `act_ru_task` steht eine
Zeile, im Cockpit unter **Tasklist** erscheint `Confirm membership`. Öffne den Task: `email` ist
vorbefüllt. Setze den Haken bei `confirmed` und schließe ab – die Instanz läuft über den (noch
manuellen) „Send welcome mail" bis `Member joined` durch.

> **Begriff: Wait State.** Eine Stelle, an der die Prozessinstanz **stehen bleibt und auf ein
> Ereignis von außen wartet** – hier der User Task, der auf seinen Abschluss wartet. Die Engine
> schreibt den Zustand in die `act_ru_*`-Tabellen und gibt den Thread frei; deshalb übersteht eine
> wartende Instanz einen Neustart. Ein Manual Task ist **kein** Wait State.

## Selbstcheck

- [ ] `userTask_confirmMembership` ist ein User Task (kein Manual Task mehr)
- [ ] Er trägt eine selbst erstellte Generated Form mit `email` und `confirmed`
- [ ] Eine gestartete Instanz wartet am User Task (`act_ru_task` enthält eine Zeile)
- [ ] Die beiden Runtime-Abfragen liefern während des Wartens je eine Zeile, nach dem Abschließen keine mehr
- [ ] Nach dem Abschließen über die Tasklist endet die Instanz an `Member joined`
- [ ] Der Prozess-Key ist `subscribeNewsletter`, `Executable` ist aktiv, `historyTimeToLive` = 180

## Hinweise

Das Start-Formular (aus Aufgabe 1) und die Form am User Task sind beide **Generated Forms** –
derselbe einfache Mechanismus, einmal am Start Event, einmal am User Task. Das ist der bequeme
Einstieg für Formulare. Ab Aufgabe 4 wandert die fachliche Interaktion aus der Tasklist heraus in
eigene REST-Endpunkte.

## Referenzlösung

`../../solutions/exercise-02/` – oder direkt laden:

```bash
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

## Nächster Schritt

In Aufgabe 3 wird aus dem zweiten Platzhalter „Send welcome mail" ein echter **Service Task** –
und du lernst den **JavaDelegate** kennen, der ihn ausführt.

➡️ [Weiter zu Aufgabe 3](exercise-03.md)
