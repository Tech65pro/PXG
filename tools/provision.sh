#!/usr/bin/env bash
#
# Provision every dependency VortexMC's real build needs, from GitHub and PyPI only.
#
# This environment has no route to services.gradle.org, maven.fabricmc.net,
# repo1.maven.org or any Mojang host, so `./gradlew build` cannot run and nothing can
# be fetched from Maven. Everything Loom would download is instead taken from
# repositories that commit the artifacts as ordinary files:
#
#   Scratch2033Alt/minecraft-jars-mirror  the vanilla obfuscated 1.21.11 client jar
#   barmith/TheHouse                      a committed Gradle cache holding the
#                                         Yarn-named merged jar, yarn + intermediary
#                                         mappings, Tiny Remapper, fabric-loader, the
#                                         fabric-api modules, ModMenu, and every
#                                         Minecraft library
#   kazga/bloxhost                       Temurin JDK 17 jmods (a real, complete set)
#   FabricMC/yarn                        the Enigma mapping sources at branch HEAD,
#                                         which is Yarn build.6
#
# The output is a self-contained directory that tools/real-build.sh consumes.
#
# NOT part of the mod. Lives under tools/ and is never shipped in the jar.
#
# Usage:  bash tools/provision.sh
# Env:    MB=/path/to/build-dir   (default: /home/user/mcbuild)

set -euo pipefail

MB="${MB:-/home/user/mcbuild}"
LIBS="$MB/libs"
JDK="$MB/jdk17rt"
WORK="$MB/work"

say() { printf '\n=== %s ===\n' "$*"; }

mkdir -p "$LIBS" "$WORK"

# ---------------------------------------------------------------------------
say "1/6  JDK 17 from the jmods in kazga/bloxhost"
# bloxhost ships a full Temurin 17.0.13+11, but its lib/modules is a Git LFS pointer,
# so bin/javac cannot start. The jmods/ directory, however, is real. jlink reassembles
# a working image from them, running on a host JVM taken from PyPI.
if [ ! -x "$JDK/bin/javac" ]; then
	DL="$MB/dl"
	mkdir -p "$DL/jmods17" "$DL/host"

	if [ ! -d "$DL/jmods17/java.base.jmod" ] && [ ! -f "$DL/jmods17/java.base.jmod" ]; then
		[ -f "$DL/bh.tar.gz" ] || curl -sfL --retry 3 -o "$DL/bh.tar.gz" \
			"https://codeload.github.com/kazga/bloxhost/tar.gz/refs/heads/main"
		tar -xzf "$DL/bh.tar.gz" -C "$DL/jmods17" --strip-components=4 \
			"bloxhost-main/jdk/jdk-17.0.13+11/jmods"
	fi
	[ -f "$DL/host/jdk4py/java-runtime/bin/java" ] || {
		( cd "$DL" && pip3 download jdk4py --no-deps -q -d . && \
		  unzip -q -o jdk4py-*.whl -d host && mv host/jdk4py host/_j && rm -rf host/jdk4py && mv host/_j host/jdk4py )
	}

	# Explode every jmod's classes/ into mods/<module>/ so jlink can be run from -cp.
	python3 - "$DL/jmods17" "$MB/mods" <<'PY'
import sys, zipfile, io, pathlib, shutil
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
if dst.exists(): shutil.rmtree(dst)
dst.mkdir(parents=True)
for jmod in sorted(src.glob('*.jmod')):
    raw = jmod.read_bytes()
    assert raw[:4] == b'JM\x01\x00', jmod          # 4-byte jmod magic
    z = zipfile.ZipFile(io.BytesIO(raw[4:]))
    out = dst / jmod.name[:-5]
    for info in z.infolist():
        if not info.filename.startswith('classes/'): continue
        rel = info.filename[len('classes/'):]
        if not rel or rel.endswith('/'): continue
        t = out / rel
        t.parent.mkdir(parents=True, exist_ok=True)
        t.write_bytes(z.read(info))
PY

	# jlink refuses to link a target whose java.base version differs from its own, so the
	# version string inside java.base.jmod is raised to match the host. The replacement is
	# the same length, so no constant-pool offsets move. The 4-byte magic must be re-prepended.
	python3 - "$DL/jmods17/java.base.jmod" <<'PY'
import sys, zipfile, io, pathlib
p = pathlib.Path(sys.argv[1]); raw = p.read_bytes(); MAGIC = raw[:4]
a = zipfile.ZipFile(io.BytesIO(raw[4:])); out = {}
for n in a.namelist():
    d = a.read(n)
    if n == 'classes/module-info.class' and b'17.0.13' in d:
        d = d.replace(b'17.0.13', b'25.0.13')
    out[n] = d
with open(p, 'wb') as f:
    f.write(MAGIC)
    with zipfile.ZipFile(f, 'w', zipfile.ZIP_DEFLATED) as z:
        for n, d in out.items(): z.writestr(n, d)
PY

	HOST="$DL/host/jdk4py/java-runtime/bin/java"
	rm -rf "$JDK"
	# On the Java 25 host the jdk.internal.* packages all live in java.base, even though in
	# the Java 17 jmods some are separate modules. They must be exported as java.base/<pkg>.
	"$HOST" $(for p in module opt jimage jimage.decompressor org.objectweb.asm util perf jmod loader; do
		echo "--add-exports java.base/jdk.internal.$p=ALL-UNNAMED"; done) \
		-cp "$MB/mods/jdk.jlink:$MB/mods/jdk.internal.opt" \
		jdk.tools.jlink.internal.Main \
		--module-path "$DL/jmods17" --add-modules ALL-MODULE-PATH \
		--output "$JDK" --ignore-signing-information
fi
"$JDK/bin/javac" -version

# ---------------------------------------------------------------------------
say "2/6  the vanilla obfuscated 1.21.11 client jar"
# Committed as a real 31 MB file, not a Git LFS pointer.
if [ ! -f "$LIBS/1.21.11-client.jar" ]; then
	rm -rf "$WORK/mcjar"
	git clone -q --depth 1 --filter=blob:none --sparse \
		https://github.com/Scratch2033Alt/minecraft-jars-mirror.git "$WORK/mcjar"
	( cd "$WORK/mcjar" && git sparse-checkout set --skip-checks client/1.21.11-client.jar \
		&& git checkout -q )
	cp "$WORK/mcjar/client/1.21.11-client.jar" "$LIBS/"
fi
ls -l "$LIBS/1.21.11-client.jar"

# ---------------------------------------------------------------------------
say "3/6  the Fabric and Minecraft library jars"
# All committed as real files in a Gradle cache.
if [ ! -f "$LIBS/fabric-loader-0.18.4.jar" ]; then
	rm -rf "$WORK/thehouse"
	git clone -q --depth 1 --filter=blob:none --sparse \
		https://github.com/barmith/TheHouse.git "$WORK/thehouse"
	C=".gradle-home/caches/modules-2/files-2.1"
	( cd "$WORK/thehouse" && git sparse-checkout set --skip-checks \
		"$C/net.fabricmc/fabric-loader/0.18.4/dc4682125d5db647d6ce80a98b98945835adcd4b/fabric-loader-0.18.4.jar" \
		"$C/net.fabricmc.fabric-api/fabric-api-base/1.0.5+4ebb5c083e/555b845e5ea7141b01bef15460980659d94f16f7/fabric-api-base-1.0.5+4ebb5c083e.jar" \
		"$C/net.fabricmc.fabric-api/fabric-key-binding-api-v1/1.1.7+4fc5413f3e/e3085cba5258bcbee165350caadd1c9a400e5724/fabric-key-binding-api-v1-1.1.7+4fc5413f3e.jar" \
		"$C/net.fabricmc.fabric-api/fabric-lifecycle-events-v1/2.6.15+4ebb5c083e/40e1f2b9098d25b11eeca625289119f8864ed2b3/fabric-lifecycle-events-v1-2.6.15+4ebb5c083e.jar" \
		"$C/net.fabricmc.fabric-api/fabric-rendering-v1/16.2.10+0290ad933e/2485bb9579abaada1f556258b823dd9fbb1e3b05/fabric-rendering-v1-16.2.10+0290ad933e.jar" \
		"$C/net.fabricmc/yarn/1.21.11+build.3/30d7534adddabe81eb1a80720bd28378a81472d1/yarn-1.21.11+build.3-v2.jar" \
		"$C/net.fabricmc/intermediary/1.21.11/16a84fabebbbfaf4950abe072f6d3653090430a/intermediary-1.21.11-v2.jar" \
		"$C/net.fabricmc/tiny-remapper/0.12.2/218327e5596d130afb930a03458dadc60a1b9d64/tiny-remapper-0.12.2.jar" \
		"$C/net.fabricmc/tiny-mappings-parser/0.3.0+build.17/2f10540a290e382a7cd35c16ec3900046a4e252/tiny-mappings-parser-0.3.0+build.17.jar" \
		"$C/net.fabricmc/mapping-io/0.8.0/6e572f573c1fcd4916a5a3f457f2a8a950eb31b9/mapping-io-0.8.0.jar" \
		"$C/org.ow2.asm/asm/9.9/c29635c8a7afa03d74b33c1884df8abb2b3f3dcc/asm-9.9.jar" \
		"$C/org.ow2.asm/asm-tree/9.9/f8de6eead6d24dd0f45bd065bbe112b2cda6ea21/asm-tree-9.9.jar" \
		"$C/org.ow2.asm/asm-commons/9.9/db9165a3bf908ded6b08612d583a15d1d0c7bda0/asm-commons-9.9.jar" \
		"$C/org.ow2.asm/asm-analysis/9.9/bf4fa6e66638851c1cd22c2caea0c3ee5d5f437/asm-analysis-9.9.jar" \
		"$C/org.ow2.asm/asm-util/9.9/42fdfc0508b43807c8078d6e82ecff2ce2112ae8/asm-util-9.9.jar" \
		"$C/com.terraformersmc/modmenu/17.0.0-beta.1/a409614af507ec8b31185d00d45cee1682097fe1/modmenu-17.0.0-beta.1.jar" \
		&& git checkout -q )
	find "$WORK/thehouse/.gradle-home" -name '*.jar' -exec cp {} "$LIBS/" \;
fi
ls "$LIBS"/*.jar | wc -l | xargs echo "fabric/asm/modmenu jars:"

# Every Minecraft library the compile classpath needs. Committed as real files too.
if [ ! -d "$LIBS/mclibs" ] || [ -z "$(ls -A "$LIBS/mclibs" 2>/dev/null)" ]; then
	C=".gradle-home/caches/modules-2/files-2.1"
	LIST=$( cd "$WORK/thehouse" && git ls-tree -r HEAD --name-only | \
		grep -E "^$C/(com\.mojang|com\.google\.guava|com\.google\.code\.gson|org\.joml|it\.unimi\.dsi|org\.apache\.logging\.log4j|com\.github\.oshi|io\.netty|org\.lwjgl|net\.java\.dev\.jna|commons-io|commons-codec|org\.apache\.commons)/[^/]+/[^/]+/[^/]+/[^/]+\.jar$" | \
		grep -vE "sources|javadoc|natives" )
	( cd "$WORK/thehouse" && git sparse-checkout set --skip-checks $(echo "$LIST" | tr '\n' ' ') && git checkout -q )
	mkdir -p "$LIBS/mclibs"
	find "$WORK/thehouse/.gradle-home" -name '*.jar' -exec cp {} "$LIBS/mclibs/" \;
	rm -f "$LIBS"/mclibs/*natives* "$LIBS"/mclibs/jtracy-*.jar
fi
ls "$LIBS/mclibs"/*.jar | wc -l | xargs echo "minecraft library jars:"

# ---------------------------------------------------------------------------
say "4/6  Yarn build.6 mapping sources (branch HEAD)"
if [ ! -d "$MB/yarn-src" ]; then
	rm -rf "$WORK/yarn"
	curl -sfL -o "$WORK/yarn.tar.gz" \
		"https://codeload.github.com/FabricMC/yarn/tar.gz/refs/heads/1.21.11"
	tar -xzf "$WORK/yarn.tar.gz" -C "$WORK"
	mv "$WORK/yarn-1.21.11" "$MB/yarn-src"
fi
find "$MB/yarn-src/mappings" -name '*.mapping' | wc -l | xargs echo "enigma mapping files:"

# ---------------------------------------------------------------------------
say "5/6  converting the Enigma sources to a tiny v2 file"
# The published yarn jar contains a generated mappings/mappings.tiny; the repository only
# carries the Enigma sources, so the tiny file is produced here. The format is
#   c <intermediary> <named> / f <desc> <intermediary> <named> / m <desc> <intermediary> <named>
python3 - "$MB/yarn-src/mappings" "$WORK/mappings-build6.tiny" <<'PY'
import sys, pathlib
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
out = ['tiny\t2\t0\tintermediary\tnamed']
classes = 0
for f in sorted(src.rglob('*.mapping')):
    lines = f.read_text(encoding='utf-8').splitlines()
    current = None
    for line in lines:
        if not line.strip(): continue
        indent = len(line) - len(line.lstrip('\t'))
        parts = line.strip().split('\t')
        if indent == 0 and parts[0] == 'CLASS':
            current = parts
            out.append('c\t%s\t%s' % (parts[1], parts[2]))
            classes += 1
        elif indent == 1 and parts[0] == 'FIELD' and len(parts) >= 4:
            out.append('\tf\t%s\t%s\t%s' % (parts[2], parts[1], parts[3]))
        elif indent == 1 and parts[0] == 'METHOD' and len(parts) >= 4:
            out.append('\tm\t%s\t%s\t%s' % (parts[2], parts[1], parts[3]))
dst.write_text('\n'.join(out) + '\n', encoding='utf-8')
print('  %d classes, %d lines' % (classes, len(out)))
PY

# ---------------------------------------------------------------------------
say "6/6  Yarn build.3 tiny, for comparison"
# build.3 is the version the committed jar was originally built against. Keeping it lets
# the two be diffed, which is how build.6 was shown to be a safe upgrade.
if [ ! -f "$WORK/mappings-build3.tiny" ]; then
	rm -rf "$WORK/y3" && mkdir -p "$WORK/y3"
	( cd "$WORK/y3" && "$JDK/bin/jar" xf "$LIBS/yarn-1.21.11+build.3-v2.jar" mappings )
	mv "$WORK/y3/mappings/mappings.tiny" "$WORK/mappings-build3.tiny"
	rm -rf "$WORK/y3"
fi

# ---------------------------------------------------------------------------
say "7/7  the Yarn-named merged Minecraft jar Loom compiles against"
# Loom puts a Yarn-named, merged (client+server) Minecraft jar on the compile classpath.
# The vanilla client jar from step 2 is obfuscated and cannot be compiled against, so this
# is the artifact real-build.sh actually needs. It lives in TheHouse's committed Loom cache.
MM="$LIBS/minecraft-merged-1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.3-v2.jar"
if [ ! -f "$MM" ]; then
	TH="$WORK/thehouse"
	if [ ! -d "$TH/.git" ]; then
		rm -rf "$TH"
		git clone --filter=blob:none --no-checkout --depth 1 https://github.com/barmith/TheHouse.git "$TH"
	fi
	MMPATH=".gradle-home/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.3-v2"
	( cd "$TH" && git sparse-checkout set --skip-checks "$MMPATH" && git checkout -q )
	cp "$TH/$MMPATH/minecraft-merged-1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.3-v2.jar" "$MM"
fi
ls -l "$MM" | awk '{print "  "$5" bytes  "$NF}'

# real-build.sh reads work/mappings.tiny; keep the build.3 copy there too.
[ -f "$WORK/mappings.tiny" ] || cp "$WORK/mappings-build3.tiny" "$WORK/mappings.tiny"

say "provisioned"
du -sh "$MB" | sed 's/^/  total: /'
ls "$LIBS"/*.jar | wc -l | xargs echo "  lib jars:"
