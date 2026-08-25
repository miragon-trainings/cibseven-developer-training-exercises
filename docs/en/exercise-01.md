# Exercise 1 – Getting the engine running

> **Prerequisite:** Exercise 0 is complete (the target process exists at the business level).
> **Working directory:** `services/process-application`
> **New in this exercise:** CIB Seven starter, engine configuration, auto-deployment, Cockpit, `act_*` tables, start form, Manual Task.

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

### 1. Start the database

The engine stores its entire state in a relational database. So bring up the container stack first;
it comes with PostgreSQL and MailHog. Use Docker or Podman — the compose file works with both:

```bash
cd stack && docker compose up -d
# or with Podman:
cd stack && podman compose up -d
```

### 2. Create the database schema

All modules share the schema `exercise`. Create it once:

```bash
docker exec -i postgres psql -U admin -d cibseven-training < stack/init-schemas.sql
# or with Podman:
podman exec -i postgres psql -U admin -d cibseven-training < stack/init-schemas.sql
```

### 3. Enable the dependencies

Open `services/process-application/pom.xml` and uncomment the `TODO Exercise 1` block: the two
CIB Seven starters (`webapp-4` and `rest-4`). Only with these are the engine, the Cockpit webapp,
and the REST API present in the module. The versions come centrally from the root `pom.xml` – so
add them **without** a `<version>`.

### 4. Enable the configuration

In `services/process-application/src/main/resources/application.yaml`, uncomment the
`TODO Exercise 1` block: database connection, Cockpit admin user, and webclient. Without a
database connection the engine won't start.

### 5. Arm the application

In `TrainingApplication.java`, enable the commented-out annotations
**`@SpringBootApplication`** and **`@EnableJpaRepositories`**. Only then do auto-configuration
and the automatic BPMN deployment kick in: all `*.bpmn` files under `src/main/resources` are
deployed into the engine at startup.

### 6. Start the application

Now everything comes together. Watch the log – it shows how auto-configuration brings up the
engine and deploys the BPMN file.

```bash
cd services/process-application && ../../mvnw spring-boot:run
```

### 7. Look at the engine's tables

On the first start the engine created its data model itself: several dozen tables with the prefix
`act_`. Connect to the database with a tool of your choice (in IntelliJ via *Database*, in VS Code
via *SQLTools*), host `localhost`, port `5432`, database `cibseven-training`, user `admin`,
password `admin`. These five tables are the most important:

| Table | Prefix | Content |
|---|---|---|
| `act_re_procdef` | `re` = Repository | deployed **process definitions** (`subscribeNewsletter`) |
| `act_ru_execution` | `ru` = Runtime | **running** process instances |
| `act_ru_task` | `ru` | open **User Tasks** |
| `act_ru_variable` | `ru` | **process variables** of running instances |
| `act_hi_procinst` | `hi` = History | **completed** process instances |

As a start, check whether your model was deployed:

```sql
SELECT key_, name_, version_ FROM exercise.act_re_procdef;
```

### 8. Explore the Cockpit

The Cockpit is the engine's web interface. Open
[http://localhost:8080/webapp/#/seven/auth/start](http://localhost:8080/webapp/#/seven/auth/start)
(admin / admin). Under **Processes** you'll see `Join Inner Circle` – the display name of the
model; the technical process key behind it is `subscribeNewsletter`. Click your way through
**Cockpit**, **Tasklist**, and **Admin**.

### 9. Play through the process

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
- All modules run on port `8080` and in the schema `exercise`. Always start only one at a time.

## Expected result

The log shows `Auto-Deploying resources: [... membership.bpmn]` and
`Started TrainingApplication`. The Cockpit is reachable, `Join Inner Circle` appears under
**Processes**, and an instance started via the start form runs to the End Event without
stopping.

## Self-check

- [ ] PostgreSQL is running, the schema `exercise` exists
- [ ] Dependencies, configuration, and `@SpringBootApplication` are enabled
- [ ] The application starts and deploys `membership.bpmn`
- [ ] `Join Inner Circle` appears in the Cockpit under **Processes**
- [ ] An instance started via the start form runs all the way through (History `COMPLETED`)
- [ ] You can explain why the instance made up of nothing but Manual Tasks never waits

## Hints

- If the application starts with a database error, check step 2 first: without the schema
  `exercise` the engine can't find its tables.
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
