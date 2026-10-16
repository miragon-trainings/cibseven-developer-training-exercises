# CIB Seven Developer Training Exercises

> 🇩🇪 **Deutsch** · [🇬🇧 English](README.md)

Praxisübungen für das CIB Seven Developer Training. Das Projekt implementiert einen Inner-Circle-Membership-Prozess mit CIB Seven als Process Engine und einer hexagonalen Architektur, die Business-Logik von Infrastruktur entkoppelt.

## Übungen

### Hintergrund: Miravelo

**Miravelo** ist ein Online-Shop für hochwertige Fahrräder — Gravel Bikes für lange
Wochenendtouren, Rennräder für alle, die es auf dem Asphalt schnell mögen. Die Kundschaft
ist jung, markenbewusst und ziemlich leidenschaftlich.

Der Shop wächst, neue Produkte kommen laufend dazu. Das Team beschließt:
Wir bauen einen **Newsletter**, damit Kundinnen und Kunden über Produkt-Launches und
exklusive Angebote informiert bleiben. Jemand trägt sich ein, bekommt eine
Willkommens-Mail – fertig.

> *„Das ist doch in einer Stunde gebaut."*
> — Jeder Entwickler, der einen Newsletter unterschätzt hat.

Das Training findet im Kontext des exklusiven **Miravelo Inner Circle** statt — einer auf
tausend Plätze limitierten Membership für die treuesten Kundinnen und Kunden. Du modellierst
zuerst den Kernablauf fachlich, automatisierst ihn dann Schritt für Schritt und ergänzt unterwegs
die Ausnahmen.

Was folgt, ist eine Reise durch immer komplexere BPMN-Muster: Gateways, Boundary Events,
Subprozesse, Parallel Gateways, Call Activities, DMN-Entscheidungstabellen und Kompensation —
jede Aufgabe baut auf der vorherigen auf.

![Prozessmodell](docs/newsletter-subscription.png)

### Aufgabenübersicht

Detaillierte Aufgabenbeschreibungen befinden sich in [`docs/`](docs/).

| Aufgabe | Thema | Beschreibung |
|---|---|---|
| [0](docs/de/exercise-00.md) | Fachliche BPMN-Modellierung | Den Kernablauf der Inner-Circle-Membership rein fachlich und nur mit den Grundformen aus Kapitel 1 modellieren – der Ausgangspunkt für das ganze Training |
| [1](docs/de/exercise-01.md) | Engine zum Laufen bringen | Das vorgegebene Start-Formular-/Manual-Task-Modell durchlaufen lassen, Cockpit und `act_*`-Tabellen der Engine kennenlernen |
| [2](docs/de/exercise-02.md) | User Task und Service Task | „Confirm" zum User Task mit selbst erstellter Generated Form machen, „Send welcome mail" zum Service Task mit JavaDelegate (Start über Cockpit) |
| [3](docs/de/exercise-03.md) | Die Anwendung übernimmt | Message Start Event, Business Key, REST-Endpunkte für Register + Confirm, Korrelation, Persistenz, Kapazitätsprüfung mit Exclusive Gateway |
| [4](docs/de/exercise-04.md) | Prozess-Tests | Transaktionsgrenzen, Prozess-Unit-Test mit In-Memory-Engine, gemockten Use Cases, ohne laufende Anwendung |
| [4 · Add-on](docs/de/exercise-04-addon.md) | bpmn-to-code | Element-IDs als generierte Konstanten statt handgetippter Strings, Pfade compile-sicher mit `PathWalk` navigieren |
| [5](docs/de/exercise-05.md) | Subprozess, Boundary Events & Parallelität | Subprozess, Timer- und Message-Boundary-Events, Parallel Gateway, Teams-Anbindung |
| [6](docs/de/exercise-06.md) | Kompensation, Call Activity & DMN | Compensation Boundary Event, Compensating End Event, Call Activity, DMN-Entscheidungstabelle, Business Rule Task |
| [7](docs/de/exercise-07.md) | Remote Engine als geteilte Infrastruktur | Eine Abteilung besitzt einen **eigenen** kleinen Prozess (`sendWelcomeKit`) in ihrem Remote-Service: Modell, Worker, Deployment und Tests liegen dort; getriggert per Signal-Broadcast; die Engine über einen generierten OpenAPI-Client getrieben |
| [Extra 1](docs/de/extra-task-1.md) | Process-Engine-API | Aufgabe 7 engine-neutral umbauen: Worker statt JavaDelegate, Adapter-Tausch statt Engine-Lock-in |

> Aufbau, Sprache und Qualitätskriterien der Aufgaben sind in
> [`docs/aufgaben-template.md`](docs/aufgaben-template.md) festgehalten. Neue oder geänderte
> Aufgaben werden mit dem Skill `aufgaben-review` gegengeprüft
> (`.claude/skills/aufgaben-review/SKILL.md`).

## Quick Start

Die Engine nutzt eine eingebettete H2-Datenbank, die als Datei unter `~/.cibseven-training` liegt –
du musst keine Datenbank installieren oder starten. Der Datenbestand übersteht einen Neustart; für
einen sauberen Neuanfang beende die Anwendung und lösche den Ordner `~/.cibseven-training`
(unter Windows `%USERPROFILE%\.cibseven-training`).

```bash
# Alles bauen
./mvnw clean install

# process-application-Modul starten (das eine Modul, in dem du alle Aufgaben bearbeitest)
cd services/process-application && ../../mvnw spring-boot:run

# CIB Seven Cockpit
open http://localhost:8080/webapp/#/seven/auth/start    # admin / admin

# H2-Konsole (Engine-Tabellen) – im Login die vorbelegte JDBC-URL jdbc:h2:~/test ersetzen
open http://localhost:8080/h2-console    # JDBC-URL jdbc:h2:file:~/.cibseven-training/exercise, Benutzer sa, kein Passwort
```

### Lösung einer Aufgabe laden

Das `process-application`-Modul startet im Zustand von **Aufgabe 1** (Engine noch auskommentiert). Wenn du
eine Aufgabe nicht ganz fertig bekommst, kannst du die Referenzlösung in dein `process-application`-Modul
laden und mit ihr weiterarbeiten:

```bash
# solutions/exercise-02 in das process-application-Modul laden (gültige Werte: 00–07, zweistellig; 00 = zurück zum Auslieferungszustand)
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

Der Task setzt `src/` und `pom.xml` auf den Auslieferungszustand zurück und kopiert die Lösung darüber:
`src/main` (Java, `application.yaml`, BPMN/DMN), die Prozess-Tests und die passende `pom.xml` (etwa mit
den CIB-Seven-Startern aus Aufgabe 1). Die Stubs der späteren Aufgaben bleiben stehen, der
`ArchitectureTest` bleibt, wie er ist. Deine eigenen Änderungen in `src/` und `pom.xml` werden ersetzt –
der Task sichert sie vorher unter `services/process-application/.load-solution-backup/<Zeitstempel>/`.
Weil sich die `pom.xml` ändert, lass Maven danach erneut laufen (IntelliJ: Maven-Projekt neu laden).
Bist du zu einer früheren Aufgabe zurückgesprungen, beende die Anwendung und lösche den Ordner
`~/.cibseven-training` – sonst bleiben Deployments und Prozessinstanzen der späteren Aufgabe in der Datenbank.

Alle Module – das `process-application`-Modul **und** alle Solutions – laufen auf demselben Port
(`8080`) und nutzen dieselbe eingebettete H2-Datenbankdatei; es läuft also immer nur **ein** Modul zur Zeit
(die laufende Anwendung sperrt die Datei, eine zweite Instanz bricht beim Start ab).

## Repository-Struktur

```
cibseven-developer-training-exercises/
├── docs/                             # Aufgabenbeschreibungen: docs/de/ (Deutsch) + docs/en/ (Englisch) + Assets
├── services/                         # Die Services, an denen du arbeitest
│   ├── process-application/          # Prozess-Anwendung (startet im Zustand von Aufgabe 1)
│   │   └── src/main/java/io/miragon/training/
│   │       ├── adapter/
│   │       │   ├── inbound/
│   │       │   │   ├── cibseven/     # JavaDelegate-Implementierungen
│   │       │   │   └── rest/         # REST-Controller
│   │       │   └── outbound/
│   │       │       ├── cibseven/     # Process-Engine-Adapter (Start/Korrelation)
│   │       │       └── db/           # JPA-Persistence-Adapter
│   │       ├── application/
│   │       │   ├── port/
│   │       │   │   ├── inbound/      # Use-Case-Interfaces
│   │       │   │   └── outbound/     # Repository- und Prozess-Port-Interfaces
│   │       │   └── service/          # Use-Case-Implementierungen
│   │       └── domain/               # Domain-Modell (reines Java, keine Framework-Abhängigkeiten)
│   └── (logistics-service/)          # erst in Aufgabe 7 aus templates/ angelegt (Remote-Owner)
├── templates/
│   ├── exercise-07/logistics-service/ # Vorlage für den Aufgabe-7-Worker (in services/ kopieren)
│   └── process-application-starter/  # Auslieferungszustand von process-application (Basis für load-solution)
├── solutions/                        # Kumulative Lösungen pro Aufgabe (exercise-01 … exercise-07, extra-task-1)
│   ├── exercise-{01-07}/             # exercise-07/ ist verschachtelt: process-application/ + logistics-service/
│   └── extra-task-1/
├── models/                           # Referenz-BPMN-/DMN-Modelle
└── pom.xml
```

## Technologie-Stack

| Komponente | Technologie |
|---|---|
| Sprache | Java 21 |
| Framework | Spring Boot 4 |
| Process Engine | CIB Seven 2.2.0 |
| Datenbank | H2 (eingebettet, dateibasiert; JPA / Hibernate) |
| Build | Maven |
| Architektur-Tests | ArchUnit |

## CIB Seven

[CIB Seven](https://cibseven.org) ist eine community-gepflegte Distribution von Camunda Platform 7. Sie bietet volle Kompatibilität mit der Camunda-7-API und wird unabhängig als Open Source weiterentwickelt.

In diesem Projekt läuft CIB Seven eingebettet in Spring Boot, stellt die Camunda-Webanwendung unter `http://localhost:8080/webapp/#/seven/auth/start` bereit und übernimmt die BPMN-Prozessausführung für den Inner-Circle-Membership-Prozess.

Service Tasks werden über das `JavaDelegate`-Pattern mit `DelegateExpression` angebunden:

```java
@Component
public class SendWelcomeMailDelegate extends BaseDelegate {

    private final SendWelcomeMailUseCase useCase;

    public SendWelcomeMailDelegate(SendWelcomeMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var subscriptionId = (String) execution.getVariable("subscriptionId");
        useCase.sendWelcomeMail(new SubscriptionId(UUID.fromString(subscriptionId)));
    }
}
```

## Architektur

Das Projekt folgt einer **hexagonalen Architektur** (Ports & Adapters):

```
REST / JavaDelegates           Application              CIB7 / Database
  (Inbound-Adapter)    →   Ports + Services   →     (Outbound-Adapter)
                               ↑
                            Domain
                        (engine-neutral)
```

Architekturregeln werden zur Build-Zeit über [ArchUnit](https://www.archunit.org/)-Tests sichergestellt.
