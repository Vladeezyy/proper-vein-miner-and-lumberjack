#!/usr/bin/env bash
# One GitHub release per Minecraft version (v<mod version>+<minecraft>), with the Fabric and the NeoForge jar.
# Skips releases that exist. Needs gh logged in. Notes from docs/release-notes.md.
set -eu
cd "$(dirname "$0")/.."
version=$(grep '^mod.version' stonecutter.properties.toml | sed 's/.*"\(.*\)"/\1/')
# Oldest first, so the newest Minecraft version ends up as "Latest"
for node in 1.21.1 1.21.3 1.21.4 1.21.5 1.21.8 1.21.10 1.21.11 26.1.2 26.2 26.3; do
  releases=$(python3 - "$node" <<'PY'
import re, sys
text = open("stonecutter.properties.toml").read()
body = re.search(r'^\["%s"\]\n(.*?)(?=^\[)' % re.escape(sys.argv[1]), text, re.M | re.S).group(1)
r = re.findall(r'"([^"]+)"', re.search(r'mod\.mc_releases = \[(.*?)\]', body).group(1))
print(r[0] if len(r) == 1 else r[0] + "–" + r[-1])
PY
)
  tag="v$version+$node"
  if gh release view "$tag" > /dev/null 2>&1; then echo "exists: $tag"; continue; fi
  jars=(build/libs/$version/properveinminer-fabric-$version+$node.jar build/libs/$version/properveinminer-neoforge-$version+$node.jar)
  notes=$(sed "s/{{minecraft}}/$releases/g; s/{{version}}/$version/g; s/{{node}}/$node/g" docs/release-notes.md)
  gh release create "$tag" "${jars[@]}" --target main --title "$version for Minecraft $releases" --notes "$notes" > /dev/null
  echo "released: $tag ($releases)"
done
