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
zuerst den kompletten Sollprozess fachlich und automatisierst ihn dann Schritt für Schritt.

Was folgt, ist eine Reise durch immer komplexere BPMN-Muster: Gateways, Boundary Events,
Subprozesse, Parallel Gateways, Call Activities, DMN-Entscheidungstabellen und Kompensation —
jede Aufgabe baut auf der vorherigen auf.

![Prozessmodell](docs/newsletter-subscription.png)

### Aufgabenübersicht

Detaillierte Aufgabenbeschreibungen befinden sich in [`docs/`](docs/).

| Aufgabe | Thema | Beschreibung |
|---|---|---|
| [0](docs/de/exercise-00.md) | Fachliche BPMN-Modellierung | Den kompletten Inner-Circle-Membership-Prozess rein fachlich modellieren — die gemeinsame Vorlage für das ganze Training |
| [1](docs/de/exercise-01.md) | Engine zum Laufen bringen | Das vorgegebene Start-Formular-/Manual-Task-Modell durchlaufen lassen, Cockpit und `act_*`-Tabellen der Engine kennenlernen |
| [2](docs/de/exercise-02.md) | Der erste Wartepunkt | Aus dem Manual Task „Confirm" einen User Task machen und ihm eine selbst erstellte Generated Form geben |
| [3](docs/de/exercise-03.md) | Einen Schritt automatisieren | Aus dem Manual Task „Send welcome mail" einen Service Task mit JavaDelegate machen (Start über Cockpit) |
| [4](docs/de/exercise-04.md) | Die Anwendung übernimmt | Message Start Event, REST-Endpunkte für Register + Confirm, Nachrichten-Korrelation, Persistenz |
| [5](docs/de/exercise-05.md) | Kapazitätsprüfung mit Gateway | Exclusive Gateway, Transaktionsgrenzen, Business Key, Task-Formular |
| [6](docs/de/exercise-06.md) | Prozess-Tests | Prozess-Unit-Test mit In-Memory-Engine, gemockten Use Cases, ohne PostgreSQL |
| [6 · Add-on](docs/de/exercise-06-addon.md) | bpmn-to-code | Element-IDs als generierte Konstanten statt handgetippter Strings |
| [7](docs/de/exercise-07.md) | Subprozess, Boundary Events & Parallelität | Subprozess, Timer- und Message-Boundary-Events, Parallel Gateway, Teams-Anbindung |
| [8](docs/de/exercise-08.md) | Kompensation (SAGA) | Compensation Boundary Event, Compensating End Event, Kompensations-Handler |
| [9](docs/de/exercise-09.md) | Call Activity & DMN | Call Activity, DMN-Entscheidungstabelle, Business Rule Task |
| [10](docs/de/exercise-10.md) | Remote Engine als geteilte Infrastruktur | Eine Abteilung besitzt einen **eigenen** kleinen Prozess (`sendWelcomeKit`) in ihrem Remote-Service: Modell, Worker, Deployment und Tests liegen dort; getriggert per Signal-Broadcast; die Engine über einen generierten OpenAPI-Client getrieben |
| [Extra 1](docs/de/extra-task-1.md) | Process-Engine-API | Aufgabe 10 engine-neutral umbauen: Worker statt JavaDelegate, Adapter-Tausch statt Engine-Lock-in |

> Aufbau, Sprache und Qualitätskriterien der Aufgaben sind in
> [`docs/aufgaben-template.md`](docs/aufgaben-template.md) festgehalten. Neue oder geänderte
> Aufgaben werden mit dem Skill `aufgaben-review` gegengeprüft
> (`.claude/skills/aufgaben-review/SKILL.md`).

## Quick Start

```bash
# PostgreSQL + MailHog starten (Docker oder Podman)
cd stack && docker compose up -d      # Podman: podman compose up -d

# Alles bauen
./mvnw clean install

# process-application-Modul starten (das eine Modul, in dem du alle Aufgaben bearbeitest)
cd services/process-application && ../../mvnw spring-boot:run

# CIB Seven Cockpit
open http://localhost:8080/webapp/#/seven/auth/start    # admin / admin
```

### Lösung einer Aufgabe laden

Das `process-application`-Modul startet im Zustand von **Aufgabe 1** (Engine noch auskommentiert). Wenn du
eine Aufgabe nicht ganz fertig bekommst, kannst du die Referenzlösung in dein `process-application`-Modul
kopieren und mit ihr weiterarbeiten:

```bash
# solutions/exercise-02 in das process-application-Modul kopieren (gültige Werte: 01–10, zweistellig)
./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=02
```

Der Task ersetzt `src/main` komplett (Java, `application.yaml`, BPMN/DMN); `src/test` bleibt
unberührt. Alle Module – das `process-application`-Modul **und** alle Solutions – laufen auf demselben Port
(`8080`) und demselben DB-Schema (`exercise`); es läuft also immer nur **ein** Modul zur Zeit.
Die in **Aufgabe 1** aktivierten CIB-Seven-Abhängigkeiten (`pom.xml`) bleiben bestehen – lade
eine Lösung ab `exercise-2` daher erst, nachdem Aufgabe 1 abgeschlossen ist.

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
│   └── (logistics-service/)          # erst in Aufgabe 10 aus templates/ angelegt (Remote-Owner)
├── templates/
│   └── exercise-10/logistics-service/ # Vorlage für den Aufgabe-10-Worker (in services/ kopieren)
├── solutions/                        # Kumulative Lösungen pro Aufgabe (exercise-01 … exercise-10, extra-task-1)
│   ├── exercise-{01-10}/             # exercise-10/ ist verschachtelt: process-application/ + logistics-service/
│   └── extra-task-1/
├── models/                           # Referenz-BPMN-/DMN-Modelle
├── stack/
│   ├── docker-compose.yml            # PostgreSQL + MailHog (Docker oder Podman)
│   └── init-schemas.sql
└── pom.xml
```

## Technologie-Stack

| Komponente | Technologie |
|---|---|
| Sprache | Java 21 |
| Framework | Spring Boot 4 |
| Process Engine | CIB Seven 2.2.0 |
| Datenbank | PostgreSQL (JPA / Hibernate) |
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
