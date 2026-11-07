# VortexMC

A client-side performance mod for **Minecraft Java Edition 1.21.11** (Fabric).

VortexMC measures what your game is actually doing, shows it to you in one place, and lets you
change the graphics settings that matter on a weak machine. Its Smart Auto mode watches the frame
rate, proposes one conservative change at a time, asks first, and then **measures the result
before saying anything about it**.

Mod id `vortexmc`, package `com.tech65pro.vortexmc`, client-side only, Java 21.

---

## What VortexMC will and will not tell you

These are design rules, not disclaimers. Every one of them is enforced somewhere in the code.

| VortexMC does | VortexMC does not |
|---|---|
| Measure frame times from real rendered frames, once per frame | Estimate, interpolate or invent any number |
| Say "measuring..." when there is not enough data yet | Show a zero or a placeholder that looks like data |
| Report a gain only after measuring it across a whole window | Claim a change helped because it "should" |
| Describe a setting change as "less work for the renderer" | Promise any specific FPS increase |
| Read the heap from `Runtime` | Call `System.gc()` to tidy a number up |
| Detect other mods by id through the loader | Read, reflect into or copy another mod's code |
| Change only settings you asked it to change | Touch anything silently |
| Say a metric is unavailable when it is | Report `0` for "unknown" |

---

## Installing

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.18.4 or newer** for Minecraft 1.21.11.
   0.18.4 is the version this build was compiled and linked against; the Fabric API modules it
   needs only ask for `>=0.17.3`.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) **0.141.6+1.21.11 or newer**. Only
   three modules are needed, but the bundle is easiest:
   `fabric-key-binding-api-v1`, `fabric-lifecycle-events-v1`, `fabric-rendering-v1`.
3. Drop `vortexmc-1.0.1.jar` into `.minecraft/mods/`.
4. Optional: install [ModMenu](https://modrinth.com/mod/modmenu) **17.0.1 or newer** to get a
   config button in the mod list. VortexMC works without it.

**Keys** (both rebindable in Controls → VortexMC):

| Key | Action |
|---|---|
| `H` | Open the dashboard |
| `J` | Toggle the in-game overlay |

The dashboard can also be opened from ModMenu's mod list.

### Building from source

```bash
./gradlew build
```

The jar lands in `build/libs/`. Requires **JDK 21** and network access. The Gradle wrapper
(`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, **Gradle 9.8.2**) is committed,
so there is nothing to install first.

Toolchain versions, all checked against the upstream repositories:

| Component | Version | Why |
|---|---|---|
| Gradle | 9.8.2 | Loom 1.19 requires Gradle 9.x; 8.x is not supported |
| Fabric Loom | `1.19` (plugin id `net.fabricmc.fabric-loom-remap`) | Loom 1.13 and older predate MC 1.21.11 and cannot resolve it; 1.14 is the first release with 1.21.11 support. The remapping plugin id is used because the mod is written against Yarn, not Mojang mappings |
| Yarn mappings | `1.21.11+build.3` | **The version the shipped jar was compiled against.** See below for why not build.6 |
| Fabric Loader | `0.18.4` | The version the shipped jar was compiled and linked against. 0.20.1 is newer but was not used |
| Fabric API | `0.141.6+1.21.11` | Latest published build for 1.21.11; there is no `0.135.0+1.21.11` |
| ModMenu | `17.0.1` | The release at the head of ModMenu's own 1.21.11 branch. The offline compile used `17.0.0-beta.1`; see below |

Every row above is either the version this mod was really compiled against, or is flagged in
*What was and was not verified* as unverified. Nothing here is claimed to have been tested
unless it was.

Two things in `build.gradle` are load-bearing and easy to break by accident:

* **`splitEnvironmentSourceSets()` must stay in the `loom {}` block.** Loom does not detect
  `src/client/java` on its own. Without it there is no `client` source set, `src/client/java` is
  never compiled, and `com.tech65pro.vortexmc.VortexClient` is missing from the jar — which makes
  Fabric Loader fail at startup with a missing-entrypoint error.
* **The `mods { "vortexmc" { sourceSet sourceSets.main; sourceSet sourceSets.client } }` block**
  tells Loom which source sets belong to this mod so the dev run configuration puts both outputs
  on the classpath.

---

## The dashboard

Seven tabs, dark with blue/purple accents, drawn to stay cheap on a weak GPU.

**Overview** — current frame rate, 1% low, frame time and session average as cards, plus the live
Smart Auto state and a short note on how to read the numbers.

**FPS Monitor** — average FPS and 1% low over the last five minutes as sparklines, then the full
statistics table: samples in the rolling window, instant FPS, average FPS, 1% and 0.1% lows,
median / average / worst frame time, frame-time spread, session range, trend per second, and
whether a sustained drop has been detected.

**Graphics** — every setting VortexMC supports, as sliders and toggles: render distance,
simulation distance, entity distance, cloud render distance, particles, clouds, smooth lighting,
VSync, mipmap levels, biome blend radius, brightness, framerate limit, inactivity framerate limit
and GUI scale. Each control reads the live value from the game, so what you see is what is
configured. Values are clamped to the ranges the vanilla video screen allows.

**Memory** — used / committed / maximum heap, used percentages, uncommitted heap, processor
count, and an assessment. There is deliberately no "collect now" button; see below.

**Network** — connection state, server name and address, latency, known player count, packet
rates in each direction, entity counts, and whether the connection is open.

**Profiles** — the three built-in profiles, your custom profiles, and a field to save the current
settings as a new one.

**Settings** — Smart Auto behaviour (enabled, ask before applying, measure a baseline first,
auto-revert a measured regression, measurement length, cooldown, target frame rate), interface
options, the change history, and which other mods VortexMC detected.

---

## Manual mode

Move a slider and the change is written straight to the live game options and persisted to
`options.txt`, exactly as the vanilla video settings screen does. Three built-in profiles are
provided:

| Profile | Particles | Clouds | Render | Sim | Entity dist. | Smooth lighting | Mipmaps |
|---|---|---|---|---|---|---|---|
| Low-End | Minimal | Off | 6 | 5 | 50% | off | 0 |
| Balanced | Decreased | Fast | 10 | 8 | 75% | on | 2 |
| High-Performance | All | Fancy | 16 | 12 | 100% | on | 4 |

The Low-End profile is the conservative one and is aimed at integrated graphics such as Intel HD
Graphics 4600. "Save the current settings as a profile" creates a custom profile you can tune and
re-apply; built-in profiles are never overwritten.

---

## Smart Auto mode

Smart Auto is a small state machine:

```
IDLE --requestAnalysis()--> AWAITING_APPROVAL --approve()--> MEASURING_BEFORE
                                   |                              | enough samples
                                   | decline()                    v
                                   v                            (apply) -> MEASURING_AFTER
                                 IDLE                                              | enough samples
                                                                                   v
                                                          EVALUATED (or REVERTED)
```

1. **Watch.** Frame times are sampled once per rendered frame. A one-hertz rollup feeds the trend
   analysis.
2. **Propose.** At most one change is suggested, always the gentlest one that applies. Nothing is
   proposed while the game is running above your target frame rate. If the machine is really
   struggling, it skips ahead to render distance rather than offering cosmetics first.
3. **Ask.** With `requireApproval` on (the default) nothing is applied until you say so. The
   proposal states what will change and why that removes work.
4. **Baseline.** On approval, the current settings are snapshotted and a baseline window is
   measured first, so the comparison is against your machine *now*, not against a stale average.
5. **Apply.** One setting changes. The change is recorded in the history with its before and after
   values.
6. **Measure.** A second window of the same length is measured.
7. **Report honestly.** The two windows are compared. A change only counts as an improvement if
   the effect is larger than 2% *and* present throughout the window; a one-off spike does not
   count. If it measured worse and `autoRevertFailedChanges` is on, the previous values are
   restored automatically. Otherwise you can undo it at any time, or restore the last known-good
   snapshot from the Settings tab.

Smart Auto never changes more than one setting at a time, never stacks unverified changes, and
never reports a gain it did not measure.

---

## Why there is no "collect garbage" button

`System.gc()` stalls the game for exactly as long as the collector needs to run. Pressing it to
make a memory number look tidier is the opposite of what a performance mod is for, and it would
not make the reading more accurate. VortexMC reads `Runtime` and leaves the collector alone. If
used heap is sitting near the maximum, the fix is a larger `-Xmx` in the launcher.

## Why it does not claim to speed up your internet

The Network tab shows latency (the round-trip time the server reports for you), packet rates, and
connection state. Those describe the path to the server you are playing on. They are not a
bandwidth measurement, and no client-side mod changes them.

---

## Project layout

Two source sets, deliberately separated:

```
src/main/java     common: pure JDK, no Minecraft, no Fabric, no Gson
src/client/java   client-only: Minecraft integration and the dashboard GUI
src/test/java     unit tests for the common source set
```

`src/main/java` is the whole of the logic that does not need a game: the JSON layer, the profile
model, the configuration, the frame-time statistics, the trend analysis and the recommendation
rules. It has **zero dependencies beyond the JDK**, which is why it can be unit-tested and why it
compiles anywhere.

`src/client/java` is the thin layer that talks to Minecraft: reading and writing `GameOptions`,
the HUD element that samples frames, the overlay, and the dashboard. Everything there is
defensive, because every value it reads can legitimately be unavailable — no world, no server, no
player list, a connection that has not finished handshaking.

VortexMC ships its own small JSON layer (`com.tech65pro.vortexmc.json`) rather than using the Gson
that Minecraft bundles. That is what keeps `src/main/java` dependency-free.

### Notable classes

| Class | Responsibility |
|---|---|
| `core/FrameTimeStats` | Rolling frame-time window: average, median, percentiles, 1% and 0.1% lows. Allocation-free after construction. |
| `core/MetricSeries` | Fixed-size ring buffer of timestamped samples, with slope and half-over-half comparison. |
| `core/PerformanceAnalyzer` | Compares two windows. Refuses to call a spike an improvement. |
| `core/RecommendationEngine` | The Smart Auto suggestion rules. Pure functions, fully unit-tested. |
| `core/Recommendation` | A proposal. Carries a reason, never a promised frame rate. |
| `config/OptimizationProfile` | A profile. Any unset field means "do not touch this option". |
| `config/VortexConfig` | Everything persisted, with a tolerant loader. |
| `client/SettingMapper` | The only class that reads or writes Minecraft options. |
| `client/SmartAutoController` | The Smart Auto state machine. |
| `client/ConfigStore` | Atomic configuration writes. |
| `client/CompatibilityDetector` | Detects Sodium / Lithium / FerriteCore and friends by id. |
| `client/screen/DashboardScreen` | The dashboard shell, tab strip and scrolling. |
| `client/screen/Ui` | Shared drawing helpers: panels, rows, bars, sparklines, word wrap. |

### Compatibility with other optimisation mods

VortexMC detects Sodium, Embeddium, Rubidium, Oculus, Iris, Indium, Lithium, FerriteCore,
Starlight, Krypton and a few others **by mod id only**, through `FabricLoader.isModLoaded`. It
never reads, reflects into or copies another mod's code, and it never disables anything. The
dashboard shows what it found and explains, honestly, which of VortexMC's changes are likely to
have an effect given what is installed — for example, that a renderer replacement already handles
chunk building, so VortexMC's render-distance change is the part that still applies.

Because VortexMC only touches options the game itself owns, it composes with those mods rather
than fighting them. It declares just three Fabric API modules so the dependency graph stays small.

---

## Configuration

`config/vortexmc.json`, written atomically (temp file, then move). A file that cannot be parsed is
moved aside to `vortexmc.json.broken` and defaults are used — a corrupt config never stops the game
from starting. Unknown keys and unrecognised enum values are ignored, so a config written by a
newer build does not break an older one.

Every numeric setting is clamped on load, so a hand-edited value cannot lock the game into an
unusable state.

---

## What was and was not verified

This section is deliberately blunt. The five categories below are kept separate on purpose,
because they are not the same claim and none of them is upgraded to another.

| Category | Status | Evidence |
|---|---|---|
| **Source code** | **Verified** | 48 source files compile with zero errors and zero `-Xlint:all` warnings against the real Minecraft 1.21.11 and real Fabric API. No placeholder, no `TODO`, no stub. |
| **Compilation** | **Verified** | `tools/real-build.sh` runs the whole chain against real jars and reproduces a byte-identical 125,519-byte jar. This is **not** `./gradlew build` — see below. |
| **Unit tests** | **Verified** | 47 tests, 47 passed, 0 failed (`bash tools/run-tests.sh`). Pure-JDK common layer only; the client layer has no unit tests because it needs a running game. |
| **JAR metadata** | **Verified** | 62 class files, class-file version 61, 0 non-intermediary `net/minecraft/` references, both entrypoints present, `depends` on exactly the three Fabric API modules used, `environment: client`, `java >= 21`. |
| **Minecraft runtime** | **NOT verified** | Minecraft has never been launched here. No dashboard has been seen, no keybind pressed, no vanilla option written by the real game. Nothing below is evidence of runtime behaviour. |

The build that produced the shipped jar is a **manual reproduction of Loom's steps**, not Loom
itself. That distinction is the whole point of this section.

### Verified here

* **Compiled against the real Minecraft 1.21.11 and the real Fabric API.** Not stubs. The
  vanilla obfuscated client jar, the Yarn-named merged jar that Loom puts on the compile
  classpath, Yarn `1.21.11+build.3`, intermediary `1.21.11`, Tiny Remapper `0.12.2`,
  fabric-loader `0.18.4`, the `fabric-api` modules `fabric-api-base`, `fabric-key-binding-api-v1`,
  `fabric-lifecycle-events-v1`, `fabric-rendering-v1`, ModMenu `17.0.0-beta.1` and every
  Minecraft library (brigadier, guava, gson, joml, fastutil, netty, log4j, datafixerupper,
  authlib, oshi, lwjgl) were all obtained and used. 48 source files compile with **zero errors
  and zero warnings** against them.
* **The jar in `build/libs/` is remapped to intermediary and is installable.** It was remapped
  with Tiny Remapper 0.12.2, the same library Loom uses. Verified by dumping the constant pool
  of all 62 classes:
  * 0 references to `net/minecraft/` that are not intermediary
  * 30 distinct intermediary classes referenced, and **all 30 exist** in intermediary 1.21.11
  * 72 intermediary methods and 17 intermediary fields referenced, **none** invented
  * 11 `net/fabricmc/` references, correctly left un-remapped
  * class file version 61 throughout, which the Java 21 runtime Minecraft 1.21.11 requires loads
    without complaint
* **`fabric.mod.json` is correct and expanded.** `"version": "1.0.0"`, `"minecraft": "~1.21.11"`,
  `"fabricloader": ">=0.18.4"`, `"java": ">=21"`, `"environment": "client"`, both entrypoints
  declared, and `depends` on the three Fabric API modules actually used. No placeholder survives.
* **The build is reproducible.** `bash tools/real-build.sh` performs the whole chain from
  scratch — remap the Fabric API jars into Yarn names, re-version the real jars, compile, remap
  to intermediary, package — and produces a byte-identical jar.
* **47 unit tests pass, 0 fail.** The common source set has no Minecraft dependency, so it is
  tested directly. The tests were executed through a small reflective runner
  (`bash tools/run-tests.sh`) because JUnit itself could not be downloaded; the test sources are
  ordinary JUnit 5 and run under `gradle test` normally.
* **Every build coordinate was checked against upstream**, and four were wrong in ways that
  would each have failed `./gradlew build` immediately. See the table under *Building from
  source*.
* **The 1.0.1 startup crash is fixed and the fix is proven in the jar.** `KeyBinding.Category`
  is a record whose factory throws on an equal `Identifier`; the shipped jar contains exactly
  **one** `Category.create` call site, in `VortexKeys`'s static initialiser, shared by both
  keybinds. See *The startup crash fixed in 1.0.1*.

### Bugs the real compile found that the stubs had hidden

These are worth recording, because they are exactly what a stub-based build cannot catch. Each
was verified against the real class file and then fixed properly rather than worked around:

| # | Real 1.21.11 API | What the stub had claimed |
|---|---|---|
| 1 | `Identifier(String,String)` is **private**; the factory is `Identifier.of(...)` | a public two-argument constructor |
| 2 | `MinecraftClient.isConnectedToServer()` is **private** | a public method |
| 3 | `ServerInfo.version` is a **`Text`**, not a `String` | a `String` field |
| 4 | `GameOptions.getInactivityFpsLimit()` is `SimpleOption<InactivityFpsLimit>`, an **enum** (`MINIMIZED`/`AFK`) — not an FPS number at all | `SimpleOption<Integer>` |
| 5 | `KeyBinding.Category` is a **record**; `create(String)` is **private**, `create(Identifier)` is public | a class with a public `create(String)` |
| 6 | `SliderWidget.setValue(double)` is **protected** | public |
| 7 | `Screen.addDrawableChild(T)` is **protected** | public |

Item 4 changed behaviour, not just a call: the setting is no longer an FPS slider but a two-way
cycle, because that is what the underlying option actually is. The profile field, the
configuration key and the GUI control were all updated to match, and the translation key was
corrected from `key.categories.vortexmc` to `key.category.vortexmc.main`, which is what
`Identifier.toTranslationKey` derives from the category identifier.

### Why the mappings are pinned to build.3 and not build.6

`gradle.properties` names Yarn `1.21.11+build.3`, not the `build.6` that heads Yarn's 1.21.11
branch. That is a deliberate, evidenced decision, not an oversight.

Yarn's 1.21.11 branch publishes only **Enigma source** mappings (`mappings/` directories of
`.mapping` files). The `mappings.tiny` that Loom consumes is generated by Yarn's own Gradle
build, so no build.6 tiny file exists to download. Build.6 was therefore converted from its
Enigma sources and diffed against build.3 on **exactly the 119 intermediary elements this mod
references** (30 classes, 72 methods, 17 fields). Result: **116 are identical. Three are not**,
and all three are the same class of change — Mojang turned option enums into Codec-registry
classes between the two builds and the enum constants vanished:

| Element | Owner | build.3 | build.6 |
|---|---|---|---|
| `field_18164` | `class_4063` `CloudRenderMode` | `FANCY` | constant removed |
| `field_18197` | `class_4066` `ParticlesMode` | `ALL` | constant removed |
| `field_52743` | `class_9927` `InactivityFpsLimit` | `MINIMIZED` | constant removed |

`SettingMapper.java` reads all three, so this mod does not compile against build.6 without
source changes. Claiming `yarn_mappings=1.21.11+build.6` would be claiming a build this mod was
never tested against.

### What a static review of the runtime paths found

Minecraft could not be launched here, so the feature list was audited by reading the code
instead. Three real defects were found and fixed. They are recorded because none of them would
have been caught by compiling, and two of them would have made the headline feature unusable.

| # | Defect | Consequence | Fix |
|---|---|---|---|
| 1 | `SmartAutoController.approve()` never cleared `pending`, and the dashboard had no button that called it at all | Smart Auto could propose a change and the player had **no way to accept it**. Worse, had a button existed, approving twice would have overwritten the baseline snapshot mid-measurement and silently corrupted the before/after comparison | Added an `inflight` field that `approve()` moves the proposal into, so `pending` — which is what the Approve button is shown for — is empty the moment approval happens. Wired real **Approve and measure** / **Decline** buttons into the Overview tab, with `approvalRowHeight()` keeping `build()` and `render()` in agreement so the buttons cannot overlap the panel |
| 2 | `gradlew.bat` was committed with LF-only line endings | Gradle ships that script with CRLF. `cmd.exe` mis-parses LF-only `.bat` files around `goto` labels and parenthesised blocks, so `gradlew.bat clean build` could fail or behave erratically on Windows for reasons unrelated to the project | Converted to CRLF and added a `.gitattributes` pinning `*.bat` to `eol=crlf` so it cannot regress |
| 3 | `build.gradle` declared no repository that hosts ModMenu | `com.terraformersmc:modmenu` is published to `maven.terraformersmc.com`, not to Fabric's maven or Maven Central. The optional ModMenu integration could never have resolved | Added the TerraformersMC maven repository |

Also corrected during the audit: `modmenu_version` had been changed to `17.0.0-beta.1` on the
grounds that it was the only artifact obtainable here. That was the wrong reason to pick a
version — `17.0.1` is verified (via the GitHub API) to be a non-prerelease release sitting at
the head of ModMenu's own 1.21.11 branch, so that is what `gradle.properties` now names, with
the beta recorded in *Not verified here* as what the offline compile actually used.

### The startup crash fixed in 1.0.1

Version 1.0.0 crashed the client during initialization with:

```
java.lang.IllegalArgumentException: Category 'vortexmc:main' is already registered.
    at VortexKeys.register(VortexKeys.java:45)
    at VortexClient.onInitializeClient(VortexClient.java:29)
```

The cause is confirmed against the real bytecode, not inferred. `KeyBinding.Category` (Yarn
`class_304$class_11900`) is a **record**, so two instances holding the same `Identifier` are
`equals()`. Its factory keeps a `List<Category>` and checks it:

```
 13: invokeinterface List.contains(Object)   // equal id -> already there
 18: ifeq 45                                  // not present: add it
 21: new IllegalArgumentException
 28: ldc "Category '%s' is already registered."
 ...athrow
```

`register()` called `Category.create(CATEGORY_ID)` once per keybinding, so the second call
found an equal category and threw.

The fix creates the category **once**, as a `static final` field, and both keybinds share it:

```java
public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(CATEGORY_ID);
...
openDashboard = ... new KeyBinding("key.vortexmc.open_dashboard", GLFW_KEY_H, CATEGORY);
toggleOverlay = ... new KeyBinding("key.vortexmc.toggle_overlay", GLFW_KEY_J, CATEGORY);
```

Verified in the built jar by disassembling all 62 classes: `Category.create` (intermediary
`class_304$class_11900.method_74698`) has **exactly one call site in the entire jar**, at offset
13 of `VortexKeys`'s static initialiser, and `register()` contains none. H = 72 and J = 74 are
unchanged.

The rest of the startup path was audited for the same class of fault. Every other constructor
only stores its argument and defers touching game state, `ConfigStore.load()` catches
`IOException` and `RuntimeException` and falls back to defaults, and every accessor null-guards.
No second eager registration exists.

### Not verified here

* **Minecraft has never been launched and the mod has never been run.** No dashboard has been
  seen on screen, no keybind has been pressed, no option has been written by the real game.
  Nothing here is evidence of correct runtime behaviour.
* **`./gradlew build` has never run.** `services.gradle.org`, `maven.fabricmc.net`,
  `repo1.maven.org` and every Mojang host are TLS-blocked in this environment, so Gradle, Loom
  and Maven cannot be reached. What is in `build/libs/` was produced by `tools/real-build.sh`
  reproducing Loom's steps by hand with Loom's own libraries.
* **The compile used fabric-loader 0.18.4 and ModMenu 17.0.0-beta.1**, because those are the
  versions obtainable here. `gradle.properties` names ModMenu **17.0.1**, which is *not* what
  was compiled against, and that difference is deliberate: 17.0.1 is the real release for
  1.21.11 (verified via the GitHub API to be a non-prerelease release sitting at the head of
  ModMenu's 1.21.11 branch), whereas 17.0.0-beta.1 is merely the only artifact this sandbox
  could fetch. Both target 1.21.11 and the one API used,
  `ModMenuApi.getModConfigScreenFactory()`, is unchanged between them. Resolving 17.0.1 from
  `maven.terraformersmc.com` was **not** tested.
* **The Fabric API modules came from a committed Gradle cache**, so they are a real published
  set, but not necessarily the exact `0.141.6+1.21.11` release that `gradle.properties` names.
  What was compiled against was `fabric-api-base 1.0.5+4ebb5c083e`,
  `fabric-key-binding-api-v1 1.1.7+4fc5413f3e`, `fabric-lifecycle-events-v1 2.6.15+4ebb5c083e`
  and `fabric-rendering-v1 16.2.10+0290ad933e`. The three APIs used
  (`KeyBindingHelper.registerKeyBinding`, `ClientTickEvents`, `HudElementRegistry.addLast`) are
  long-stable, but resolving the exact `0.141.6+1.21.11` release coordinate was **not** tested
  here, because `maven.fabricmc.net` is unreachable from this environment.
* **`fabric-rendering-v1`'s `HudElement.render` takes `RenderTickCounter` in Yarn build.3**, and
  the code deliberately never names that type, so it is immune to the rename. Worth knowing if
  you ever upgrade the mappings.

### What remains necessary

On a machine with network access, from the repository root:

```bash
./gradlew build
```

Then check the jar the same way this one was checked, and launch:

```bash
./gradlew runClient
```

In this order: the dashboard opens on `H`; the FPS Monitor tab shows a rising sample count;
changing render distance in the Graphics tab actually changes it in the vanilla video screen;
the Inactivity limit control cycles between MINIMIZED and AFK and the vanilla video screen
agrees; Smart Auto proposes something sensible and its measured verdict matches what you see
in the F3 graph.

---

## Licence

MIT. See `LICENSE`.
