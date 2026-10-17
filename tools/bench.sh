#!/usr/bin/env bash
# Server tick benchmark (Bench.java): a real dedicated server, a fake player breaks a big ore vein, a big tree and
# 1000 stone with the mod and the vanilla way; every tick timed. Default node 1.21.11-fabric.
#   tools/bench.sh                    # 1.21.11-fabric
#   tools/bench.sh 26.3-neoforge ...  # others
# Gradle has to run on Java 25 (Loom 1.18): set JAVA_HOME to a JDK 25.
set -u
cd "$(dirname "$0")/.."
mkdir -p build/bench
nodes=("$@")
[ ${#nodes[@]} -eq 0 ] && nodes=("1.21.11-fabric")
for node in "${nodes[@]}"; do
  dir="run/$node"
  mkdir -p "$dir"
  echo "eula=true" > "$dir/eula.txt"
  [ -f "$dir/server.properties" ] || printf 'spawn-protection=0\nonline-mode=false\nserver-port=25599\n' > "$dir/server.properties"
  # an empty server pauses after 60 s (1.21.2+); the benchmark runs longer
  grep -q '^pause-when-empty-seconds=' "$dir/server.properties" \
    && sed -i '' 's/^pause-when-empty-seconds=.*/pause-when-empty-seconds=-1/' "$dir/server.properties" \
    || echo 'pause-when-empty-seconds=-1' >> "$dir/server.properties"
  log="build/bench/$node.log"
  perl -e 'alarm 1200; exec @ARGV' ./gradlew ":$node:runServer" -Pbench --console=plain > "$log" 2>&1 < /dev/null
  echo "== $node"
  grep -a "\[PVM bench\]" "$log" | sed 's/.*\[PVM bench\] //'
done
