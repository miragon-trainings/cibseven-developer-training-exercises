# Exercise 7 – Starter: `logistics-service`

> [🇩🇪 Deutsch](README.de.md) · 🇬🇧 **English**

This is the **template** for the logistics service from Exercise 7 (remote owner of the `sendWelcomeKit` process).
It deliberately lives here under `templates/` and **not** in `services/` – that keeps `services/` tidy for
Exercises 0–8.

## How to create the service in Exercise 7

1. Copy this folder to `services/logistics-service`:

   ```bash
   cp -R templates/exercise-07/logistics-service services/logistics-service
   ```

2. Register the module in the root `pom.xml` under `<modules>`:

   ```xml
   <module>services/logistics-service</module>
   ```

3. Fill in the `TODO Exercise 7` spots (details in [`docs/exercise-07.md`](../../../docs/en/exercise-07.md)):
   - **`send-welcome-kit.bpmn`** – deliberately contains only an empty model with a start event.
     Model the `sendWelcomeKit` process yourself (process ID, signal + manual start event,
     external service task with topic, end event).
   - **`pom.xml`** – uncomment the two commented-out generator blocks (bpmn-to-code + OpenAPI client)
     and set the `TODO` values → generates the Process API + a typed `/engine-rest` client.
   - **`EngineClientConfig`** – provide the `ProcessDefinitionApi` bean.
   - **`EngineDeploymentAdapter`** – deploy your own model on start-up.
   - **`ShipWelcomeKitWorker`** – fulfil the external task (the class is intentionally empty).
   - **`RemoteWelcomeKitProcessAdapter`** – start the process via the client.

Reference solution: [`solutions/exercise-07/logistics-service`](../../../solutions/exercise-07/logistics-service).

> Tip: The template already compiles in its initial state (the client part is commented out). To check
> the template without copying it: `./mvnw -Pexercise-07 -pl templates/exercise-07/logistics-service compile`.
