#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
image=openapitools/openapi-generator-cli:v7.10.0
dry_run=false
if [[ ${1:-} == --dry-run ]]; then
    dry_run=true
elif [[ $# -gt 0 ]]; then
    printf 'Usage: bash scripts/generate-sdks.sh [--dry-run]\n' >&2
    exit 2
fi
if [[ ! -f "$root/docs/api/openapi.json" ]]; then
    printf 'Capture the API specification with scripts/snapshot-openapi.sh first.\n' >&2
    exit 1
fi
docker_bin=${DOCKER_BIN:-docker}
mount_root=$root
# Docker Desktop can serve WSL through its Windows client without changing integration settings.
if [[ $docker_bin == docker ]] && ! docker info >/dev/null 2>&1; then
    if command -v docker.exe >/dev/null 2>&1 && docker.exe info >/dev/null 2>&1; then
        docker_bin=docker.exe
    else
        printf 'Start a Docker engine before generating the SDKs.\n' >&2
        exit 1
    fi
fi
if [[ $docker_bin == *.exe ]]; then mount_root=$(wslpath -w "$root"); fi

generate() {
    local directory=$1 generator=$2 properties=$3
    local command=("$docker_bin" run --rm --user "$(id -u):$(id -g)" --mount "type=bind,source=$mount_root,target=/local"
        "$image" generate -i /local/docs/api/openapi.json -g "$generator"
        -o "/local/clients/$directory" --global-property skipFormModel=false --additional-properties "$properties")
    if $dry_run; then
        printf '%q ' "${command[@]}"
        printf '\n'
    else
        "${command[@]}"
    fi
}
generate python python 'packageName=gecko_client,projectName=geckocircuits-client,packageVersion=1.0.0,hideGenerationTimestamp=true'
generate java java 'groupId=gecko,artifactId=geckocircuits-client,invokerPackage=gecko.client,apiPackage=gecko.client.api,modelPackage=gecko.client.model,library=native,hideGenerationTimestamp=true'
generate typescript typescript-fetch 'npmName=geckocircuits-client,npmVersion=1.0.0,supportsES6=true,hideGenerationTimestamp=true'
