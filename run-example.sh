#!/bin/sh
set -eu
classes="${TMPDIR:-/tmp}/nonprofit-agent-classes"
rm -rf "$classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java -name '*.java')
java -cp "$classes" org.example.nonprofit.AgentLoopApplication
