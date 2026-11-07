vortexmc-1.0.1.jar
=================

Built by tools/real-build.sh from the sources in this repository.

Fixes the 1.0.0 startup crash
-----------------------------
1.0.0 threw IllegalArgumentException: Category 'vortexmc:main' is already registered.
during client initialization. KeyBinding.Category is a record, so two instances holding the
same Identifier are equal, and its factory keeps a List<Category> and throws when it finds
an equal entry. VortexKeys.register() called Category.create(CATEGORY_ID) once per keybind,
so the second call threw.

This jar creates the category exactly once, as a static final field, and both keybinds share
it. Verified by disassembling all 62 classes: Category.create has exactly one call site in
the whole jar, in VortexKeys' static initialiser. Keybinds are unchanged: H = 72 dashboard,
J = 74 overlay.

What this jar is
----------------
* 62 class files, 125,878 bytes.
* Compiled against the REAL Minecraft 1.21.11 client and the REAL Fabric API modules
  (fabric-api-base, fabric-key-binding-api-v1, fabric-lifecycle-events-v1,
  fabric-rendering-v1), fabric-loader 0.18.4 and ModMenu 17.0.0-beta.1.
* Remapped to the intermediary namespace with Tiny Remapper 0.12.2, the same library
  Fabric Loom uses. Verified: 0 non-intermediary net/minecraft references, class file
  version 61, both entrypoints present.
* fabric.mod.json: version 1.0.1, minecraft ~1.21.11, fabricloader >=0.18.4, java >=21,
  environment client.

What this jar is NOT
--------------------
It has never been run. Minecraft has not been launched in the environment that produced it,
so the crash fix is verified by bytecode inspection, not by a successful game start. Being
correctly remapped and correctly linked means the classes resolve and link; it does not
prove the fix works at runtime.

Install
-------
Drop this jar into .minecraft/mods/ alongside Fabric Loader 0.18.4+ for Minecraft 1.21.11
and the Fabric API modules named above. ModMenu is optional. Requires Java 21+.

On Windows the supported build is:

    gradlew.bat clean build

See README.md, section "What was and was not verified", for the full list.
