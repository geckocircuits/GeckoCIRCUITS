#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
base_url=${GECKO_API_BASE_URL:-http://localhost:8080/gecko}
destination="$root/docs/api/openapi.json"
if [[ ${1:-} == --dry-run ]]; then
    printf 'Read %s/api-docs and write validated JSON to %s\n' "${base_url%/}" "$destination"
    exit 0
fi
if [[ $# -gt 0 ]]; then
    printf 'Usage: GECKO_API_BASE_URL=<url> bash scripts/snapshot-openapi.sh [--dry-run]\n' >&2
    exit 2
fi
mkdir -p "$(dirname "$destination")"
curl --fail --silent --show-error "${base_url%/}/api-docs" |
    python3 -c 'import json,pathlib,sys; spec=json.load(sys.stdin); assert "openapi" in spec and "paths" in spec; pathlib.Path(sys.argv[1]).write_text(json.dumps(spec,indent=2,sort_keys=True)+"\n")' "$destination"
printf 'Saved %s\n' "$destination"
