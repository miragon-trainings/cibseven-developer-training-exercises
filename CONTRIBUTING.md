# Contributing

This page is for trainers and maintainers who change the exercises, the solutions or the build. Participants start in the [README](README.md).

## Build and run

You need JDK 21. The Maven wrapper downloads the pinned Maven version on its first run.

```bash
./mvnw clean install                                          # build and test everything
cd services/process-application && ../../mvnw spring-boot:run # the participants' module
cd solutions/exercise-03 && ../../mvnw spring-boot:run        # a reference solution
./mvnw test -Dtest=ArchitectureTest                           # the architecture rules only
```

| What | Where |
|---|---|
| CIB seven Cockpit | `http://localhost:8080/webapp/#/seven/auth/start` (`admin` / `admin`) |
| H2 console | `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:file:~/.cibseven-training/exercise`, user `sa`, no password (replace the pre-filled `jdbc:h2:~/test`) |

All modules use port `8080` and the same H2 file `~/.cibseven-training/exercise.mv.db`, so only one runs at a time. Process tests (Exercise 4 on) use a separate in-memory H2 under the `test` profile.

The Maven wrapper needs `mvnw`, `mvnw.cmd` and `.mvn/`. IntelliJ imports the project with the wrapper's Maven; on WSL or Git Bash that import can fail, then set **Maven home path** to **Bundled (Maven 3)** in IntelliJ's Maven settings.

## Repository structure

```
├── docs/                              # Exercise descriptions: docs/en/, docs/de/, assets/
├── services/process-application/      # The module participants work in (starter state of Exercise 1)
├── templates/
│   ├── process-application-starter/   # Byte copy of the starter, the baseline for load-solution
│   └── exercise-07/logistics-service/ # Starter for Exercise 7, copied into services/ by participants
├── solutions/                         # Cumulative solutions: exercise-01 … exercise-07, extra-task-1
├── models/                            # Reference BPMN/DMN models
└── pom.xml
```

`solutions/exercise-07/` and `solutions/extra-task-1/` are nested into `process-application/` and `logistics-service/`.

## Architecture

Every module follows a hexagonal architecture (ports and adapters), enforced by ArchUnit in `ArchitectureTest`:

```
REST / JavaDelegates           Application              CIB seven / Database
  (inbound adapters)   →   ports + services   →     (outbound adapters)
                               ↑
                            Domain
                        (engine-neutral)
```

The domain has no framework imports; adapters depend only on application ports.

## Changing the starter or a solution

`load-solution` (`-Dsolution=00` … `07`) resets `services/process-application` to `templates/process-application-starter/` and copies the solution on top. Three rules keep it working:

- **The starter template stays a byte copy** of `services/process-application` (`src/` and `pom.xml`). CI checks it with `diff -r`. After changing the starter, sync it:

  ```bash
  rm -rf templates/process-application-starter/src \
    && cp -R services/process-application/src templates/process-application-starter/ \
    && cp services/process-application/pom.xml templates/process-application-starter/
  ```

- **`TODO Exercise N` markers are load-bearing.** A starter file with that marker is replaced from solution N on, so solution N and every later solution must contain it. A file a solution deletes or renames needs a marker.
- **Every dependency or plugin a solution adds** must exist in the starter `pom.xml` as a commented `@exercise-N` block, or `load-solution` won't bring it.

Check the task locally before changing the starter, a solution or the task itself:

```bash
bash .github/scripts/check-load-solution.sh baseline
bash .github/scripts/check-load-solution.sh load 03
```

## Changing an exercise

Structure, language and quality criteria of the exercise descriptions are in [`docs/aufgaben-template.md`](docs/aufgaben-template.md) (German). Change the English and the German version together, and review the result with the `aufgaben-review` Claude Code skill in `.claude/skills/`.

## Pull requests

Use semantic commit messages (`feat:`, `fix:`, `docs:` …). The build workflow runs on every pull request against `main`.
