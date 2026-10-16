# Aufgabe 0 – Den Mitgliedschaftsablauf fachlich modellieren

> **Voraussetzung:** Kapitel 1 – du kennst BPMN als Notation: Start- und End Events, User Task und Service Task, Sequenzfluss, Exclusive Gateway und Parallel Gateway.
> **Arbeitsverzeichnis:** ein beliebiger Ordner deiner Wahl (noch kein Code, noch kein Modul).
> **Neu in dieser Aufgabe:** BPMN-Modeler, der fachliche Kernablauf der Mitgliedschaft als gemeinsame Landkarte.

## Darum geht es

**Miravelo** ist ein Online-Shop für hochwertige Fahrräder. Für die treuesten Kundinnen und Kunden
gibt es den **Inner Circle**, eine Mitgliedschaft mit begrenzter Platzzahl: Wer beitreten will,
registriert sich, bekommt einen Platz reserviert, bestätigt per Double-Opt-In und wird willkommen
geheißen. Bevor irgendetwas automatisiert wird, hältst du diesen Kernablauf fachlich fest. Ab
Aufgabe 1 automatisierst du ihn Stück für Stück.

> *„Zeichnen wir das doch erst mal auf, bevor jemand anfängt zu programmieren."*
> — der eine vernünftige Satz im ganzen Kickoff.

## Lernziele

Nach dieser Aufgabe kannst du

- einen BPMN-Modeler installieren und darin ein durchgängiges Modell anlegen,
- die Grundformen aus Kapitel 1 auf einen realen Geschäftsprozess anwenden,
- begründen, wo der Ablauf auf einen Menschen wartet und wo eine Bedingung entscheidet,
- Elemente so benennen, dass der Fachbereich den Ablauf ohne Rückfragen vorlesen kann.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-00.svg)

## Aufgabe

### 1. Modeler in der IDE installieren

Installiere den **Miragon BPMN Modeler** als
[VS-Code-Extension](https://marketplace.visualstudio.com/items?itemName=miragon-gmbh.vs-code-bpmn-modeler)
oder als [IntelliJ-Plugin](https://plugins.jetbrains.com/plugin/32634-miragon-bpmn-modeler). Ohne
VS Code oder IntelliJ nutzt du die [Desktop-App](https://miragon.github.io/bpmn-modeler/). Lege ein
neues BPMN-Diagramm an.

### 2. Registrierung und Kapazitätsprüfung modellieren

Modelliere das Rückgrat: Eine Person registriert sich, der Ablauf reserviert einen Platz und prüft,
ob einer frei war. Ist keiner frei, endet die Bewerbung mit einer Absage.

| Typ | Name |
|---|---|
| Start Event | Submit registration form |
| Service Task | Claim membership |
| Exclusive Gateway | Free seat available? |
| Service Task | Send rejection mail |
| End Event | Membership rejected |

Verbinde Start → *Claim membership* → *Free seat available?*. Der Pfad **No** führt zu *Send
rejection mail* → *Membership rejected*. Den Pfad **Yes** füllst du im nächsten Schritt.

### 3. Die Bestätigung modellieren

Modelliere den **Yes**-Pfad: Erst geht eine Bestätigungs-Mail raus, dann bestätigt die Person
selbst.

| Typ | Name |
|---|---|
| Service Task | Send confirmation mail |
| User Task | Confirm membership |

Der User Task wartet auf einen Menschen, der Service Task davor erledigt ein System.

### 4. Die Aktivierung parallel modellieren

Nach der Bestätigung passieren zwei Dinge gleichzeitig: Die Willkommens-Mail geht raus, und die
Community wird informiert. Modelliere das mit einem Parallel Gateway als Fork und einem Parallel
Gateway als Join:

| Typ | Name |
|---|---|
| Parallel Gateway (Fork) | – |
| Service Task | Send Welcome Mail |
| Service Task | Notify community |
| Parallel Gateway (Join) | – |
| End Event | Membership activated |

Führe *Confirm membership* in den Fork, beide Service Tasks parallel in den Join und von dort zu
*Membership activated*.

### 5. Modell sichern

Speichere die Datei als `membership.bpmn` in einem Ordner deiner Wahl.

## Randbedingungen

- **Nur fachlich.** Element-IDs, Formularfelder, Java-Anbindung, `isExecutable` und
  `historyTimeToLive` kommen ab Aufgabe 2.
- **Nur die Grundformen aus Kapitel 1.** Subprozess, Boundary Events und Kompensation kommen
  später.
- **Modellierungsregeln aus Kapitel 1:** Aktivitäten als Verb plus Objekt, Gateways als Frage,
  ausgehende Sequenzflüsse als Antwort (*Yes* / *No*), jedes End Event mit eigenem Namen. Der Happy
  Path verläuft gerade von links nach rechts, die Absage kreuzungsfrei nach unten. Fork und Join
  sind getrennte Gateways.
- **Noch nicht ins Modul kopieren.** Unter `services/process-application/src/main/resources/bpmn/`
  liegt bereits ein rudimentäres `membership.bpmn` für Aufgabe 1.

## Erwartetes Ergebnis

Dein Modell zeigt Registrierung, Platzreservierung, die Kapazitätsprüfung mit Absage, die
Bestätigung am wartenden User Task und die parallele Aktivierung. Der Modeler meldet keine Fehler.

## Selbstcheck

- [ ] Der Ablauf startet mit *Submit registration form* und endet in *Membership activated* oder *Membership rejected*
- [ ] Aktivitäten sind als Verb plus Objekt benannt, das Gateway als Frage, seine Pfade als *Yes* / *No*
- [ ] Die Bestätigung besteht aus *Send confirmation mail* und dem User Task *Confirm membership*
- [ ] Die Aktivierung läuft über einen Parallel-Fork und einen Parallel-Join
- [ ] Der Happy Path verläuft gerade, kein Element ist unverbunden
- [ ] Die Datei liegt als `membership.bpmn` vor

## Hinweise

Die Ausnahmen bekommen später eigene Aufgaben: Fristen, Erinnerung und Rückzug mit Boundary Events
in Aufgabe 5, die Rücknahme eines reservierten Platzes als Kompensation in Aufgabe 6. Pools, Lanes
und Datenobjekte lässt du weg: Der einzige menschliche Schritt ist *Confirm membership*, alles
andere erledigt ein System.

## Referenzlösung

`../../models/exercise-00/membership.bpmn` – öffne das Modell im Modeler und vergleiche es mit
deinem.

## Nächster Schritt

In Aufgabe 1 bringst du die Engine zum Laufen, die solche Modelle ausführt, zunächst mit einem
winzigen Ausschnitt.

➡️ [Weiter zu Aufgabe 1](exercise-01.md)
