# shellcheck shell=bash
set -euo pipefail

AWS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_DIR="$(cd "$AWS_DIR/.." && pwd)"
TF_DIR="$AWS_DIR/terraform"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
erro() { printf '\033[1;31mERRO:\033[0m %s\n' "$*" >&2; exit 1; }

exige() { command -v "$1" >/dev/null 2>&1 || erro "'$1' não encontrado no PATH"; }

tf_out() {
  terraform -chdir="$TF_DIR" output -raw "$1" 2>/dev/null \
    || erro "output '$1' indisponível — rode 01-infra.sh primeiro"
}
