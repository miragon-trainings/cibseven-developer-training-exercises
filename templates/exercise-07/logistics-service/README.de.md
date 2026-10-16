# Aufgabe 7 – Starter: `logistics-service`

> 🇩🇪 **Deutsch** · [🇬🇧 English](README.md)

Dies ist die **Vorlage** für den Logistik-Service aus Aufgabe 7 (Remote-Owner des `sendWelcomeKit`-Prozesses).
Sie liegt bewusst hier unter `templates/` und **nicht** in `services/` – so bleibt `services/` für die
Aufgaben 0–8 aufgeräumt.

## So legst du den Service in Aufgabe 7 an

1. Diesen Ordner nach `services/logistics-service` kopieren:

   ```bash
   cp -R templates/exercise-07/logistics-service services/logistics-service
   ```

2. Das Modul in der root `pom.xml` unter `<modules>` eintragen:

   ```xml
   <module>services/logistics-service</module>
   ```

3. Die `TODO Exercise 7`-Stellen ausfüllen (Details in [`docs/exercise-07.md`](../../../docs/de/exercise-07.md)):
   - **`send-welcome-kit.bpmn`** – enthält bewusst nur ein leeres Modell mit einem Start-Event.
     Modelliere den `sendWelcomeKit`-Prozess selbst (Prozess-ID, Signal- + manuelles Start-Event,
     External Service Task mit Topic, End-Event).
   - **`pom.xml`** – die beiden auskommentierten Generator-Blöcke (bpmn-to-code + OpenAPI-Client)
     einkommentieren und die `TODO`-Werte setzen → erzeugt Process-API + typisierten `/engine-rest`-Client.
   - **`EngineClientConfig`** – die `ProcessDefinitionApi`-Bean bereitstellen.
   - **`EngineDeploymentAdapter`** – das eigene Modell beim Start deployen.
   - **`ShipWelcomeKitWorker`** – den External-Task erfüllen (die Klasse ist absichtlich leer).
   - **`RemoteWelcomeKitProcessAdapter`** – den Prozess über den Client starten.

Referenzlösung: [`solutions/exercise-07/logistics-service`](../../../solutions/exercise-07/logistics-service).

> Tipp: Die Vorlage kompiliert schon im Ausgangszustand (der Client-Teil ist auskommentiert). Zum Prüfen
> der Vorlage ohne Kopieren: `./mvnw -Pexercise-07 -pl templates/exercise-07/logistics-service compile`.
