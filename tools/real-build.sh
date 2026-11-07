#!/usr/bin/env bash
#
# Build VortexMC against the REAL Minecraft 1.21.11 and REAL Fabric artifacts, then
# remap the result to intermediary so the jar is installable.
#
# Why this exists: this environment has no network route to services.gradle.org,
# maven.fabricmc.net, repo1.maven.org or any Mojang host, so `./gradlew build` cannot
# run. Everything Loom would do is reproduced here by hand, using the same libraries
# Loom itself uses:
#
#   * Tiny Remapper 0.12.2  - the remapper Loom uses, both directions
#   * Yarn 1.21.11+build.3  - the real mappings, in Loom's own cache format
#   * the real merged, Yarn-named Minecraft jar that Loom puts on the compile classpath
#
# The Minecraft client jar and the Fabric artifacts are not on any reachable Maven host,
# so they are taken from GitHub repositories that commit them as ordinary files:
#
#   Scratch2033Alt/minecraft-jars-mirror  -> the vanilla obfuscated client jar
#   barmith/TheHouse                      -> a committed Gradle cache holding the
#                                            Yarn-named merged jar, the yarn and
#                                            intermediary mappings, Tiny Remapper,
#                                            fabric-loader, the fabric-api modules,
#                                            ModMenu, and every Minecraft library
#
# NOT part of the mod. Lives under tools/ and is never shipped in the jar.
#
# Usage:  bash tools/real-build.sh
# Env:    MB=/path/to/build-dir   (default: /home/user/mcbuild)

set -euo pipefail

MB="${MB:-/home/user/mcbuild}"
REPO="$(cd "$(dirname "$0")/.." && pwd)"
# Derive the artifact name from gradle.properties so the jar name can never drift from the
# declared version.
VERSION="$(sed -n 's/^mod_version=//p' "$REPO/gradle.properties")"
JARNAME="vortexmc-${VERSION}.jar"
JDK="$MB/jdk17rt"
J="$JDK/bin"

LIBS="$MB/libs"
V61="$MB/v61"
NAMED="$MB/named-api"
WORK="$MB/work"
OUT="$MB/final"
SRC="$REPO/src"

ASM="$LIBS/asm-9.9.jar:$LIBS/asm-tree-9.9.jar:$LIBS/asm-commons-9.9.jar:$LIBS/asm-analysis-9.9.jar"
TR="$LIBS/tiny-remapper-0.12.2.jar:$LIBS/tiny-mappings-parser-0.3.0+build.17.jar:$LIBS/mapping-io-0.8.0.jar:$ASM:$LIBS/asm-util-9.9.jar"

MC_JAR="$LIBS/minecraft-merged-1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.3-v2.jar"
MAPPINGS="$WORK/mappings.tiny"

say() { printf '\n=== %s ===\n' "$*"; }

[ -x "$J/javac" ] || { echo "error: no JDK at $JDK. Run: bash tools/provision.sh" >&2; exit 1; }
[ -f "$MC_JAR" ]  || { echo "error: $MC_JAR missing. See the provenance notes at the top of this script." >&2; exit 1; }

mkdir -p "$V61" "$NAMED" "$WORK" "$OUT"

# ---------------------------------------------------------------------------
say "1/6  building the helper tools (ASM + Tiny Remapper drivers)"
[ -f "$WORK/classes/Remap.class" ] || {
	mkdir -p "$WORK/classes"
	"$J/javac" -nowarn -cp "$TR" -d "$WORK/classes" "$REPO/tools/real-build/Remap.java"
}
[ -f "$WORK/classes/Downgrade.class" ] || {
	"$J/javac" -nowarn -cp "$ASM" -d "$WORK/classes" "$REPO/tools/real-build/Downgrade.java"
}

# ---------------------------------------------------------------------------
say "2/6  extracting the Yarn mappings"
# The yarn jar holds mappings/mappings.tiny in the form: tiny 2 0 intermediary named
[ -f "$MAPPINGS" ] || {
	rm -rf "$WORK/yarn" && mkdir -p "$WORK/yarn"
	(cd "$WORK/yarn" && "$J/jar" xf "$LIBS/yarn-1.21.11+build.3-v2.jar" mappings)
	mv "$WORK/yarn/mappings/mappings.tiny" "$MAPPINGS"
	rm -rf "$WORK/yarn"
}
head -1 "$MAPPINGS"

# ---------------------------------------------------------------------------
say "3/6  re-versioning the real jars so a JDK 17 javac can read them"
# Mojang ships 1.21.11 as Java 21 bytecode (version 65); javac 17 stops at 61. ASM
# parses either, so only the version word is lowered. Names, descriptors and frames
# are untouched, so what we compile against is the real API.
[ -f "$V61/minecraft-named.jar" ] || \
	"$J/java" -Xmx3g -cp "$ASM:$WORK/classes" Downgrade "$MC_JAR" "$V61/minecraft-named.jar"

# The published fabric-api jars reference Minecraft in intermediary; Loom remaps them
# into the project's mappings for compilation. Same thing here, then re-version.
for m in fabric-api-base-1.0.5+4ebb5c083e \
         fabric-key-binding-api-v1-1.1.7+4fc5413f3e \
         fabric-lifecycle-events-v1-2.6.15+4ebb5c083e \
         fabric-rendering-v1-16.2.10+0290ad933e; do
	if [ ! -f "$V61/$m.jar" ]; then
		"$J/java" -Xmx2g -cp "$TR:$WORK/classes" Remap intermediary named "$MAPPINGS" \
			"$LIBS/$m.jar" "$NAMED/$m.jar" "$V61/minecraft-named.jar"
		"$J/java" -cp "$ASM:$WORK/classes" Downgrade "$NAMED/$m.jar" "$V61/$m.jar"
	fi
done

# ModMenu is optional and also arrives in intermediary.
if [ ! -f "$V61/modmenu.jar" ]; then
	"$J/java" -Xmx2g -cp "$TR:$WORK/classes" Remap intermediary named "$MAPPINGS" \
		"$LIBS/modmenu-17.0.0-beta.1.jar" "$NAMED/modmenu.jar" "$V61/minecraft-named.jar"
	"$J/java" -cp "$ASM:$WORK/classes" Downgrade "$NAMED/modmenu.jar" "$V61/modmenu.jar"
fi

# ---------------------------------------------------------------------------
say "4/6  compiling against the real Minecraft / Fabric API"
CP="$V61/minecraft-named.jar:$V61/fabric-api-base-1.0.5+4ebb5c083e.jar"
CP="$CP:$V61/fabric-key-binding-api-v1-1.1.7+4fc5413f3e.jar"
CP="$CP:$V61/fabric-lifecycle-events-v1-2.6.15+4ebb5c083e.jar"
CP="$CP:$V61/fabric-rendering-v1-16.2.10+0290ad933e.jar"
CP="$CP:$V61/modmenu.jar:$LIBS/fabric-loader-0.18.4.jar"
for j in "$LIBS"/mclibs/*.jar; do CP="$CP:$j"; done

rm -rf "$WORK/vortex" && mkdir -p "$WORK/vortex"
find "$SRC/main/java" "$SRC/client/java" -name '*.java' > "$WORK/sources.txt"
"$J/javac" -nowarn -encoding UTF-8 -source 17 -target 17 -Xlint:-options \
	-cp "$CP" -d "$WORK/vortex" @"$WORK/sources.txt"
echo "compiled $(find "$WORK/vortex" -name '*.class' | wc -l) classes"

# ---------------------------------------------------------------------------
say "5/6  remapping to intermediary, as Loom does for a release jar"
rm -rf "$WORK/pkg" && mkdir -p "$WORK/pkg"
cp -r "$WORK/vortex/." "$WORK/pkg/"
(cd "$WORK/pkg" && "$J/jar" cf "$WORK/classes-only.jar" com)

"$J/java" -Xmx3g -cp "$TR:$WORK/classes" Remap named intermediary "$MAPPINGS" \
	"$WORK/classes-only.jar" "$WORK/vortex-intermediary.jar" "$V61/minecraft-named.jar"

# ---------------------------------------------------------------------------
say "6/6  packaging"
rm -rf "$OUT" && mkdir -p "$OUT/assets/vortexmc/lang"
(cd "$OUT" && "$J/jar" xf "$WORK/vortex-intermediary.jar")
cp "$SRC/main/resources/assets/vortexmc/lang/en_us.json" "$OUT/assets/vortexmc/lang/"

python3 - "$REPO" "$OUT" <<'PY'
import io, json, re, sys
repo, out = sys.argv[1], sys.argv[2]
props = {}
for line in io.open(repo + '/gradle.properties', encoding='utf-8'):
    line = line.strip()
    if not line or line.startswith('#') or '=' not in line: continue
    k, v = line.split('=', 1); props[k.strip()] = v.strip()
props.setdefault('version', props.get('mod_version', '1.0.0'))
props.setdefault('minecraft_version', '1.21.11')
src = io.open(repo + '/src/main/resources/fabric.mod.json', encoding='utf-8').read()
text = re.sub(r'\$\{([^}]+)\}', lambda m: props[m.group(1)], src)
assert '${' not in text, 'unexpanded placeholder left in fabric.mod.json'
json.loads(text)
io.open(out + '/fabric.mod.json', 'w', encoding='utf-8').write(text)
PY

(cd "$OUT" && "$J/jar" cf "$WORK/$JARNAME" com assets fabric.mod.json)
mkdir -p "$REPO/build/libs"
cp "$WORK/$JARNAME" "$REPO/build/libs/$JARNAME"

say "done"
ls -l "$REPO/build/libs/$JARNAME"
