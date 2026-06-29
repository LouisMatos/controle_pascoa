#!/usr/bin/env bash
# =============================================================================
# register-canary-tenants.sh — Provisiona 5 tenants TRIAL para o canary v6
#
# Cada tenant cobre um TipoNegocio distinto, validando que os 6 templates
# (5 + CUSTOM) funcionam end-to-end. O 6º (CUSTOM) é provisionado sob demanda.
#
# Uso:
#   ./register-canary-tenants.sh                 → cria os 5
#   ./register-canary-tenants.sh --dry-run       → só mostra os payloads
#   ./register-canary-tenants.sh --base URL      → aponta para outro host
#   ./register-canary-tenants.sh --rollback      → desativa (suspende) os 5
# =============================================================================
set -euo pipefail

BASE_URL="${TENANT_SERVICE_URL:-http://localhost:8094}"
DRY=false
ROLLBACK=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)  DRY=true; shift ;;
    --base)     BASE_URL="$2"; shift 2 ;;
    --rollback) ROLLBACK=true; shift ;;
    *) echo "Argumento desconhecido: $1"; exit 1 ;;
  esac
done

# slug | razaoSocial | email | tipoNegocio | corPrimaria | nomeApp
TENANTS=(
  "confeitaria-canary-1|Confeitaria Canary 1 LTDA|canary1@confeitaria.test|CONFEITARIA|#e91e63|Doces Canary 1"
  "marmitaria-canary-1|Marmitaria Canary 1 ME|canary1@marmitaria.test|MARMITARIA|#4caf50|Marmitas Canary"
  "restaurante-canary-1|Restaurante Canary 1 LTDA|canary1@restaurante.test|RESTAURANTE|#ff9800|Restaurante Canary"
  "salgaderia-canary-1|Salgaderia Canary 1 ME|canary1@salgaderia.test|SALGADERIA|#795548|Salgaderia Canary"
  "doces-canary-1|Doces Canary 1 LTDA|canary1@doces.test|DOCES|#9c27b0|Doces Premium Canary"
)

color()  { printf "\033[%sm%s\033[0m" "$1" "$2"; }
info()   { echo "$(color '1;34' '[INFO]') $*"; }
ok()     { echo "$(color '1;32' '[ OK ]') $*"; }
warn()   { echo "$(color '1;33' '[WARN]') $*"; }
err()    { echo "$(color '1;31' '[ERR ]') $*"; }

if [[ "$ROLLBACK" == "true" ]]; then
  info "Rollback — suspendendo todos os tenants canary em $BASE_URL"
  for entry in "${TENANTS[@]}"; do
    IFS='|' read -r SLUG _ _ _ _ _ <<<"$entry"
    info "Suspendendo $SLUG..."
    curl -fsS -X POST "$BASE_URL/tenants/$SLUG/suspender" >/dev/null \
      && ok "$SLUG → SUSPENSO" \
      || warn "$SLUG não encontrado (já removido?)"
  done
  ok "Rollback concluído. Para deletar definitivamente: DELETE /tenants/{id}"
  exit 0
fi

info "Provisionando 5 tenants TRIAL em $BASE_URL"
$DRY && warn "Modo --dry-run: apenas exibindo payloads"

for entry in "${TENANTS[@]}"; do
  IFS='|' read -r SLUG RAZAO EMAIL TIPO COR NOME_APP <<<"$entry"
  PAYLOAD=$(cat <<JSON
{
  "tenantId": "$SLUG",
  "razaoSocial": "$RAZAO",
  "email": "$EMAIL",
  "tipoNegocio": "$TIPO",
  "whiteLabel": {
    "nomeApp": "$NOME_APP",
    "corPrimaria": "$COR",
    "corSecundaria": "#ffffff"
  }
}
JSON
  )

  if $DRY; then
    echo "── $SLUG ──"
    echo "$PAYLOAD"
    continue
  fi

  info "→ $SLUG ($TIPO)"
  RESP=$(curl -fsS -o /tmp/canary-resp.json -w "%{http_code}" \
    -X POST "$BASE_URL/onboarding/iniciar" \
    -H 'Content-Type: application/json' \
    -d "$PAYLOAD" || echo "ERR")

  case "$RESP" in
    201) ok  "$SLUG criado (todas as etapas OK)" ;;
    207) warn "$SLUG criado com avisos (alguma etapa best-effort falhou — ver resposta)";
         jq -r '.aplicacaoTemplate.detalhe, .inicioTrial.detalhe' /tmp/canary-resp.json 2>/dev/null ;;
    409) warn "$SLUG já existia (idempotente)" ;;
    *)   err "$SLUG falhou (HTTP $RESP)";
         cat /tmp/canary-resp.json 2>/dev/null; echo ;;
  esac
done

ok "Concluído. Próximos passos:"
echo "  1. curl $BASE_URL/tenants/confeitaria-canary-1 — validar status TRIAL"
echo "  2. Monitorar /actuator/health + metering em /subscriptions/{id}/uso"
echo "  3. Para rollback: $0 --rollback"
