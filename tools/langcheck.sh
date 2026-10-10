#!/usr/bin/env bash
# Screenshots the settings screen in every language (one version, default 1.21.11-fabric) to build/clientcheck/lang-*.png.
set -u
cd "$(dirname "$0")/.."
node="${1:-1.21.11-fabric}"
dir="run/$node"
mkdir -p "$dir/screenshots" build/clientcheck
[ -f "$dir/options.txt" ] || printf 'onboardAccessibility:false\nlang:en_us\nnarrator:0\ntutorialStep:none\nsoundCategory_master:0.0\nguiScale:2\n' > "$dir/options.txt"
for lang in $(ls src/main/resources/assets/properveinminer/lang | sed 's/.json//'); do
  if grep -q '^lang:' "$dir/options.txt"; then sed -i '' "s/^lang:.*/lang:$lang/" "$dir/options.txt"; else echo "lang:$lang" >> "$dir/options.txt"; fi
  rm -f "$dir"/screenshots/*.png
  perl -e 'alarm 600; exec @ARGV' ./gradlew ":$node:runClient" -Pscreenshots --console=plain > "build/clientcheck/lang-$lang.log" 2>&1 < /dev/null
  shot=$(ls -t "$dir"/screenshots/*.png 2>/dev/null | head -1)
  [ -n "$shot" ] && cp "$shot" "build/clientcheck/lang-$lang.png"
  echo "$lang: $(grep -a '\[PVM client\] \(PASS\|FAIL\)' "build/clientcheck/lang-$lang.log" | tail -1 | sed 's/.*\[PVM client\] //')"
done
sed -i '' "s/^lang:.*/lang:en_us/" "$dir/options.txt"
