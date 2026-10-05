#!/usr/bin/env bash
# Pipeline completo: infra -> build/push -> deploy.
source "$(dirname "$0")/scripts/_common.sh"

"$AWS_DIR/scripts/01-infra.sh"
"$AWS_DIR/scripts/02-build-push.sh"
"$AWS_DIR/scripts/03-deploy.sh"
