#!/bin/bash
# Start RuneLite (developer mode) met de Farm Run Planner v2 plugin, via de Gradle 'run' taak.
# Inloggen met een Jagex account: zie LEESMIJ.md / https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
set -e
cd "$(dirname "$0")"

# Gebruik de meegeleverde Java 11 als die er is.
JDK="$(ls -d .tools/jdk-11*/Contents/Home 2>/dev/null | head -1)"
if [ -n "$JDK" ]; then
	export JAVA_HOME="$PWD/$JDK"
fi

# Gradle-cache buiten OneDrive houden (OneDrive blokkeert die bestanden anders).
exec ./gradlew run --no-daemon --project-cache-dir "$HOME/.cache/farm-run-planner-gradle" "$@"
