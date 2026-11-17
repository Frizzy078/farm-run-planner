#!/bin/bash
# Start RuneLite met de Farm Run Planner plugin.
# Inloggen met een Jagex account: zie LEESMIJ.md (eenmalig credentials laten opslaan).
# Gebruikt de RuneLite-bestanden die je RuneLite-installatie al heeft gedownload
# (~/.runelite/repository2), dus er is geen extra download nodig.
set -e
cd "$(dirname "$0")"

JAVA_HOME="$(ls -d .tools/jdk-11*/Contents/Home | head -1)"
REPO="$HOME/.runelite/repository2"

# Nieuwste RuneLite-versie die op deze Mac staat
VERSION="$(ls "$REPO"/client-*.jar | sed -E 's/.*client-(.*)\.jar/\1/' | sort -V | tail -1)"
echo "RuneLite versie: $VERSION"

CP=""
for jar in "$REPO"/*.jar; do
	name="$(basename "$jar")"
	case "$name" in
		client-*|runelite-api-*)
			[[ "$name" == *"-$VERSION"* ]] || continue ;;
	esac
	CP="$CP:$jar"
done

rm -rf build/local
mkdir -p build/local
"$JAVA_HOME/bin/javac" --release 11 -nowarn -d build/local -cp "$CP" \
	src/main/java/com/farmplanner/*.java src/test/java/com/farmplanner/FarmPlannerPluginTest.java

exec "$JAVA_HOME/bin/java" -ea -cp "build/local$CP" com.farmplanner.FarmPlannerPluginTest "$@"
