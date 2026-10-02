#!/usr/bin/env bash
# ForgePlaytime direct-javac build.
# The Gradle daemon cannot run in this sandbox (loopback TCP is blocked),
# so this compiles with javac directly against the paper-deps jars,
# exactly like ~/workspace/bin/mb-build does for mythbound.
set -euo pipefail

ROOT="$HOME/workspace/forge-playtime"
DEPS="$HOME/workspace/.toolchains/paper-deps"
VERSION="1.0.0"
JAVAC="$HOME/workspace/.toolchains/jdk-25.0.4.1+1/bin/javac"
JAR="$HOME/workspace/.toolchains/jdk-25.0.4.1+1/bin/jar"
CP=$(ls "$DEPS"/*.jar | tr '\n' ':')
OUT="$ROOT/build"

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/stage"

find "$ROOT/src/main/java" -name '*.java' > "$OUT/sources.txt"
echo "==> javac (-Werror -Xlint:deprecation,unchecked)"
$JAVAC -Werror -Xlint:deprecation -Xlint:unchecked -parameters -d "$OUT/classes" -cp "$CP" @"$OUT/sources.txt"

echo "==> jar"
cp -r "$OUT/classes"/. "$OUT/stage"/
cp "$ROOT/src/main/resources/plugin.yml" "$OUT/stage/plugin.yml"
cp "$ROOT/src/main/resources/config.yml" "$OUT/stage/config.yml"
( cd "$OUT/stage" && $JAR --create --file "$ROOT/ForgePlaytime-$VERSION.jar" . )

echo
echo "built $ROOT/ForgePlaytime-$VERSION.jar"
"$HOME/workspace/.toolchains/jdk-25.0.4.1+1/bin/jar" tf "$ROOT/ForgePlaytime-$VERSION.jar"
