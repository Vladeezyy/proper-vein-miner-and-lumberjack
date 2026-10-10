#!/usr/bin/env bash
# Starts the client of every version and loader (or the ones given), which opens the settings screen, checks the keys
# and saves a screenshot (ScreenshotScene.java), then quits. Needs a display. Gradle on JDK 25 (JAVA_HOME).
#   tools/clientcheck.sh                 # all
#   tools/clientcheck.sh 26.3-fabric     # one
set -u
cd "$(dirname "$0")/.."
mkdir -p build/clientcheck
nodes=("$@")
if [ ${#nodes[@]} -eq 0 ]; then
  for v in 1.21.1 1.21.3 1.21.4 1.21.5 1.21.8 1.21.10 1.21.11 26.1.2 26.2 26.3; do nodes+=("$v-fabric" "$v-neoforge"); done
fi
for node in "${nodes[@]}"; do
  dir="run/$node"
  mkdir -p "$dir/screenshots"
  [ -f "$dir/options.txt" ] || printf 'onboardAccessibility:false\nlang:en_us\nnarrator:0\ntutorialStep:none\nsoundCategory_master:0.0\nguiScale:2\n' > "$dir/options.txt"
  rm -f "$dir"/screenshots/*.png
  log="build/clientcheck/$node.log"
  perl -e 'alarm 600; exec @ARGV' ./gradlew ":$node:runClient" -Pscreenshots --console=plain > "$log" 2>&1 < /dev/null
  result=$(grep -a "\[PVM client\] \(PASS\|FAIL\)" "$log" | tail -1 | sed 's/.*\[PVM client\] //')
  [ -z "$result" ] && result="NO RESULT (see $log)"
  shot=$(ls -t "$dir"/screenshots/*.png 2>/dev/null | head -1)
  [ -n "$shot" ] && cp "$shot" "build/clientcheck/$node.png"
  echo "$node: $result"
done
