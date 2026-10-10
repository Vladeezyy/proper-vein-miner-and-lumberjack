#!/usr/bin/env bash
# Runs the self-test (SelfTest.java) on a real dedicated server of every version and loader, or of the ones given:
#   tools/selftest.sh                 # all
#   tools/selftest.sh 1.21.11-fabric  # one
# Needs the Minecraft EULA accepted: this script writes eula=true into run/<version>/ (the user accepted it).
# Gradle has to run on Java 25 (Loom 1.18): set JAVA_HOME to a JDK 25.
set -u
cd "$(dirname "$0")/.."
mkdir -p build/selftest
nodes=("$@")
if [ ${#nodes[@]} -eq 0 ]; then
  for v in 1.21.1 1.21.3 1.21.4 1.21.5 1.21.8 1.21.10 1.21.11 26.1.2 26.2 26.3; do nodes+=("$v-fabric" "$v-neoforge"); done
fi
for node in "${nodes[@]}"; do
  dir="run/$node"
  mkdir -p "$dir"
  echo "eula=true" > "$dir/eula.txt"
  [ -f "$dir/server.properties" ] || printf 'spawn-protection=0\nonline-mode=false\nserver-port=25599\n' > "$dir/server.properties"
  log="build/selftest/$node.log"
  perl -e 'alarm 900; exec @ARGV' ./gradlew ":$node:runServer" -Pselftest --console=plain > "$log" 2>&1 < /dev/null
  result=$(grep -a "\[PVM selftest\] \(PASS all\|FAIL [0-9]\)" "$log" | tail -1 | sed 's/.*\[PVM selftest\] //')
  [ -z "$result" ] && result="NO RESULT (see $log)"
  echo "$node: $result"
done
