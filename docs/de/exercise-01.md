# Aufgabe 1 – Die Engine zum Laufen bringen

> **Voraussetzung:** Aufgabe 0 ist abgeschlossen (der Sollprozess liegt fachlich vor).
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** CIB-Seven-Starter, Engine-Konfiguration, Embedded-Datenbank (H2), Auto-Deployment, Cockpit, H2-Konsole, `act_*`-Tabellen, Start-Formular, Manual Task.

## Darum geht es

In Aufgabe 0 hast du den **kompletten Sollprozess** des Inner Circle modelliert. Bevor jemand den
ganzen Ablauf automatisiert, hat ein externer Consultant einen **bewusst winzigen ersten Ausschnitt**
grob aufgesetzt: ein Start-Formular für die Anmeldedaten, danach zwei **Manual Tasks** –
„Confirm" und „Send welcome mail". Manual Tasks sind Platzhalter: Die Engine läuft einfach durch
sie hindurch. So bekommst du eine Prozessinstanz, die von vorne bis hinten durchläuft – ohne eine
Zeile Code.

Diese Mini-Fassung ist **nicht** der Sollprozess und **nicht** das Zielmodell. Sie existiert nur,
um die Engine überhaupt einmal zu starten und ein Gefühl für Deployment, Ausführung und den
Datenbestand zu bekommen. Ab Aufgabe 2 baust du die Platzhalter Stück für Stück zu echten
Schritten aus.

Was jetzt fehlt, ist die **Laufzeitumgebung**: ein Spring-Boot-Modul, in dem die CIB-Seven-Engine
läuft. Genau das richtest du jetzt ein.

## Lernziele

Nach dieser Aufgabe kannst du

- ein Spring-Boot-Modul mit eingebetteter CIB-Seven-Engine lauffähig machen,
- benennen, was die Engine zum Starten braucht (Datenbank, Auto-Configuration, Auto-Deployment),
- dich im Cockpit zwischen Processes, Tasklist und Admin bewegen,
- die `act_*`-Tabellen den Kategorien Repository, Runtime und History zuordnen,
- eine Instanz über ein **Start-Formular** in der Tasklist starten,
- erklären, warum eine Instanz mit lauter **Manual Tasks** ohne Anhalten durchläuft.

## Ziel-Modell

![BPMN-Modell der Aufgabe](../assets/exercise-01.svg)

Das ist der bewusst reduzierte Ausschnitt: Start-Formular → Manual Task „Confirm" → Manual Task
„Send welcome mail" → Ende. Er liegt fertig unter
`services/process-application/src/main/resources/bpmn/membership.bpmn`. Du modellierst in dieser
Aufgabe nichts – du bringst ihn zum Laufen.

## Aufgabe

> Es geht ums **Einrichten und Kennenlernen** – kein Business-Code.

### 1. Dependencies aktivieren

Öffne `services/process-application/pom.xml` und kommentiere den Block `TODO Exercise 1` ein: die
beiden CIB-Seven-Starter (`webapp-4` und `rest-4`). Erst damit sind Engine, Cockpit-Webapp und
REST-API im Modul. Die Versionen kommen zentral aus der Root-`pom.xml` – trage sie **ohne**
`<version>` ein.

### 2. Konfiguration aktivieren

Die Engine speichert ihren gesamten Zustand in einer relationalen Datenbank. Im Training ist das
eine **H2-Datenbank**, die direkt in der Anwendung mitläuft und ihre Daten in einer Datei unter
`~/.cibseven-training/` ablegt. Du musst nichts installieren und nichts separat starten: H2 legt
die Datei beim ersten Start an, die Engine erzeugt darin ihre `act_*`-Tabellen selbst.

Kommentiere in `services/process-application/src/main/resources/application.yaml` den Block
`TODO Exercise 1` ein. Er enthält:

- die Datenbank-Anbindung an die H2-Datei (`jdbc:h2:file:~/.cibseven-training/exercise`, Benutzer
  `sa`, leeres Passwort),
- die **H2-Konsole** – eine SQL-Oberfläche im Browser, die Teil der Anwendung ist; du brauchst sie
  in Schritt 5,
- den Cockpit-Admin-User,
- den Webclient, unter anderem mit dem JWT-Secret für den Cockpit-Login.

Ohne diesen Block startet die Anwendung nicht. Spring Boot weicht zwar von sich aus auf eine
flüchtige In-Memory-H2 aus, doch der Start scheitert am Cockpit, dem das JWT-Secret fehlt
(`Secret must be at least 155 characters long and a base64 decodable string`).

> **Begriff: Embedded-Datenbank.** Eine Datenbank, die als Bibliothek **im selben Prozess** wie die
> Anwendung läuft statt als eigener Server. Sie startet und stoppt mit der Anwendung; weil H2 hier
> in eine Datei schreibt, übersteht der Datenbestand trotzdem einen Neustart.

### 3. Anwendung scharf schalten

Aktiviere in `TrainingApplication.java` die auskommentierten Annotationen
**`@SpringBootApplication`** und **`@EnableJpaRepositories`**. Erst dadurch greifen
Auto-Configuration und das automatische BPMN-Deployment: Alle `*.bpmn` unter `src/main/resources`
werden beim Start in die Engine deployt.

### 4. Anwendung starten

Jetzt kommt alles zusammen. Beobachte das Log – es zeigt, wie die Auto-Configuration die Engine
hochfährt und die BPMN-Datei deployt.

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

### 5. Die Tabellen der Engine ansehen

Beim ersten Start hat die Engine ihr Datenmodell selbst angelegt: mehrere Dutzend Tabellen mit dem
Präfix `act_`. Sieh sie dir in der H2-Konsole an, die in der laufenden Anwendung steckt:

1. Öffne [http://localhost:8080/h2-console](http://localhost:8080/h2-console).
2. Ersetze im Feld **JDBC URL** den vorbelegten Wert `jdbc:h2:~/test` durch
   `jdbc:h2:file:~/.cibseven-training/exercise` – sonst sucht die Konsole eine Datenbank, die es
   nicht gibt.
3. Lass den Benutzer auf `sa`, das Passwort leer und klicke **Connect**.

In der linken Spalte listet die Konsole jetzt die Tabellen auf (H2 schreibt ihre Namen groß, in SQL
ist die Schreibweise egal). Diese fünf Tabellen sind die wichtigsten:

| Tabelle | Präfix | Inhalt |
|---|---|---|
| `act_re_procdef` | `re` = Repository | deployte **Prozessdefinitionen** (`subscribeNewsletter`) |
| `act_ru_execution` | `ru` = Runtime | **laufende** Prozessinstanzen |
| `act_ru_task` | `ru` | offene **User Tasks** |
| `act_ru_variable` | `ru` | **Prozessvariablen** laufender Instanzen |
| `act_hi_procinst` | `hi` = History | **abgeschlossene** Prozessinstanzen |

Prüfe zum Einstieg, ob dein Modell deployt wurde. Tippe die Abfrage ins Eingabefeld der Konsole und
klicke **Run**:

```sql
SELECT key_, name_, version_ FROM act_re_procdef;
```

### 6. Cockpit erkunden

Das Cockpit ist die Weboberfläche der Engine. Öffne
[http://localhost:8080/webapp/#/seven/auth/start](http://localhost:8080/webapp/#/seven/auth/start)
(admin / admin). Unter **Processes** erscheint `Join Inner Circle` – der Anzeigename des Modells;
der technische Prozess-Key dahinter ist `subscribeNewsletter`. Klick dich durch **Cockpit**,
**Tasklist** und **Admin**.

### 7. Prozess durchspielen

Der Prozess ist deployt, aber noch nie gelaufen. Starte ihn über das Start-Formular:

1. **Tasklist** → **Start process** → `Join Inner Circle`
2. Es erscheint ein **Start-Formular** mit den Feldern `email`, `name`, `age`. Fülle es aus und
   starte.
3. Die Instanz läuft **ohne anzuhalten** bis zum Ende durch – beide Manual Tasks werden von der
   Engine einfach durchlaufen.
4. In `act_hi_procinst` steht die Instanz auf `COMPLETED`; `act_ru_*` ist wieder leer.

> **Begriff: Start-Formular (Generated Form).** Die Felder `email`/`name`/`age` stehen als
> `camunda:formData` direkt am Start Event. Die Tasklist rendert daraus beim Starten automatisch
> ein Formular – keine zusätzliche Datei, kein HTML. Die eingegebenen Werte werden zu
> Prozessvariablen der Instanz.

> **Begriff: Manual Task.** Ein Task, den die Engine **nicht** ausführt und an dem sie **nicht**
> wartet – sie läuft einfach hindurch. Ein Platzhalter für „hier passiert später etwas". Deshalb
> durchläuft die Instanz das Modell auf einen Schlag. Ab Aufgabe 2 wird der erste Platzhalter zu
> einem echten User Task.

## Randbedingungen

- Du arbeitest ausschließlich im Modul `services/process-application`.
- Das Modul enthält bereits das vollständige Skelett der hexagonalen Architektur. Die
  Business-Schicht ist auskommentiert und wird hier noch nicht gebraucht.
- Alle Module laufen auf Port `8080` und nutzen dieselbe H2-Datei unter `~/.cibseven-training/`.
  Starte immer nur eines – die laufende Anwendung sperrt die Datei.

## Erwartetes Ergebnis

Im Log erscheinen `Auto-Deploying resources: [... membership.bpmn]` und
`Started TrainingApplication`. Das Cockpit ist erreichbar, `Join Inner Circle` steht unter
**Processes**, und eine über das Start-Formular gestartete Instanz läuft ohne anzuhalten bis zum
End Event durch.

## Selbstcheck

- [ ] Dependencies, Konfiguration und `@SpringBootApplication` sind aktiviert
- [ ] Die Anwendung startet und deployt `membership.bpmn`
- [ ] Die H2-Konsole zeigt die `act_*`-Tabellen, und `act_re_procdef` enthält `subscribeNewsletter`
- [ ] `Join Inner Circle` erscheint im Cockpit unter **Processes**
- [ ] Eine über das Start-Formular gestartete Instanz läuft vollständig durch (History `COMPLETED`)
- [ ] Du kannst erklären, warum die Instanz mit lauter Manual Tasks nirgends wartet

## Hinweise

- Bricht der Start mit
  `Database may be already in use: "…/.cibseven-training/exercise.mv.db"` ab, läuft bereits eine
  andere Anwendung auf derselben Datei – ein anderes Modul oder dasselbe in einem zweiten Terminal.
  Beende sie und starte neu. Aus demselben Grund öffnen externe Datenbank-Werkzeuge (etwa
  *Database* in IntelliJ) die Datei nicht, solange die Anwendung läuft – nimm die H2-Konsole.
- Meldet die H2-Konsole
  `Database "…/test" not found, either pre-create it or allow remote database creation …`, steht
  im Feld **JDBC URL** noch der vorbelegte Wert. Ersetze ihn wie in Schritt 5 durch
  `jdbc:h2:file:~/.cibseven-training/exercise`.
- Der Datenbestand übersteht einen Neustart der Anwendung. Für einen sauberen Neuanfang beende die
  Anwendung und lösche den Ordner `~/.cibseven-training` (unter Windows
  `%USERPROFILE%\.cibseven-training`); beim nächsten Start legen H2 und die Engine alles neu an.
- Die Präfixe sind ein Merkanker: `re` liegt fest, `ru` bewegt sich, `hi` ist Vergangenheit.

## Referenzlösung

- Fertiges Modul: `../../solutions/exercise-01/`
- Modell: `../../models/exercise-01/membership.bpmn`
- Direkt ins Arbeitsmodul laden:

  ```bash
  ./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=01
  ```

## Nächster Schritt

In Aufgabe 2 wird aus dem Platzhalter „Confirm" ein echter **User Task**, den du selbst mit
einem Formular modellierst.

➡️ [Weiter zu Aufgabe 2](exercise-02.md)
