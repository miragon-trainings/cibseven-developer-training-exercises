# Aufgabe 0 – Den Mitgliedschaftsablauf fachlich modellieren

> **Voraussetzung:** Kapitel 1 – du kennst BPMN als Notation: Start- und End Events, User Task und Service Task, Sequenzfluss, Exclusive Gateway und Parallel Gateway.
> **Arbeitsverzeichnis:** ein beliebiger Ordner deiner Wahl (noch kein Code, noch kein Modul).
> **Neu in dieser Aufgabe:** BPMN-Modeler, der fachliche Kernablauf der Mitgliedschaft als gemeinsame Landkarte.

## Darum geht es

**Miravelo** ist ein Online-Shop für hochwertige Fahrräder – Gravel Bikes für lange
Wochenendtouren, Rennräder für alle, die es auf dem Asphalt schnell mögen. Die Kundschaft
ist jung, markenbewusst und ziemlich leidenschaftlich.

Miravelo baut daraus mehr als einen Verteiler: den **Inner Circle**, eine exklusive
Mitgliedschaft mit begrenzter Platzzahl. Wer beitreten will, registriert sich, bekommt einen
Platz reserviert, bestätigt per Double-Opt-In – und wird am Ende willkommen geheißen. Klingt nach
einer Handvoll Schritten, hat aber eine Kapazitätsgrenze und (später) Bestätigungsfristen und
einen Rückzieher, wenn am Ende doch kein Platz frei ist.

> *„Zeichnen wir das doch erst mal auf, bevor jemand anfängt zu programmieren."*
> — der eine vernünftige Satz im ganzen Kickoff.

Bevor irgendetwas automatisiert wird, hältst du den **Kernablauf fachlich** fest: Was passiert,
in welcher Reihenfolge, wo wartet der Ablauf, wo verzweigt er? Das ist die Sprache, in der
Fachbereich und Entwicklung sich einig werden – ohne eine Zeile Technik. Dieses Modell ist der
**Ausgangspunkt für das ganze Training**: Ab Aufgabe 1 automatisierst du es Stück für Stück, und
die Ausnahmen ergänzt du, sobald du die passenden BPMN-Formen kennst.

## Lernziele

Nach dieser Aufgabe kannst du

- einen BPMN-Modeler installieren und darin ein durchgängiges Modell anlegen,
- die Notation aus Kapitel 1 auf einen realen Geschäftsprozess anwenden – Start- und End Events,
  User Task und Service Task, Exclusive Gateway und Parallel Gateway,
- Warte- und Verzweigungspunkte im Ablauf begründen (wo wartet der Ablauf auf einen Menschen, wo
  entscheidet eine Bedingung),
- Elemente so benennen, dass ein Fachbereich den Ablauf ohne Rückfragen vorlesen kann.

## Ziel-Modell

Das ist der Kernablauf des Inner Circle – nur aus Grundformen, die du aus Kapitel 1 kennst.
Genau diesen Ablauf baust du im Lauf des Trainings Schritt für Schritt technisch nach und
erweiterst ihn später um die Ausnahmen.

![BPMN-Modell der Aufgabe](../assets/exercise-00.svg)

## Aufgabe

### 1. Modeler in der IDE installieren

Wir modellieren direkt in der IDE – keine separate Anwendung nötig. Installiere den **Miragon BPMN
Modeler** als [VS-Code-Extension](https://marketplace.visualstudio.com/items?itemName=miragon-gmbh.vs-code-bpmn-modeler)
oder als [IntelliJ-Plugin](https://plugins.jetbrains.com/plugin/32634-miragon-bpmn-modeler). Egal
welche IDE, es ist derselbe Modeler – so öffnet jede folgende Aufgabe das Modell in einem echten
Modeler. Ohne VS Code oder IntelliJ nutzt du die
[eigenständige Desktop-App](https://miragon.github.io/bpmn-modeler/) als Rückfalloption. Lege
danach ein neues BPMN-Diagramm an.

### 2. Registrierung und Kapazitätsprüfung modellieren

Beginne mit dem Rückgrat des Prozesses: Ein Interessent registriert sich, der Ablauf reserviert
einen Platz und prüft dann, ob überhaupt noch einer frei ist. Ist keiner frei, endet die
Bewerbung mit einer Absage.

| Typ | Name |
|---|---|
| Start Event | Submit registration form |
| Service Task | Claim membership |
| Exclusive Gateway | Free seat available? |
| Service Task | Send rejection mail |
| End Event | Membership rejected |

Verbinde Start → *Claim membership* → *Free seat available?*. Vom Gateway führt der Pfad **No** zu
*Send rejection mail* → *Membership rejected*. Der Pfad **Yes** bleibt zunächst offen – ihn füllst
du im nächsten Schritt.

### 3. Die Bestätigung modellieren

Wer einen Platz bekommt, muss die Mitgliedschaft aktiv bestätigen (Double-Opt-In). Modelliere den
**Yes**-Pfad des Gateways aus Schritt 2 als zwei aufeinanderfolgende Schritte: erst geht eine
Bestätigungs-Mail raus, dann bestätigt der Interessent selbst.

| Typ | Name |
|---|---|
| Service Task | Send confirmation mail |
| User Task | Confirm membership |

An diesem *User Task* hält der Ablauf an: Hier wartet er, bis ein Mensch bestätigt. Der *Service
Task* davor läuft von allein – ein System verschickt die Mail.

### 4. Die Aktivierung parallel modellieren

Ist die Mitgliedschaft bestätigt, wird das Mitglied aktiviert – und zwei Dinge passieren
gleichzeitig: die Willkommens-Mail geht raus, und die Community wird informiert. Modelliere das
mit einem **Parallel Gateway** (Fork und Join). Schließe den Fork mit einem Parallel-Join
desselben Typs (AND mit AND), damit beide Zweige wieder synchronisiert werden, bevor es zu
*Membership activated* geht.

| Typ | Name |
|---|---|
| Parallel Gateway (Fork) | – |
| Service Task | Send Welcome Mail |
| Service Task | Notify community |
| Parallel Gateway (Join) | – |
| End Event | Membership activated |

Führe den Ausgang von *Confirm membership* in den Fork, beide Service Tasks parallel, dann in den
Join und zu *Membership activated*.

### 5. Modell sichern

Speichere die Datei als `membership.bpmn` in einem Ordner deiner Wahl. Sie ist dein Referenzbild
für alle Folgeaufgaben.

## Randbedingungen

- **Nur fachlich.** Es geht um Ablauf und Benennung. Element-IDs nach Konvention, Formularfelder,
  die Anbindung von Service Tasks an Java-Code sowie `isExecutable` und `historyTimeToLive` lässt
  du hier bewusst weg – das kommt ab Aufgabe 2.
- **Nur, was du aus Kapitel 1 kennst.** Der Ablauf nutzt ausschließlich Start- und End Events,
  User Task und Service Task, Exclusive Gateway und Parallel Gateway. Fortgeschrittene Formen wie
  Subprozess, Boundary Events oder Kompensation kommen erst in späteren Aufgaben dazu – lass sie
  hier weg.
- **Sauber modellieren, nicht nur vollständig.** Wende die Modellierungsregeln aus Kapitel 1 an:
  Benenne jede Aktivität als Verb plus Objekt (*Claim membership*), jedes Gateway als Frage und
  jeden ausgehenden Sequenzfluss als Antwort (*Yes* / *No*). Rahme den Ablauf mit Start- und End
  Events und gib jedem End Event einen eigenen Namen. Halte den Happy Path – Registrierung,
  Bestätigung, Aktivierung – auf einer geraden Linie von links nach rechts und führe die Absage
  kreuzungsfrei nach unten weg. Verzweige nur an Gateways: Fork und Join sind getrennte Rauten.
- **Das ist der Ausgangspunkt, nicht das Endbild.** Die Ausnahmen – Bestätigungsfristen, eine
  tägliche Erinnerung, eine aktive Ablehnung und die Rücknahme eines reservierten Platzes – kommen
  in späteren Aufgaben dazu.
- **Noch nicht ins Modul kopieren.** Unter `services/process-application/src/main/resources/bpmn/`
  liegt bereits ein bewusst rudimentäres `membership.bpmn`, das du in Aufgabe 1 brauchst.
  Überschreibe es jetzt nicht.
- Namen in den Modellen sind durchgehend englisch; die Aufgabenbeschreibungen gibt es auf
  Deutsch und Englisch.

## Erwartetes Ergebnis

Dein Modell bildet den Kernablauf ab: Registrierung, Platzreservierung, die Kapazitätsprüfung am
Exclusive Gateway mit der Absage, die Bestätigung mit dem wartenden User Task und die parallele
Aktivierung. Der Modeler meldet keine Fehler, und jemand aus dem Fachbereich könnte den Ablauf
vorlesen, ohne nachzufragen, was ein einzelnes Element bedeutet.

## Selbstcheck

- [ ] Der Ablauf ist mit Start- und End Events gerahmt: er startet mit *Submit registration form*
      und endet in genau einem benannten End Event pro Ausgang – *Membership activated* oder
      *Membership rejected*
- [ ] Jede Aktivität ist als Verb plus Objekt benannt, das Exclusive Gateway als Frage
      (*Free seat available?*) und seine ausgehenden Sequenzflüsse als Antwort (*Yes* / *No*)
- [ ] Die Kapazität wird über ein Exclusive Gateway mit einem **No**-Pfad zur Absage geprüft
- [ ] Die Bestätigung besteht aus dem Service Task *Send confirmation mail* und dem User Task
      *Confirm membership*, an dem der Ablauf auf einen Menschen wartet
- [ ] Die Aktivierung läuft über ein Parallel Gateway; der Fork wird mit einem Parallel-Join
      desselben Typs geschlossen, der auf beide Zweige wartet (Willkommens-Mail und Community-Info
      laufen gleichzeitig)
- [ ] Der Happy Path verläuft gerade von links nach rechts; der Absage-Pfad zweigt kreuzungsfrei
      ab, und kein Gateway führt Zweige zusammen und verzweigt zugleich (Fork und Join sind
      getrennte Rauten)
- [ ] Du bist beide Pfade einmal von Hand durchgegangen: Happy Path und Absage enden jeweils in
      genau einem benannten End Event; kein Zweig läuft ins Leere
- [ ] Du kannst in je einem Satz sagen, wo der Ablauf auf einen Menschen wartet (am User Task) und
      warum das Exclusive Gateway verzweigt
- [ ] Alle Elemente sind über Sequenzflüsse verbunden – kein loses Element
- [ ] Die Datei liegt als `membership.bpmn` gespeichert vor

## Hinweise

Der Ablauf wächst im Lauf des Trainings. Den Kernablauf setzt du Schritt für Schritt technisch um –
den Bestätigungsschritt in Aufgabe 4, das Kapazitäts-Gateway mit Bedingung in Aufgabe 5. Die
Ausnahmen bekommen später ihre **eigene Aufgabe**, in der du sie fachlich und technisch ergänzt:
die Fristen, die Erinnerung und die aktive Ablehnung (mit Boundary Events) in Aufgabe 7 und die
Rücknahme eines reservierten Platzes als Kompensation in Aufgabe 8. Hier modellierst du zuerst den
Kernablauf, damit du bei jedem Teilschritt weißt, wohin er gehört.

Warum ein User Task und ein Service Task? Der **User Task** wartet auf einen Menschen – jemand
bestätigt die Mitgliedschaft. Der **Service Task** wird von einem System erledigt – der
Mailversand, die Platzreservierung. Diese Unterscheidung legt fest, wo der Ablauf wartet und wo er
von allein weiterläuft. Die drei Mails sind übrigens **Service Tasks**, keine Send Tasks – ein
System erledigt sie (ab Aufgabe 3 setzt du sie als JavaDelegate um). Manual Task, Business Rule
Task, Script Task und die übrigen Task-Typen aus Kapitel 1 tauchen erst später auf oder bleiben
ganz außen vor.

**Warum keine Pools, Lanes oder Datenobjekte?** Der Ablauf ist aus Sicht von Miravelo modelliert.
Der einzige menschliche Berührungspunkt ist der User Task *Confirm membership*, an dem der
Interessent bestätigt; alle übrigen Schritte sind Service Tasks. Nach der Regel „Lanes mit
Disziplin" bekommt ein System keine eigene Lane. Pools und Lanes sind reine Dokumentation; hier
würden sie nichts erklären, sondern die Landkarte nur mit Rauschen füllen. Datenobjekte sind rein
beschreibend, die Engine wertet sie nicht aus; deshalb lassen wir sie hier bewusst weg und
konzentrieren uns auf Ablauf und Benennung.

## Referenzlösung

`../../models/exercise-00/membership.bpmn` – öffne das Modell im Modeler und vergleiche es mit
deinem.

## Nächster Schritt

In Aufgabe 1 bringst du die Engine zum Laufen, die genau solche Modelle ausführt – zunächst mit
einem bewusst winzigen Ausschnitt.

➡️ [Weiter zu Aufgabe 1](exercise-01.md)
