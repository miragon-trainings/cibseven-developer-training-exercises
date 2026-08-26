# Aufgaben-Template & Styleguide

Diese Datei ist die verbindliche Vorlage für alle Aufgaben in `docs/`. Wer eine Aufgabe
ergänzt oder überarbeitet, richtet sich danach. Der automatisierte Review-Lauf
(`.claude/skills/aufgaben-review/SKILL.md`) prüft genau gegen dieses Dokument.

---

## 1. Aufbau einer Aufgabe

Jede Aufgabendatei folgt derselben Reihenfolge. Abschnitte in **fett** sind Pflicht,
die übrigen entfallen, wenn sie nichts beitragen.

| # | Abschnitt | Inhalt |
|---|---|---|
| 1 | **`# Aufgabe N – Titel`** | Kurz, aktiv, ohne Doppelpunkt-Kaskaden |
| 2 | **Info-Kasten** (Blockquote direkt unter dem Titel) | `Voraussetzung` · `Arbeitsverzeichnis` · `Neu in dieser Aufgabe` |
| 3 | **`## Darum geht es`** | Ausgangssituation: fachlicher Anlass in 3–6 Sätzen, gerne mit Pointe |
| 4 | **`## Lernziele`** | „Nach dieser Aufgabe kannst du …" – 3–6 Bullets, jeder mit einem Verb |
| 5 | **`## Ziel-Modell`** | Grafik (`../assets/exercise-NN.svg`) und/oder ASCII-Ablauf; bei reinen Code-Aufgaben ein Hinweis, dass kein Modell dazukommt |
| 6 | **`## Aufgabe`** | Nummerierte Arbeitsschritte, jeder Schritt beginnt mit einem Imperativ |
| 7 | **`## Randbedingungen`** | Vorgaben, Konventionen, Annahmen, bewusste Auslassungen |
| 8 | **`## Erwartetes Ergebnis`** | Woran erkennst du Erfolg? Beobachtbares Verhalten + die Befehle, um es zu erzeugen |
| 9 | **`## Selbstcheck`** | Checkliste `- [ ]` mit prüfbaren Akzeptanzkriterien |
| 10 | `## Hinweise` | Nur wenn sie den Lernfortschritt tragen (Stolperfallen, Hintergrund, Ausblick) |
| 11 | **`## Referenzlösung`** | Pfad zur Lösung, ggf. `load-solution`-Befehl |
| 12 | **`## Nächster Schritt`** | Ein Satz Anschluss + Link zur Folgeaufgabe |

**Warum diese Reihenfolge?** Die Vorgabe des Auftrags („Lernziel vor Ausgangssituation")
ist umgestellt: Die Story steht vorne, weil sie in diesem Training den Zugang schafft und
die Lernziele erst dadurch motiviert sind. Das Ziel-Modell folgt als *advance organizer* –
Teilnehmende sehen zuerst, wohin die Reise geht, bevor sie einzelne Schritte abarbeiten.
„Erwartetes Ergebnis" und „Prüfung" sind bewusst getrennt: Ersteres beschreibt das
beobachtbare Verhalten, Letzteres ist die abhakbare Abnahme.

### Info-Kasten – Format

```markdown
> **Voraussetzung:** Aufgabe 3 ist abgeschlossen.
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** Exclusive Gateway, Business Key, generiertes Task-Formular
```

---

## 2. Styleguide (Deutsch)

### Anrede und Ton

- Konsequent **„du"**, nie „Sie", nie „man".
- **„wir"** nur dort, wo das Trainingsteam gemeinsam eine Entscheidung erzählt
  („Wir bauen einen Bestätigungsschritt") – nicht für Arbeitsaufträge.
- Trocken-humorvolle Einleitungen sind erwünscht, aber kurz. Ein Zitat pro Aufgabe reicht.
- Keine Lobhudelei („Super!", „Toll gemacht!") außer am Ende der letzten Aufgabe.

### Arbeitsaufträge

- Jeder Schritt beginnt mit einem **Imperativ**: *Ergänze, Setze, Modelliere, Prüfe,
  Implementiere, Ersetze, Kommentiere … ein*.
- **Kein Schritt besteht nur aus einem Codeblock oder einer Tabelle.** Vor jedem Codeblock,
  jeder Tabelle und jedem ASCII-Diagramm steht mindestens ein Satz, der sagt, *was* passiert
  und *wozu*. Ein Befehl ohne Erklärung wird abgetippt, nicht verstanden. Eine fett gesetzte
  Zwischenüberschrift („**Freier Platz vorhanden:**") ersetzt diesen Satz nicht.
- Dasselbe gilt für die Abschnitte selbst: Auch `## Erwartetes Ergebnis` und Unterabschnitte
  beginnen mit einem einleitenden Satz, nicht direkt mit einem Codeblock.
- Ein Schritt = ein Ziel. Was zwei Ziele hat, wird zu zwei Schritten.
- Keine schwammigen Verben: nicht *„kümmere dich um"*, *„passe an"* ohne Objekt,
  *„sorge dafür, dass"*.
- Vorgaben, die exakt stimmen müssen (IDs, Topics, Keys), stehen in einer **Tabelle**
  oder in Backticks – nie nur im Fließtext.

### Engine-Kontext: selbst implementieren, nicht einkommentieren

Wo das Lernziel die Anbindung an die Process Engine ist, schreibt die teilnehmende Person den
Code **selbst** – sie aktiviert ihn nicht nur. Das betrifft alles, was direkt mit der Engine
spricht: ein JavaDelegate implementieren, mit `DelegateExecution` arbeiten, Prozessvariablen
lesen oder setzen, den `RuntimeService` (oder einen anderen Engine-Service) nutzen, einen
Prozess starten, Nachrichten oder Signale korrelieren, einen External-Task-Worker schreiben.

- **Engine-Kontext** wird als *Write-it-yourself-Stub* ausgeliefert: Imports, Klassenrumpf und
  Methodensignatur dürfen vorgegeben sein, der Methodenkörper ist ein
  `throw new UnsupportedOperationException(...)`, den die teilnehmende Person ersetzt.
- **Nicht-Engine-Plumbing** darf komplett auskommentiert ausgeliefert und per Einkommentieren
  aktiviert werden: REST-Controller, JPA-/DB-Adapter, Spring-/Maven-Konfiguration, Build-Plugins,
  vorbereitende technische Verdrahtung. Das lenkt nicht vom Engine-Lernziel ab.
- **Hinweise** nennen Service und Methode konzeptionell (der Methodenname wie
  `startProcessInstanceByKey` oder `createMessageCorrelation` ist erlaubt), geben aber **keine
  paste-fertige Aufruf- oder Cast-Zeile**. Ein **abstraktes Code-Skelett** – die Aufrufkette mit
  Platzhaltern statt echten Argumenten (`.processInstanceBusinessKey(/* membershipId */)`) – ist
  oft besser als reine Prosa: Es zeigt die Form der API (etwa den Fluent Builder einer
  Korrelation), ohne die Lösung vorwegzunehmen. Exakte Daten (Prozess-Key, Variablennamen,
  Message-, Signal- und Topic-Namen, IDs) gehören weiterhin in Tabellen oder Backticks – das ist
  Datenvorgabe, kein Lösungsweg. Ein vollständig ausgeschriebenes Beispiel mit echten Argumenten
  ist nur dort in Ordnung, wo eine unbekannte API *einmal* vorgeführt und danach selbst
  wiederholt wird (etwa der erste Prozess-Test in Aufgabe 6 oder der erste Worker in Extra-Aufgabe 1).

### Terminologie

Englische Fachbegriffe bleiben englisch, wenn sie im Werkzeug so heißen. Sie werden
**nicht** eingedeutscht und **nicht** dekliniert erfunden.

| Verwenden | Nicht verwenden |
|---|---|
| Service Task, User Task, Start Event, End Event, Boundary Event, Gateway, Call Activity | Serviceaufgabe, Benutzeraufgabe, Grenzereignis |
| Prozessinstanz, Prozessvariable, Prozessmodell, Prozessdefinition | Process Instance, Process Variable |
| Wait State, Job Executor, External Task, Worker | Wartezustand, Auftragsausführer |
| Transaktionsgrenze, Kompensation, Korrelation | Transaction Boundary, Compensation |
| Aufgabe 4 | Exercise 4, Übung 4 |
| Engine, Cockpit, Tasklist | Maschine, Steuerpult |

### Präzision vor Umschreibung

**Wenn ein BPMN- oder Engine-Begriff existiert, wird er benutzt.** Umschreibungen wirken
zugänglich, kosten aber genau das Vokabular, das Teilnehmende danach im Modeler, im Cockpit
und in der Dokumentation wiederfinden müssen. Wer „Weiche" lernt, sucht im Properties Panel
vergeblich.

| Umschreibung | Fachbegriff |
|---|---|
| Weiche, Verzweigung, Entscheidungspunkt | Exclusive Gateway / Parallel Gateway |
| Wartepunkt, Stelle, an der es hängen bleibt | Wait State (User Task, Receive Task, Timer, External Task) |
| Kästchen, Kasten, Box | Element, Aktivität, Task |
| Knoten im Ablauf | Element im Sequenzfluss |
| Aufrufstelle | Call Activity |
| Aufräum-Task, Aufräumpfad | Kompensations-Handler, Rücknahmepfad |
| Fläche, an der etwas hängt | Aktivität, an die das Boundary Event angeheftet ist |
| Strecke, Kette, Strang | Sequenzfluss, Pfad, Zweig |
| „hört auf ein Signal" | wird über ein Signal-Start-Event gestartet |
| „der Prozess wartet / läuft / endet" (wenn ein konkreter Durchlauf gemeint ist) | die **Prozessinstanz** wartet / läuft / endet |

Faustregel für den letzten Punkt: *Prozess* meint die Definition (das Modell), *Instanz*
meint den einzelnen Durchlauf. Im Selbstcheck und in Testbeschreibungen ist fast immer die
Instanz gemeint.

Metaphern sind erlaubt, aber nur **zusätzlich** und nur einmal: erst der Fachbegriff, dann
das Bild – nie das Bild allein.

Weitere Festlegungen:

- **Prozess-Key** (nicht „Prozess-ID"), wenn die Engine gemeint ist (`subscribeNewsletter`).
- **Element-ID**, wenn eine BPMN-ID gemeint ist (`serviceTask_sendWelcomeMail`).
- **Delegate** (männlich, „der Delegate"), **das Modell**, **die Instanz**.
- Erste Nennung eines Begriffs, der nicht vorausgesetzt werden kann, bekommt einen
  Halbsatz Erklärung – danach nie wieder.

### Formulierungen, die nicht ins Training gehören

| Vermeiden | Warum | Stattdessen |
|---|---|---|
| „einfach", „natürlich", „selbstverständlich", „nur noch schnell" | verharmlost und frustriert, wenn es klemmt | weglassen |
| „In diesem Abschnitt werden wir nun …" | Füllsatz | direkt mit dem Auftrag beginnen |
| „Es sollte funktionieren." | nicht prüfbar | konkrete Beobachtung nennen |
| „siehe oben" / „wie bereits erwähnt" | zwingt zum Zurückspringen | Aussage wiederholen oder verlinken |
| Passiv-Ketten („Es muss konfiguriert werden") | verschleiert, wer handelt | „Konfiguriere …" |
| wörtlich übersetzte Wendungen („behauptet in Code", „den Prozess treiben", „Layer") | klingt nach Übersetzung, nicht nach Fachsprache | „steht als Assertion im Test", „die Jobs ausführen", „Schicht" |
| Nominalstil („die Durchführung der Modellierung") | schwerfällig | „modelliere" |
| Denglisch-Verben („den Task completen", „das Modell deployen" im Fließtext) | unsauber | „den Task abschließen", „das Modell deployen" ist als Fachterm ok – aber konsistent |

### Codeblöcke und Pfade

- Alle Befehle laufen, sofern nicht anders angegeben, **im Repository-Wurzelverzeichnis**.
  Wo ein anderes Verzeichnis nötig ist, steht das `cd` mit im Block.
- Maven mit `-pl` immer aus dem Wurzelverzeichnis: `./mvnw -pl services/process-application …`.
- `curl`-Aufrufe gegen JSON-Endpunkte immer mit `-H "Content-Type: application/json"`.
- Relative Pfade in Prosa beziehen sich auf die Datei selbst (`../models/…`), Pfade in
  Befehlen auf das Wurzelverzeichnis (`models/…`). Diese Trennung konsequent einhalten.
- Sprache in Codebeispielen: Bezeichner englisch (wie im Repo), Kommentare deutsch.

### Umfang

- Eine Aufgabe passt auf 2–4 Bildschirmseiten. Wird es länger, gehört Theorie in einen
  Hinweis-Block oder in ein Add-on (siehe `exercise-06-addon.md`).
- Maximal drei Verschachtelungsebenen (`###`).
- Tabellen für Element-Spezifikationen, Fließtext für Begründungen, Codeblöcke für
  alles, was wörtlich übernommen wird.

---

## 3. Konsistenz über das Training hinweg

- **Domäne:** durchgehend *Membership* / Miravelo Inner Circle – ab Aufgabe 0. Der
  Prozess-Key bleibt aus historischen Gründen `subscribeNewsletter`, die BPMN-Datei heißt
  `membership.bpmn`, der Start-Message-Name `Message_SubscriptionRequested`. Diese
  technischen Altnamen werden in Aufgabe 2 einmal erwähnt und danach nicht mehr kommentiert.
- **Personen in Beispielen:** alphabetisch fortlaufend (Alice, Bob, Carol, Dave, Eve,
  Grace, Hanna, Jane) – jede Aufgabe nimmt die nächsten Namen.
- **Element-ID-Konvention:** `startEvent_`, `endEvent_`, `userTask_`, `serviceTask_`,
  `gateway_`, `subProcess_`, `boundaryEvent_`, `callActivity_`, `businessRuleTask_`.
  Timer- und Message-Boundaries im Bestandsmodell heißen abweichend `timer_` bzw.
  `event_` – das ist gewachsen und bleibt so, damit Doku und Modell übereinstimmen.
- **Jede Aufgabe nennt ihr Referenzmodell** unter `../models/exercise-NN/`.
- **Ab Aufgabe 6 (Add-on)** referenziert jeder Test Element-IDs über die generierte
  Process-API, nicht über Strings.

---

## 4. Begriffs-Fahrplan

Jeder Fachbegriff wird **genau einmal eingeführt** – an der Stelle, an der man ihn zum
ersten Mal braucht, mit ein bis zwei Sätzen Erklärung (im Fließtext oder als
`> **Begriff: X.**`-Kasten). Danach wird er vorausgesetzt und höchstens noch verlinkt.

Diese Tabelle hält fest, wo das passiert. Wer eine Aufgabe schreibt, prüft: Ist der Begriff
schon eingeführt? Dann nur verwenden. Ist er neu? Dann hier eintragen und erklären.

**Sonderfall Aufgabe 0:** Dort werden die BPMN-Grundformen aus Kapitel 1 **rein fachlich**
angewendet – auch eingebetteter Subprozess, Boundary Events, Gateways und Kompensation. Ihre
**technische/Ausführungs-Semantik** (Flow-Bedingungen, `async`-Marker, `isForCompensation`,
Korrelation an Boundary Events) wird erst in den unten genannten Aufgaben eingeführt; der
fachliche Vorgriff in Aufgabe 0 gilt nicht als doppelte Einführung.

| Aufgabe | Wird hier eingeführt |
|---|---|
| 0 | fachliche Modellierung; Anwendung aller BPMN-Grundformen aus Kapitel 1 – Start/End Event, User Task, Service Task, Sequenzfluss, eingebetteter Subprozess, Boundary Event, Exclusive/Parallel Gateway, Kompensation (rein fachlich) |
| 1 | Engine, Deployment, Cockpit, Tasklist, Prozessdefinition, **Prozessinstanz**, Prozessvariable, **Start-Formular** (Generated Form am Start Event, vorhanden), **Manual Task** (Durchlauf ohne Code), `act_*`-Tabellen (`re` / `ru` / `hi`) |
| 2 | Prozess-Key, Element-ID, `isExecutable`, `historyTimeToLive`, **User Task**, **Wait State**, **Generated Form selbst erstellen** (am User Task) |
| 3 | Manual Task → **Service Task**, **JavaDelegate**, **Delegate Expression**, hexagonale Architektur (Delegate → Use Case → Service), Prozessvariable im Delegate lesen |
| 4 | Message Start Event, Nachricht, **Korrelation** (`createMessageCorrelation` / `correlateStartMessage`), **`RuntimeService`** (Prozessstart aus Java), **REST-Endpunkt**, **Persistenz** (Repository), Task-Completion per REST (Confirm-Endpunkt), `membershipId` als Prozessreferenz |
| 5 | Exclusive Gateway (Default-Flow, Flow-Bedingung), Business Key, generiertes Task-Formular (Freigabe) |
| 6 | **Transaktionsgrenze**, **asynchrone Continuation** (`asyncBefore` / `asyncAfter`), Commit und Rollback, **Token**, Idempotenz, Prozess-Test, In-Memory-Engine (h2), **Job Executor**, Mock (`@MockitoBean`), Assertion (`BpmnAwareTests`) |
| 6 · Add-on | generierte Process-API, `bpmn-to-code` |
| 7 | eingebetteter Subprozess (technisch), Boundary Event (unterbrechend / nicht unterbrechend), Timer als Duration und als Cycle, Message Boundary Event, Parallel Gateway (Fork / Join) |
| 8 | Kompensation (technisch), Compensation Boundary Event, Kompensations-Handler (`isForCompensation`), Association, Compensating End Event, SAGA-Muster |
| 9 | Call Activity, Called Element, In-/Out-Mapping, DMN, Entscheidungstabelle, Business Rule Task, `mapDecisionResult` |
| 10 | **Signal** und **Broadcast**, Signal-Start-Event, Signal-End-Event, External Task, Worker, `fetchAndLock`, Deployment per REST, OpenAPI-Client |
| Extra 1 | Process-Engine-API, `@ProcessEngineWorker`, Topic, `EngineCommandExecutor`, ArchUnit-Guardrail |

Zwei Regeln dazu:

- **Nicht vorgreifen.** In einem **Arbeitsauftrag**, einem Lernziel oder einem Selbstcheck
  steht kein Begriff, der erst später eingeführt wird. Erlaubt sind Vorgriffe dagegen in
  *Nächster Schritt*, in Hinweis-Kästen, in Ausblicken und in Referenztabellen (etwa der
  ID-Präfix-Konvention in Aufgabe 2) – dort ist erkennbar, dass der Begriff noch kommt. Wer
  vorgreift, markiert es: „(ab Aufgabe 10)".
- **Nicht doppelt einführen.** Taucht ein Begriff später wieder auf, wird auf die
  einführende Aufgabe verlinkt statt neu erklärt.

## 5. Checkliste vor dem Commit einer Aufgabe

- [ ] Alle Pflichtabschnitte vorhanden und in der Reihenfolge aus Abschnitt 1
- [ ] Info-Kasten mit Voraussetzung, Arbeitsverzeichnis, Neuerungen
- [ ] Jeder Arbeitsschritt beginnt mit einem Imperativ
- [ ] Kein Abschnitt und kein Schritt startet direkt mit einem Codeblock, einer Tabelle oder
      einem Diagramm – überall steht mindestens ein erklärender Satz davor
- [ ] Jedes Element wird beim Namen genannt (Service Task, User Task, Exclusive Gateway,
      Wait State …) – keine Umschreibungen aus der Tabelle in Abschnitt 2
- [ ] „Prozess" steht für die Definition, „Prozessinstanz" für den einzelnen Durchlauf
- [ ] Jeder verwendete Fachbegriff ist laut Begriffs-Fahrplan (Abschnitt 4) in dieser oder
      einer **früheren** Aufgabe eingeführt; neue Begriffe sind erklärt und dort eingetragen
- [ ] Alle genannten Element-IDs, Topics, Message- und Signalnamen existieren im
      Referenzmodell (`grep` in `models/exercise-NN/*.bpmn`)
- [ ] Alle genannten Dateipfade existieren (Modul, Lösung, Modell, Template)
- [ ] Alle Befehle sind aus dem angegebenen Verzeichnis lauffähig
- [ ] Der Selbstcheck ist ohne Trainer prüfbar
- [ ] Lösungswege stehen nicht im Aufgabentext (nur Hinweise + Referenzlösung)
- [ ] Engine-Kontext (Delegate, `DelegateExecution`, `RuntimeService`, Prozessvariablen,
      Korrelation, External Task) wird selbst implementiert; Hinweise nennen die API, nicht die
      fertige Aufrufzeile
- [ ] Link auf die Folgeaufgabe gesetzt, Eintrag in der README-Tabelle vorhanden
