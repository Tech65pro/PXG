# tools/

Everything here is **offline build and verification scaffolding**. None of it is part of
VortexMC, none of it is shipped in the jar, and none of it is needed to build the mod on a
machine with network access.

It exists because the environment this repository was first prepared in has no route to
`services.gradle.org`, `maven.fabricmc.net`, `repo1.maven.org` or any Mojang host, so
`./gradlew build` cannot run here. See `README.md`, "What was and was not verified".

| Path | What it is |
|---|---|
| `provision.sh` | Downloads every real artifact the build needs: a runnable JDK 17 built with `jlink` from GitHub-hosted `jmod`s, the vanilla obfuscated 1.21.11 client jar, the Fabric and Minecraft library jars, and the Yarn mappings. Takes a while and needs ~2 GB. |
| `real-build.sh` | Compiles `src/main/java` + `src/client/java` against the **real** Minecraft 1.21.11 and **real** Fabric API, remaps to intermediary with Tiny Remapper (the same remapper Loom uses), and packages `build/libs/vortexmc-1.0.0.jar`. This is the script that produced the jar in `build/libs/`. |
| `real-build/Remap.java` | Tiny Remapper CLI driver: intermediary → named for compilation, named → intermediary for the shipped jar. |
| `real-build/Downgrade.java` | ASM pass that lowers the class-file version word from 65 to 61 so a JDK 17 `javac` can read the Java 21 bytecode Mojang ships. Names and descriptors are untouched. |
| `run-tests.sh` | Compiles and runs the 47 unit tests with no network and no JUnit, using the stand-ins below. |
| `junit-api/` | Minimal stand-ins for the JUnit 5 annotations and assertions the tests use. |
| `offline-verify/TestRunner.java` | Reflectively runs the `src/test/java` tests. |

`tools/mcapi/`, `tools/offline-build.sh` and `tools/offline-jdk.sh` were removed. `mcapi/`
mimicked the Minecraft and Fabric API with hand-written stubs, which meant a jar built from it
was compiled against nothing real; `offline-build.sh` produced exactly such a jar.
`real-build.sh` supersedes both. Nothing in the shipped jar was ever compiled against a stub.

## Reproducing the whole thing

```bash
# 1. fetch the real artifacts (~2 GB, one time)
bash tools/provision.sh

# 2. compile against the real API, remap, package
bash tools/real-build.sh

# 3. unit tests (no network needed)
bash tools/run-tests.sh
```

## On a machine with network access

```bash
./gradlew build      # authoritative production build: Loom -> remap -> jar
./gradlew test
```

`./gradlew build` could not be executed while preparing this repository, because every Gradle,
Maven and Mojang host is unreachable from that sandbox. That is the one thing that still has to
be run somewhere else; see `README.md`, "What was and was not verified".
