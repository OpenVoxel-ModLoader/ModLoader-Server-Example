#!/usr/bin/env bash
# Builds HungerGames.jar into dist/ (needs the dedicated server JAR: OPENVOXEL_SERVER_JAR).
set -euo pipefail
cd "$(dirname "$0")"
SERVER="${OPENVOXEL_SERVER_JAR:-}"
for c in ../ModLoader-Server/server/openvoxel-server.jar ../../../ServerTest/openvoxel-server-0.0.8-SNAPSHOT.jar; do [ -z "$SERVER" ] && [ -f "$c" ] && SERVER="$c"; done
[ -n "$SERVER" ] || { echo "Server JAR not found; set OPENVOXEL_SERVER_JAR." >&2; exit 1; }
rm -rf build && mkdir -p build dist
find src -name '*.java' > build/sources.txt
javac --release 11 -encoding UTF-8 -Xlint:-options -classpath "$SERVER" -d build @build/sources.txt
rm build/sources.txt
cp resources/plugin.yml build/
jar --create --file dist/HungerGames.jar -C build .
[ -d ../ModLoader-Server/mods ] && cp -f dist/HungerGames.jar ../ModLoader-Server/mods/
echo "HungerGames.jar -> dist (and ../ModLoader-Server/mods if present)"
