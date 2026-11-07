#!/usr/bin/env bash
#
# Run the VortexMC unit tests without a network connection.
#
# src/test/java only exercises the config/ and core/ layers, which are pure JDK code with no
# Minecraft or Fabric dependency, so the tests can be compiled and executed with nothing but
# a JDK and the tiny JUnit stand-ins in tools/junit-api. On a machine with network access the
# equivalent command is simply `./gradlew test`, which uses the real JUnit 5.
#
# Usage:  bash tools/run-tests.sh
# Exit:   0 if every test passes, 1 otherwise.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# --- locate a JDK 17 or newer -------------------------------------------------------------
JAVAC="${JAVAC:-}"
JAVA="${JAVA:-}"
if [ -z "$JAVAC" ] || [ -z "$JAVA" ]; then
  for candidate in /usr/lib/jvm/*/bin /opt/*/bin "$HOME"/.jdks/*/bin "$HOME"/tools/*/bin; do
    if [ -x "$candidate/javac" ]; then JAVAC="$candidate/javac"; JAVA="$candidate/java"; break; fi
  done
fi
if [ -z "$JAVAC" ]; then
  echo "error: no javac found. Set JAVAC=/path/to/javac JAVA=/path/to/java and retry." >&2
  exit 1
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "javac: $("$JAVAC" -version 2>&1)"
echo "java:  $("$JAVA" -version 2>&1 | head -1)"
echo

# --- compile the code under test ----------------------------------------------------------
# src/main/java holds the config/ and core/ layers. src/client/java is deliberately NOT
# compiled here: it needs the real Minecraft and Fabric API, which is what tools/real-build.sh
# is for.
find src/main/java -name '*.java' > "$WORK/sources.txt"
find src/test/java -name '*.java' >> "$WORK/sources.txt"
find tools/junit-api -name '*.java' >> "$WORK/sources.txt"
find tools/offline-verify -name '*.java' >> "$WORK/sources.txt"

echo "compiling $(wc -l < "$WORK/sources.txt") source files..."
"$JAVAC" -nowarn -encoding UTF-8 -d "$WORK/classes" @"$WORK/sources.txt"

# --- run ----------------------------------------------------------------------------------
echo
"$JAVA" -cp "$WORK/classes" TestRunner
