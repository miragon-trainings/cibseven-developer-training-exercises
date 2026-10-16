---
name: aufgaben-review
description: Prüft die Trainingsaufgaben in docs/ gegen Template, Styleguide, Referenzmodelle und Referenzlösungen. Nutzen, wenn eine Aufgabe hinzugefügt oder geändert wurde, wenn Modelle/Lösungen angepasst wurden oder wenn jemand einen Review der Aufgabenbeschreibungen anfordert ("Aufgaben reviewen", "docs prüfen", "Aufgabe N gegenlesen").
---

# Review der Trainingsaufgaben

Dieser Skill prüft die deutschen Aufgabenbeschreibungen unter `docs/de/` auf **fachliche Korrektheit**,
**didaktische Nachvollziehbarkeit**, **sprachliche Natürlichkeit** und **strukturelle
Konsistenz**. Er ändert Modelle, Lösungen oder Testcode **nicht** – gefundene Abweichungen
dort werden gemeldet, nicht stillschweigend repariert.

## Geltungsbereich

Standard: alle Dateien `docs/de/exercise-*.md`, `docs/de/extra-task-*.md` und die
Aufgabentabelle in `README.de.md`. Ist eine konkrete Aufgabe genannt, nur diese – plus die
direkt angrenzenden Aufgaben, weil Übergänge mitgeprüft werden.

Referenzdokument für alle Struktur- und Sprachfragen: **`docs/aufgaben-template.md`**.

## Ablauf

### Schritt 1 – Inventarisieren

Lies `docs/aufgaben-template.md`, die betroffenen Aufgabendateien und die README-Tabelle.
Notiere je Aufgabe: Titel, Voraussetzung, Lernziele, neue Elemente, referenzierte Pfade.

### Schritt 2 – Fakten gegen das Repository prüfen

Das ist der wichtigste Teil. Jede Behauptung im Text muss im Repository nachweisbar sein.

| Behauptung im Text | Gegenprüfen mit |
|---|---|
| Element-ID, Message-, Signal- oder Topic-Name | `grep` in `models/exercise-NN/*.bpmn` |
| Prozess-Key, `isExecutable`, `historyTimeToLive` | `grep '<bpmn:process' models/exercise-NN/*.bpmn` |
| `asyncBefore` / `asyncAfter` an einem Element | `grep 'camunda:async' models/exercise-NN/*.bpmn` |
| Timer-Werte (`timeDuration`, `timeCycle`) | `grep -A2 timerEventDefinition models/exercise-NN/*.bpmn` |
| DMN-Decision-ID, Input, Output, Regeln | `models/exercise-NN/*.dmn` |
| Klassen-, Methoden- oder Feldnamen | `find solutions/exercise-NN/src -name '*.java'` |
| Log-Ausgaben („im Log erscheint …") | `grep 'log\.' solutions/exercise-NN/src/main/java/…` |
| Antwortformat eines REST-Endpunkts | Controller in `solutions/exercise-NN/…/adapter/inbound/rest/` |
| Dateipfade, Modulnamen, `TODO`-Marker | direkt im Dateisystem prüfen – im Arbeitsmodul **und** in `templates/process-application-starter/` |
| „Kommentiere den Block … in der `pom.xml` ein" | `grep -n -e '@exercise-' -e 'TODO Exercise' services/process-application/pom.xml` |
| `load-solution`: gültige Werte, was der Task mitbringt | Execution `load-solution` in `services/process-application/pom.xml`; Fehlerklasse 12 |
| Maven-Befehle | Verzeichnis plausibel? `-pl` nur aus dem Wurzelverzeichnis |

Typische Fehlerklassen, auf die gezielt zu achten ist:

1. **Stille Modelländerung** – das Referenzmodell der Aufgabe unterscheidet sich vom
   vorherigen an Stellen, die der Text nicht erwähnt. Prüfe systematisch:
   `diff <(grep -oE '<bpmn:[a-zA-Z]+ id="[^"]+"[^>]*' models/exercise-<N-1>/x.bpmn) <(… exercise-N …)`
2. **Text und Referenzlösung driften auseinander** – der Text verlangt eine Klasse oder ein
   Vorgehen, das die Lösung so nicht umsetzt.
3. **Nicht lauffähige Befehle** – falsches Arbeitsverzeichnis, fehlender
   `-H "Content-Type: application/json"`, `jq` auf einer Nicht-JSON-Antwort.
4. **Tote Verweise** – Pfade auf Module, Dateien oder Verzeichnisse, die es nicht (mehr) gibt.
5. **Schichtverletzung im Aufgabentext** – zum Beispiel ein Application-Service, der laut
   Text die `DelegateExecution` anfassen soll.
6. **Unkommentierter Codeblock** – eine Überschrift, ein Schritt oder ein Abschnitt, der
   direkt mit ```` ``` ````, einer Tabelle oder einem Diagramm beginnt. Teilnehmende tippen
   solche Blöcke ab, ohne zu verstehen, was sie bewirken. Automatisch prüfbar:

   ```bash
   python3 - <<'PY'
   import re, glob

   def is_lead(l):                       # Überschrift oder fette Zwischenzeile
       return bool(re.match(r'^#{2,4} ', l) or re.match(r'^\*\*[^*]+:?\*\*\s*$', l))

   for f in sorted(glob.glob('docs/de/exercise-*.md') + glob.glob('docs/de/extra-*.md')):
       L = open(f).read().split('\n')
       for i, l in enumerate(L):
           if not is_lead(l):
               continue
           j = i + 1                     # nächste nicht-leere Zeile danach
           while j < len(L) and not L[j].strip():
               j += 1
           if j >= len(L) or not (L[j].startswith('```') or L[j].startswith('|')):
               continue
           k = i - 1                     # nächste nicht-leere Zeile davor
           while k >= 0 and not L[k].strip():
               k -= 1
           if k < 0 or is_lead(L[k]):    # davor keine erklärende Prosa
               print(f'{f}:{i+1}  {l.strip()}')
   PY
   ```

   Gemeldet wird nur, wo **weder davor noch danach** ein erklärender Satz steht. Eine fette
   Zwischenzeile über einem Codeblock ist in Ordnung, solange der Absatz darüber den Block
   erklärt. `## Ziel-Modell` mit direkt folgender Grafik ist ebenfalls in Ordnung, sofern
   danach erklärender Text steht.
7. **Umschreibung statt Fachbegriff** – der Text sagt „Weiche" statt Exclusive Gateway,
   „Wartepunkt" statt Wait State, „Aufrufstelle" statt Call Activity. Teilnehmende finden
   diese Wörter danach weder im Modeler noch im Cockpit wieder. Maßgeblich ist die Tabelle
   *Präzision vor Umschreibung* in `docs/aufgaben-template.md`, Abschnitt 2. Grobsuche:

   ```bash
   grep -niE "Weiche|Wartepunkt|Kästchen|Aufrufstelle|Aufräum|Fläche|Knoten|Strecke|Kette|klammer|\bhört auf|Blackbox|Baustein|Zwischenschritt" docs/de/exercise-*.md docs/de/extra-*.md
   ```

   Jeder Treffer wird einzeln bewertet: Steht der Fachbegriff daneben, ist das Bild als
   Ergänzung erlaubt. Steht die Umschreibung allein, ist es ein Befund. `-i` ist wichtig –
   die Wörter tauchen auch als Kompositum-Endung auf („Ablehnungsstrecke").
8. **Wörtlich übersetzte Wendung** – Sätze, die aus dem Englischen gedacht sind: „behauptet
   in Code" (assert), „den Prozess treiben" (drive), „Layer", „feuert Requests". Sie sind
   grammatisch korrekt und trotzdem falsch – so redet keine deutschsprachige Trainerperson.
   Grep hilft nur begrenzt; diese Klasse findet man beim **Lesen**. Lies deshalb von jeder
   geänderten Aufgabe mindestens „Darum geht es" und „Erwartetes Ergebnis" am Stück und frag
   dich bei jedem Satz: Würde ich das so sagen?

   ```bash
   grep -niE "in Code|den Prozess treiben|Layer|feuerst|Requests|macht Sinn|adressier|realisier" docs/de/exercise-*.md docs/de/extra-*.md
   ```
9. **Unerklärter Begriff / Vorgriff** – eine Aufgabe benutzt einen Fachbegriff, der laut
   Begriffs-Fahrplan (`docs/aufgaben-template.md`, Abschnitt 4) erst später oder gar nicht
   eingeführt wird. Das ist der häufigste stille Fehler beim Ergänzen einer Aufgabe: Wer
   den Stoff kennt, merkt nicht mehr, was noch nicht gesagt wurde.

   Vorgehen: Für jeden Begriff aus dem Fahrplan die **erste** Fundstelle in der
   Aufgabenreihenfolge bestimmen und mit dem Fahrplan vergleichen –

   ```bash
   for t in "Wait State" "Prozessinstanz" "Transaktionsgrenze" "Continuation" "Token" \
            "Business Key" "Job Executor" "Boundary Event" "Kompensation" "Call Activity" \
            "External Task" "Broadcast" "Korrelation"; do
     echo "## $t"
     grep -l "$t" docs/de/exercise-0*.md docs/de/extra-task-1.md | sort | head -1
   done
   ```

   Liegt die erste Fundstelle **vor** der Aufgabe aus dem Fahrplan, sieh dir den Kontext an:
   In *Nächster Schritt*, in Hinweisen, Ausblicken und Referenztabellen ist ein Vorgriff
   erlaubt (idealerweise mit „(ab Aufgabe N)" markiert). Steht der Begriff dagegen in einem
   **Arbeitsauftrag, Lernziel oder Selbstcheck**, ist es ein Befund: dort erklären und den
   Fahrplan anpassen – oder die Formulierung umbauen.

   Liegt die Fundstelle richtig, prüfen, ob dort wirklich eine Erklärung steht. Ein Begriff
   im Info-Kasten oder im Lernziel allein reicht **nicht** – die Erklärung gehört an die
   Stelle, an der man den Begriff zum ersten Mal braucht.
10. **Prozess statt Prozessinstanz** – „der Prozess wartet am User Task" meint in Wahrheit
   den einzelnen Durchlauf. Prüfe jeden Treffer von
   `grep -nE "[Dd]er Prozess (wartet|läuft|endet|startet|bricht|nimmt)" docs/de/exercise-*.md docs/de/extra-*.md`:
   Ist ein konkreter Durchlauf gemeint, muss dort **Prozessinstanz** oder **Instanz** stehen.
   Aussagen über das Modell („der Prozess startet ab jetzt per Nachricht") bleiben.
11. **Engine-Kontext nur einkommentiert / Hinweis paste-fertig** – wo das Lernziel die
   Anbindung an die Process Engine ist (Delegate, `DelegateExecution`, `RuntimeService`,
   Prozessvariablen, Korrelation, External Task, Prozessstart), muss die teilnehmende Person
   den Aufruf **selbst schreiben**. Zwei Befunde:
   - Ein **Arbeitsschritt lässt Engine-Code nur einkommentieren**, statt ihn implementieren zu
     lassen („kommentiere den Delegate ein", „entferne die `/* */`", „aktiviere die Klasse"),
     ohne dass danach ein eigener Methodenkörper zu schreiben bleibt. Reines
     Nicht-Engine-Plumbing (REST, JPA, Spring-/Maven-Config, Build-Plugins) darf dagegen
     eingekommentiert werden.
   - Ein **Hinweis liefert die fertige Aufruf- oder Cast-Zeile** zum Abtippen, statt nur die API
     zu benennen. Maßgeblich ist der Unterabschnitt *Engine-Kontext: selbst implementieren* in
     `docs/aufgaben-template.md`, Abschnitt 2. Grobsuche nach paste-fertigen Engine-Aufrufen:

     ```bash
     grep -nE 'runtimeService\.|createMessageCorrelation\(|execution\.(get|set)Variable\(|startProcessInstanceByKey\(|\.correlate|processInstanceBusinessKey\(' docs/de/exercise-*.md docs/de/extra-*.md
     ```

     Jeder Treffer wird einzeln bewertet: Nennt der Text nur die API (Methodenname in Prosa, mit
     `...` oder als abstraktes Skelett mit Platzhaltern wie `.processInstanceBusinessKey(/* membershipId */)`;
     exakte Daten in Backticks), ist das in Ordnung – ein solches Skelett ist sogar erwünscht, weil
     es die Form der API zeigt. Steht dort eine vollständige, zusammengesetzte Aufrufzeile (mit
     echten Argumenten oder Cast), ist es ein Befund – auf ein Skelett oder einen API-Zeiger
     straffen. Ausnahme: ein bewusst *einmal* vorgeführtes Beispiel einer neuen, unbekannten API
     (etwa der erste Prozess-Test in Aufgabe 4, der erste Worker in Extra-Aufgabe 1).
12. **Lösung lässt sich nicht sauber laden** – `load-solution` setzt das Arbeitsmodul auf die
   Starter-Kopie zurück, lässt dabei jede Datei mit `TODO Exercise M` (M ≤ NN) weg, kopiert
   Lösung NN darüber und baut die `pom.xml` aus den `@exercise-N`-Blöcken neu. Das trägt nur,
   solange die Regeln aus `docs/aufgaben-template.md`, Abschnitt 3, eingehalten sind. Drei
   Prüfungen, alle aus dem Wurzelverzeichnis:

   - **Starter-Kopie synchron** – `templates/process-application-starter/` ist byte-gleich zum
     Arbeitsmodul:

     ```bash
     diff -r services/process-application/src templates/process-application-starter/src \
       && diff services/process-application/pom.xml templates/process-application-starter/pom.xml \
       && echo "Starter-Kopie synchron"
     ```

   - **Marker-Regel** – jede Starter-Datei mit `TODO Exercise N` liegt in Lösung N und allen
     späteren Lösungen. Jeder Treffer ist ein Befund, denn die Datei fehlt nach dem Laden:

     ```bash
     starter=templates/process-application-starter/src
     grep -rlE 'TODO Exercise [0-9]+' "$starter"/main "$starter"/test/java/io/miragon/training/process "$starter"/test/resources 2>/dev/null |
     while read -r f; do
       rel=${f#"$starter"/}
       n=$(grep -ohE 'TODO Exercise [0-9]+' "$f" | grep -oE '[0-9]+$' | sort -n | head -1)
       for s in $(seq -f %02g "$n" 7); do
         sol=solutions/exercise-$s; [ "$s" = 07 ] && sol=$sol/process-application
         [ -f "$sol/src/$rel" ] || echo "$rel (TODO Exercise $n) fehlt in $sol"
       done
     done
     ```

   - **pom-Blöcke vollständig** – jede Dependency und jedes Plugin einer Lösungs-`pom.xml` steht
     auch im Arbeitsmodul, aktiv oder als `@exercise-N`-Block. Die Ausgabe muss leer sein:

     ```bash
     for s in 01 02 03 04 05 06 07/process-application; do
       comm -13 <(grep -oE '<artifactId>[^<]+' services/process-application/pom.xml | sort -u) \
                <(grep -oE '<artifactId>[^<]+' solutions/exercise-$s/pom.xml | sort -u) |
         grep -v '>solution' | sed "s|^|exercise-$s: |"
     done
     ```

     Von Hand dazu: Die Block-Nummer ist nicht größer als die der ersten Lösung, die den Eintrag
     braucht; jeder Block hat `:begin` und `:end` mit derselben Kennung und davor einen
     `TODO Exercise N`-Kommentar; `<!--` und `-->` im Block stehen je auf einer eigenen Zeile.

   Wer eine Lösung tatsächlich laden will, tut das nur in einer **Kopie** des Repositorys – der
   Task überschreibt `src/` und `pom.xml` des Arbeitsmoduls:

   ```bash
   rsync -a --exclude target --exclude .git --exclude node_modules ./ /tmp/load-check/
   cd /tmp/load-check && ./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=NN \
     && ./mvnw -pl services/process-application verify
   ```

   Im Aufgabentext ist außerdem ein Befund, was noch das alte Verhalten beschreibt: eine
   Vorbedingung vor dem Laden („erst Aufgabe 1 abschließen"), „ersetzt `src/main` komplett",
   „`src/test` bleibt unberührt", „die `pom.xml` wird nicht mitkopiert" oder gültige Werte ohne `00`.

### Schritt 3 – Vier Review-Perspektiven

Gehe jede Aufgabe aus vier Blickwinkeln durch und halte die Befunde **getrennt**:

1. **Didaktik** – Ist das Lernziel eindeutig und wird es von der Aufgabe wirklich erreicht?
   Passt die Stufe zur Position im Training? Baut sie erkennbar auf der vorherigen Aufgabe
   auf? Ist die kognitive Last angemessen (Faustregel: höchstens ein neues *Konzept* plus
   Routine-Anwendung pro Aufgabe)?
2. **BPMN und Process Engine** – Sind Prozesslogik, BPMN-Semantik und Engine-Begriffe
   korrekt? Stimmen unterbrechend/nicht unterbrechend, Default-Flow, Cycle statt Duration,
   Wait States, Transaktionsgrenzen? Gibt es versteckte Annahmen?
3. **Deutsche Sprache** – Klingt der Text wie von einer erfahrenen deutschsprachigen
   Trainerperson? Prüfe gegen den Styleguide: Anrede, Imperative, Nominalstil, Passiv-Ketten,
   verharmlosende Wörter („einfach", „natürlich"), unnötige Anglizismen, Übersetzungsdeutsch.
   Und: Wird jedes Element beim Fachbegriff genannt, statt umschrieben zu werden? Das ist
   kein Stil-, sondern ein Lernpunkt – die Begriffe sind Teil des Lernziels.
4. **Teilnehmende** – Ist ohne Rückfrage klar, womit man beginnt und wann man fertig ist?
   Welche Frage entstünde beim Bearbeiten? Ist der Selbstcheck ohne Trainer prüfbar?
   Erklärt jeder Schritt, **wozu** er dient – oder gibt es nur einen Befehl zum Abtippen?
   Wo ein Werkzeug gebraucht wird (H2-Konsole, Modeler, Terminal), steht auch, wie man
   es anbindet.

### Schritt 4 – Konsistenz über das ganze Training

- Gleiche Abschnittsstruktur und Überschriften in allen Aufgaben
- Begriffe einheitlich (Glossar in `docs/aufgaben-template.md`, Abschnitt 2)
- Beispielnamen fortlaufend, keine Dopplung
- Voraussetzung einer Aufgabe = Ergebnis der vorherigen, ohne Lücke
- Schwierigkeitsgrad steigt ohne Sprung
- Jede Aufgabe ist in der README-Tabelle verlinkt und umgekehrt

### Schritt 5 – Befunde melden und beheben

Sortiere die Befunde nach Schwere:

| Stufe | Bedeutung |
|---|---|
| **P1** | Die Aufgabe ist so nicht lösbar oder führt in die Irre (falsche ID, fehlender Schritt, nicht lauffähiger Befehl) |
| **P2** | Lösbar, aber irreführend oder unvollständig (fehlende Begründung, unklarer Selbstcheck, Widerspruch zur Lösung) |
| **P3** | Sprache, Stil, Konsistenz |

Behebe P1- und P2-Befunde **in den Aufgabendateien**. Befunde, die außerhalb von `docs/`
liegen (Modell, Lösung, Testcode, Infrastruktur), werden **nur berichtet** – mit
konkretem Vorschlag, aber ohne Änderung.

### Schritt 6 – Gegenprüfen

Prüfe jede geänderte Aufgabe erneut gegen die Checkliste in `docs/aufgaben-template.md`,
Abschnitt 4. Erst wenn alle Haken sitzen, ist die Aufgabe fertig. Danach die nächste.

## Ausgabeformat

```
## Aufgabe N – <Titel>
Status: ✅ ok | ⚠️ überarbeitet | ⛔ blockiert

Fachlich:     …
Didaktisch:   …
Sprachlich:   …
Strukturell:  …

Geändert:     <Datei: was>
Zu klären:    <offene Frage / Annahme>
```

Zum Abschluss eine Gesamtübersicht: Befunde nach Schwere, Änderungen je Datei, offene
Fragen und alle Befunde außerhalb von `docs/`.

## Grenzen

- Keine Änderungen an `models/`, `solutions/`, `services/`, `templates/` oder Tests – auch nicht
  über `load-solution` im Arbeitsbaum.
- Keine erfundenen Quellen und keine behauptete Prüfung nicht abgerufener Dokumente.
- Bei fachlicher Unklarheit: Annahme explizit machen und als Rückfrage ausweisen, statt zu raten.
