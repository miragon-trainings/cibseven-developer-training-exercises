# Aufgabe 1 – Die Engine zum Laufen bringen

> **Voraussetzung:** Aufgabe 0 ist abgeschlossen (der Sollprozess liegt fachlich vor).
> **Arbeitsverzeichnis:** `services/process-application`
> **Neu in dieser Aufgabe:** CIB-Seven-Starter, Engine-Konfiguration, Auto-Deployment, Cockpit, `act_*`-Tabellen, Start-Formular, Manual Task.

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

### 1. Datenbank starten

Die Engine speichert ihren gesamten Zustand in einer relationalen Datenbank. Fahre zuerst den
Container-Stack hoch; er bringt PostgreSQL und MailHog mit. Nutze Docker oder Podman – die
Compose-Datei funktioniert mit beiden:

```bash
cd stack && docker compose up -d
# oder mit Podman:
cd stack && podman compose up -d
```

### 2. Datenbankschema anlegen

Alle Module teilen sich das Schema `exercise`. Lege es einmalig an:

```bash
docker exec -i postgres psql -U admin -d cibseven-training < stack/init-schemas.sql
# oder mit Podman:
podman exec -i postgres psql -U admin -d cibseven-training < stack/init-schemas.sql
```

### 3. Dependencies aktivieren

Öffne `services/process-application/pom.xml` und kommentiere den Block `TODO Exercise 1` ein: die
beiden CIB-Seven-Starter (`webapp-4` und `rest-4`). Erst damit sind Engine, Cockpit-Webapp und
REST-API im Modul. Die Versionen kommen zentral aus der Root-`pom.xml` – trage sie **ohne**
`<version>` ein.

### 4. Konfiguration aktivieren

Kommentiere in `services/process-application/src/main/resources/application.yaml` den Block
`TODO Exercise 1` ein: Datenbank-Anbindung, Cockpit-Admin-User und Webclient. Ohne
Datenbankverbindung startet die Engine nicht.

### 5. Anwendung scharf schalten

Aktiviere in `TrainingApplication.java` die auskommentierten Annotationen
**`@SpringBootApplication`** und **`@EnableJpaRepositories`**. Erst dadurch greifen
Auto-Configuration und das automatische BPMN-Deployment: Alle `*.bpmn` unter `src/main/resources`
werden beim Start in die Engine deployt.

### 6. Anwendung starten

Jetzt kommt alles zusammen. Beobachte das Log – es zeigt, wie die Auto-Configuration die Engine
hochfährt und die BPMN-Datei deployt.

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

### 7. Die Tabellen der Engine ansehen

Beim ersten Start hat die Engine ihr Datenmodell selbst angelegt: mehrere Dutzend Tabellen mit dem
Präfix `act_`. Binde die Datenbank mit einem Werkzeug deiner Wahl an (in IntelliJ über *Database*,
in VS Code über *SQLTools*), Host `localhost`, Port `5432`, Datenbank `cibseven-training`, Benutzer
`admin`, Passwort `admin`. Diese fünf Tabellen sind die wichtigsten:

| Tabelle | Präfix | Inhalt |
|---|---|---|
| `act_re_procdef` | `re` = Repository | deployte **Prozessdefinitionen** (`subscribeNewsletter`) |
| `act_ru_execution` | `ru` = Runtime | **laufende** Prozessinstanzen |
| `act_ru_task` | `ru` | offene **User Tasks** |
| `act_ru_variable` | `ru` | **Prozessvariablen** laufender Instanzen |
| `act_hi_procinst` | `hi` = History | **abgeschlossene** Prozessinstanzen |

Prüfe zum Einstieg, ob dein Modell deployt wurde:

```sql
SELECT key_, name_, version_ FROM exercise.act_re_procdef;
```

### 8. Cockpit erkunden

Das Cockpit ist die Weboberfläche der Engine. Öffne
[http://localhost:8080/webapp/#/seven/auth/start](http://localhost:8080/webapp/#/seven/auth/start)
(admin / admin). Unter **Processes** erscheint `Join Inner Circle` – der Anzeigename des Modells;
der technische Prozess-Key dahinter ist `subscribeNewsletter`. Klick dich durch **Cockpit**,
**Tasklist** und **Admin**.

### 9. Prozess durchspielen

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
- Alle Module laufen auf Port `8080` und im Schema `exercise`. Starte immer nur eines.

## Erwartetes Ergebnis

Im Log erscheinen `Auto-Deploying resources: [... membership.bpmn]` und
`Started TrainingApplication`. Das Cockpit ist erreichbar, `Join Inner Circle` steht unter
**Processes**, und eine über das Start-Formular gestartete Instanz läuft ohne anzuhalten bis zum
End Event durch.

## Selbstcheck

- [ ] PostgreSQL läuft, das Schema `exercise` existiert
- [ ] Dependencies, Konfiguration und `@SpringBootApplication` sind aktiviert
- [ ] Die Anwendung startet und deployt `membership.bpmn`
- [ ] `Join Inner Circle` erscheint im Cockpit unter **Processes**
- [ ] Eine über das Start-Formular gestartete Instanz läuft vollständig durch (History `COMPLETED`)
- [ ] Du kannst erklären, warum die Instanz mit lauter Manual Tasks nirgends wartet

## Hinweise

- Startet die Anwendung mit einem Datenbankfehler, prüfe zuerst Schritt 2: Ohne das Schema
  `exercise` findet die Engine ihre Tabellen nicht.
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
