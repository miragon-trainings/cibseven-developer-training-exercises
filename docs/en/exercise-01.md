# Exercise 1 – Getting the engine running

> **Prerequisite:** Exercise 0 is complete (the target process exists at the business level).
> **Working directory:** `services/process-application`
> **New in this exercise:** CIB Seven starter, engine configuration, embedded database (H2), auto-deployment, Cockpit, H2 console, `act_*` tables, start form, Manual Task.

## What this is about

In Exercise 0 you modeled the **complete target process** of the Inner Circle. Before anyone
automates the whole flow, an external consultant roughed out a **deliberately tiny first
excerpt**: a start form for the registration data, followed by two **Manual Tasks** –
"Confirm" and "Send welcome mail". Manual Tasks are placeholders: the engine simply runs
through them. That way you get a process instance that runs from start to finish – without a
single line of code.

This mini-version is **not** the target process and **not** the target model. It exists only
to get the engine running once and to get a feel for deployment, execution, and the data. From
Exercise 2 on you build the placeholders out into real steps, one at a time.

What's still missing is the **runtime environment**: a Spring Boot module in which the CIB Seven
engine runs. That's exactly what you'll set up now.

## Learning goals

After this exercise you can

- get a Spring Boot module with an embedded CIB Seven engine up and running,
- name what the engine needs in order to start (database, auto-configuration, auto-deployment),
- find your way around the Cockpit between Processes, Tasklist, and Admin,
- map the `act_*` tables to the categories Repository, Runtime, and History,
- start an instance from the Tasklist via a **start form**,
- explain why an instance made up of nothing but **Manual Tasks** runs through without stopping.

## Target model

![BPMN model of the exercise](../assets/exercise-01.svg)

This is the deliberately reduced excerpt: start form → Manual Task "Confirm" → Manual Task
"Send welcome mail" → End. It sits ready at
`services/process-application/src/main/resources/bpmn/membership.bpmn`. You won't model anything
in this exercise – you'll bring it to life.

## The task

> This is about **setting up and getting familiar** – no business code.

### 1. Enable the dependencies

Open `services/process-application/pom.xml` and uncomment the `TODO Exercise 1` block: the two
CIB Seven starters (`webapp-4` and `rest-4`). Only with these are the engine, the Cockpit webapp,
and the REST API present in the module. The versions come centrally from the root `pom.xml` – so
add them **without** a `<version>`.

### 2. Enable the configuration

The engine stores its entire state in a relational database. In this training that's an **H2**
database that runs right inside the application and stores its data in a file under
`~/.cibseven-training/`. There's nothing to install and nothing to start separately: H2 creates the
file on the first start, and the engine creates its `act_*` tables in it by itself.

In `services/process-application/src/main/resources/application.yaml`, uncomment the
`TODO Exercise 1` block. It contains:

- the database connection to the H2 file (`jdbc:h2:file:~/.cibseven-training/exercise`, user
  `sa`, empty password),
- the **H2 console** – an SQL interface in the browser that is part of the application; you'll need
  it in step 5,
- the Cockpit admin user,
- the webclient, including the JWT secret for the Cockpit login.

Without this block the application won't start. Spring Boot does fall back to a throwaway in-memory
H2 on its own, but startup then fails at the Cockpit, which is missing its JWT secret
(`Secret must be at least 155 characters long and a base64 decodable string`).

> **Term: embedded database.** A database that runs as a library **in the same process** as the
> application instead of as a separate server. It starts and stops with the application; because
> H2 writes to a file here, the data still survives a restart.

### 3. Arm the application

In `TrainingApplication.java`, enable the commented-out annotations
**`@SpringBootApplication`** and **`@EnableJpaRepositories`**. Only then do auto-configuration
and the automatic BPMN deployment kick in: all `*.bpmn` files under `src/main/resources` are
deployed into the engine at startup.

### 4. Start the application

Now everything comes together. Watch the log – it shows how auto-configuration brings up the
engine and deploys the BPMN file.

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

### 5. Look at the engine's tables

On the first start the engine created its data model itself: several dozen tables with the prefix
`act_`. Look at them in the H2 console, which is built into the running application:

1. Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console).
2. In the **JDBC URL** field, replace the pre-filled value `jdbc:h2:~/test` with
   `jdbc:h2:file:~/.cibseven-training/exercise` – otherwise the console looks for a database that
   doesn't exist.
3. Keep the user `sa`, leave the password empty, and click **Connect**.

The console now lists the tables in the left-hand column (H2 writes their names in upper case; in
SQL the case doesn't matter). These five tables are the most important:

| Table | Prefix | Content |
|---|---|---|
| `act_re_procdef` | `re` = Repository | deployed **process definitions** (`subscribeNewsletter`) |
| `act_ru_execution` | `ru` = Runtime | **running** process instances |
| `act_ru_task` | `ru` | open **User Tasks** |
| `act_ru_variable` | `ru` | **process variables** of running instances |
| `act_hi_procinst` | `hi` = History | **completed** process instances |

As a start, check whether your model was deployed. Type the query into the console's input field
and click **Run**:

```sql
SELECT key_, name_, version_ FROM act_re_procdef;
```

### 6. Explore the Cockpit

The Cockpit is the engine's web interface. Open
[http://localhost:8080/webapp/#/seven/auth/start](http://localhost:8080/webapp/#/seven/auth/start)
(admin / admin). Under **Processes** you'll see `Join Inner Circle` – the display name of the
model; the technical process key behind it is `subscribeNewsletter`. Click your way through
**Cockpit**, **Tasklist**, and **Admin**.

### 7. Play through the process

The process is deployed, but has never run. Start it via the start form:

1. **Tasklist** → **Start process** → `Join Inner Circle`
2. A **start form** appears with the fields `email`, `name`, `age`. Fill it in and start.
3. The instance runs **without stopping** to the end – both Manual Tasks are simply run through
   by the engine.
4. In `act_hi_procinst` the instance shows as `COMPLETED`; `act_ru_*` is empty again.

> **Term: start form (Generated Form).** The fields `email`/`name`/`age` sit as `camunda:formData`
> directly on the Start Event. When you start, the Tasklist renders a form from them automatically –
> no extra file, no HTML. The entered values become process variables of the instance.

> **Term: Manual Task.** A task the engine does **not** execute and does **not** wait at – it
> simply runs through. A placeholder for "something happens here later". That's why the instance
> runs through the model in one go. From Exercise 2 on the first placeholder becomes a real User
> Task.

## Constraints

- You work exclusively in the module `services/process-application`.
- The module already contains the complete skeleton of the hexagonal architecture. The business
  layer is commented out and isn't needed here yet.
- All modules run on port `8080` and use the same H2 file under `~/.cibseven-training/`. Always
  start only one at a time – the running application locks the file.

## Expected result

The log shows `Auto-Deploying resources: [... membership.bpmn]` and
`Started TrainingApplication`. The Cockpit is reachable, `Join Inner Circle` appears under
**Processes**, and an instance started via the start form runs to the End Event without
stopping.

## Self-check

- [ ] Dependencies, configuration, and `@SpringBootApplication` are enabled
- [ ] The application starts and deploys `membership.bpmn`
- [ ] The H2 console shows the `act_*` tables, and `act_re_procdef` contains `subscribeNewsletter`
- [ ] `Join Inner Circle` appears in the Cockpit under **Processes**
- [ ] An instance started via the start form runs all the way through (History `COMPLETED`)
- [ ] You can explain why the instance made up of nothing but Manual Tasks never waits

## Hints

- If startup fails with
  `Database may be already in use: "…/.cibseven-training/exercise.mv.db"`, another application is
  already running on the same file – a different module, or the same one in a second terminal.
  Stop it and start again. For the same reason, external database tools (such as *Database* in
  IntelliJ) can't open the file while the application is running – use the H2 console.
- If the H2 console reports
  `Database "…/test" not found, either pre-create it or allow remote database creation …`, the
  **JDBC URL** field still holds the pre-filled value. Replace it with
  `jdbc:h2:file:~/.cibseven-training/exercise` as in step 5.
- The data survives a restart of the application. For a clean slate, stop the application and
  delete the folder `~/.cibseven-training` (on Windows `%USERPROFILE%\.cibseven-training`); on the
  next start, H2 and the engine create everything anew.
- The prefixes are a handy mnemonic: `re` is fixed, `ru` is moving, `hi` is the past.

## Reference solution

- Finished module: `../../solutions/exercise-01/`
- Model: `../../models/exercise-01/membership.bpmn`
- Load it directly into the working module:

  ```bash
  ./mvnw -pl services/process-application antrun:run@load-solution -Dsolution=01
  ```

## Next step

In Exercise 2 the placeholder "Confirm" becomes a real **User Task**, which you model yourself
with a form.

➡️ [Next: Exercise 2](exercise-02.md)
