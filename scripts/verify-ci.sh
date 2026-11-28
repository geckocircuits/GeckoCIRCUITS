#!/usr/bin/env bash
set -euo pipefail

# verify runs tests, application fixtures, and the configured coverage checks.
xvfb-run -a mvn -f pom-reactor.xml verify -Pall-tests --no-transfer-progress "$@"
