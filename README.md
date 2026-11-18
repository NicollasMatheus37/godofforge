# God of Forge (GoF)

A forging-focused Minecraft mod for **NeoForge**.

Shape molds, hammer parts on the anvil, build your own forges and rise through the ranks of smithing mastery
(Apprentice → Journeyman → Master).

| | |
|---|---|
| Minecraft | 26.3 |
| Loader | NeoForge 26.3.0.103 |
| Java | 25 |
| Mod id | `godofforge` |
| License | MIT |

## Development

Requirements: JDK 25 (Eclipse Temurin recommended) and an IDE (IntelliJ IDEA recommended).

```bash
./gradlew runClient    # start the game with the mod
./gradlew runServer    # start a dedicated server with the mod
./gradlew runData      # run data generators
./gradlew build        # build the mod jar into build/libs
```

On Windows use `gradlew.bat` instead of `./gradlew`.

In IntelliJ IDEA, open the project folder, let Gradle sync finish, then use the generated `client` run configuration.

## Status

Phase 0: project setup and a test item (`godofforge:test_ingot`).
