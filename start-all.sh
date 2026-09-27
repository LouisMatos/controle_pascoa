#!/usr/bin/env bash
# =============================================================================
# start-all.sh — Sobe todo o ambiente Páscoa v5 (infra + microsserviços + monólito)
#
# Uso:
#   ./start-all.sh          → sobe tudo
#   ./start-all.sh stop     → derruba tudo (mantém volumes do Docker)
#   ./start-all.sh status   → mostra serviços rodando
#   ./start-all.sh logs <servico>  → tail -f no log de um serviço
# =============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PID_DIR="$ROOT_DIR/.pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

# Ordem de boot: nome_modulo:porta:rota_health
INFRA_FIRST=("pascoa-config-server:8888" "pascoa-eureka:8761")
MICROSERVICES=(
  "pascoa-auth-service:8081"
  "pascoa-customer-service:8082"
  "pascoa-inventory-service:8083"
  "pascoa-product-service:8084"
  "pascoa-order-service:8085"
  "pascoa-production-service:8086"
  "pascoa-financial-service:8087"
  "pascoa-notification-service:8088"
  "pascoa-analytics-service:8089"
)
GATEWAY_AND_MONOLITH=("pascoa-api-gateway:8090" "pascoa-monolith:8080")

color() { printf "\033[%sm%s\033[0m" "$1" "$2"; }
info()  { echo "$(color '1;34' '[INFO]') $*"; }
ok()    { echo "$(color '1;32' '[ OK ]') $*"; }
warn()  { echo "$(color '1;33' '[WARN]') $*"; }
err()   { echo "$(color '1;31' '[ERR ]') $*"; }

# ---------------------------------------------------------------------------
# Pré-requisitos
# ---------------------------------------------------------------------------
MVN=""

resolve_maven() {
  if [[ -x "$ROOT_DIR/mvnw" ]]; then
    MVN="$ROOT_DIR/mvnw"
    return 0
  fi
  if command -v mvn >/dev/null 2>&1; then
    MVN="mvn"
    return 0
  fi
  # Caminhos comuns no macOS quando o PATH do shell não-interativo está limitado
  for cand in /opt/homebrew/bin/mvn /usr/local/bin/mvn ~/.sdkman/candidates/maven/current/bin/mvn; do
    if [[ -x "$cand" ]]; then
      MVN="$cand"
      return 0
    fi
  done
  return 1
}

install_maven_macos() {
  if ! command -v brew >/dev/null 2>&1; then
    err "Homebrew não encontrado — instale o Maven manualmente: https://maven.apache.org/install.html"
    return 1
  fi
  warn "Maven não encontrado. Instalando via Homebrew (brew install maven)..."
  brew install maven || return 1
  resolve_maven
}

check_prereqs() {
  command -v docker >/dev/null  || { err "docker não encontrado"; exit 1; }

  # Fixa JAVA_HOME no JDK 21 — Lombok 1.18.34 (Spring Boot 3.3.4 BOM) ainda não
  # suporta JDK 24+, e o Maven do Homebrew puxa OpenJDK mais recente por padrão.
  if [[ "$(uname -s)" == "Darwin" ]] && command -v /usr/libexec/java_home >/dev/null; then
    local jdk21
    jdk21=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
    if [[ -n "$jdk21" ]]; then
      export JAVA_HOME="$jdk21"
      export PATH="$JAVA_HOME/bin:$PATH"
    else
      err "JDK 21 não instalado. Instale: brew install --cask temurin@21"; exit 1
    fi
  fi
  command -v java >/dev/null  || { err "java não encontrado"; exit 1; }
  local java_ver
  java_ver=$(java -version 2>&1 | awk -F\" '/version/ {print $2}' | cut -d. -f1)
  [[ "$java_ver" == "21" ]] || { err "Java 21 requerido (encontrado: $java_ver). Ajuste JAVA_HOME."; exit 1; }

  if ! resolve_maven; then
    if [[ "$(uname -s)" == "Darwin" ]]; then
      install_maven_macos || { err "Falha ao instalar Maven"; exit 1; }
    else
      err "Maven não encontrado e SO não é macOS — instale manualmente"; exit 1
    fi
  fi
  ok "Pré-requisitos OK (Java $java_ver, Maven=$MVN, Docker)"
}

# ---------------------------------------------------------------------------
# Espera porta TCP responder
# ---------------------------------------------------------------------------
wait_port() {
  local host=$1 port=$2 name=$3 timeout=${4:-120}
  local elapsed=0
  while ! (echo > /dev/tcp/$host/$port) >/dev/null 2>&1; do
    sleep 2
    elapsed=$((elapsed + 2))
    if (( elapsed >= timeout )); then
      err "$name não respondeu em ${timeout}s (porta $port)"
      return 1
    fi
  done
  ok "$name pronto (porta $port em ${elapsed}s)"
}

# ---------------------------------------------------------------------------
# Sobe infraestrutura Docker (postgres, rabbitmq, redis, zipkin)
# ---------------------------------------------------------------------------
start_infra() {
  info "Subindo infra Docker (Postgres, RabbitMQ, Redis, Zipkin)..."
  ( cd "$ROOT_DIR" && docker compose up -d )
  wait_port localhost 5432  "PostgreSQL"
  wait_port localhost 5672  "RabbitMQ"
  wait_port localhost 6379  "Redis"
  wait_port localhost 9411  "Zipkin"
}

# ---------------------------------------------------------------------------
# Build do projeto (apenas instala artefatos no .m2; pula testes)
# ---------------------------------------------------------------------------
build_all() {
  info "Compilando projeto ($MVN install -Dmaven.test.skip=true)..."
  ( cd "$ROOT_DIR" && "$MVN" -q -Dmaven.test.skip=true install ) \
    && ok "Build concluído" \
    || { err "Falha no build"; exit 1; }
}

# ---------------------------------------------------------------------------
# Sobe um módulo Spring Boot via mvn spring-boot:run
# ---------------------------------------------------------------------------
start_service() {
  local module=$1 port=$2
  local pidfile="$PID_DIR/$module.pid"

  if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    warn "$module já está rodando (PID $(cat "$pidfile"))"
    return 0
  fi

  info "Iniciando $module (porta $port)..."
  # -Dmaven.test.skip=true: spring-boot:run dispara test-compile no ciclo do plugin;
  # se algum teste tem dependência faltando, o serviço não sobe e o wait_port estoura
  # 180s sem mensagem clara. Como o build_all() já compilou tudo, pular testes aqui é seguro.
  ( cd "$ROOT_DIR/$module" && nohup "$MVN" -q -Dmaven.test.skip=true spring-boot:run \
      > "$LOG_DIR/$module.log" 2>&1 & echo $! > "$pidfile" )
  wait_port localhost "$port" "$module" 180 || {
    err "Veja log: $LOG_DIR/$module.log"
    # Mostra as últimas linhas do log para diagnóstico imediato
    echo "--- últimas 20 linhas de $module.log ---"
    tail -20 "$LOG_DIR/$module.log" 2>/dev/null || true
    echo "--- fim do log ---"
    rm -f "$pidfile"
    return 1
  }
}

# ---------------------------------------------------------------------------
# Para todos os serviços Java (mantém Docker rodando)
# ---------------------------------------------------------------------------
stop_services() {
  info "Parando serviços Java..."
  for pidfile in "$PID_DIR"/*.pid; do
    [[ -f "$pidfile" ]] || continue
    local pid name
    pid=$(cat "$pidfile")
    name=$(basename "$pidfile" .pid)
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" && ok "$name parado (PID $pid)"
    fi
    rm -f "$pidfile"
  done
  info "Parando infra Docker..."
  ( cd "$ROOT_DIR" && docker compose down )
  ok "Ambiente parado"
}

# ---------------------------------------------------------------------------
# Status
# ---------------------------------------------------------------------------
status() {
  echo
  echo "=== Containers Docker ==="
  docker compose ps 2>/dev/null || true
  echo
  echo "=== Serviços Java ==="
  printf "%-30s %-8s %-10s\n" "MÓDULO" "PORTA" "STATUS"
  printf -- "%.0s-" {1..52}; echo
  local all=( "${INFRA_FIRST[@]}" "${MICROSERVICES[@]}" "${GATEWAY_AND_MONOLITH[@]}" )
  for entry in "${all[@]}"; do
    local m=${entry%:*} p=${entry#*:}
    local st="DOWN"
    if (echo > /dev/tcp/localhost/$p) >/dev/null 2>&1; then st="UP"; fi
    printf "%-30s %-8s %-10s\n" "$m" "$p" "$st"
  done
  echo
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
case "${1:-up}" in
  up|start|"")
    check_prereqs
    start_infra
    build_all

    # 1) Config Server primeiro (outros serviços leem config dele)
    # 2) Eureka em paralelo após Config
    for entry in "${INFRA_FIRST[@]}"; do
      start_service "${entry%:*}" "${entry#*:}"
    done

    # 3) Microsserviços de domínio
    for entry in "${MICROSERVICES[@]}"; do
      start_service "${entry%:*}" "${entry#*:}"
    done

    # 4) Gateway + monólito (camada de borda)
    for entry in "${GATEWAY_AND_MONOLITH[@]}"; do
      start_service "${entry%:*}" "${entry#*:}"
    done

    echo
    ok "Ambiente Páscoa v5 no ar!"
    echo
    echo "  Monólito (UI Thymeleaf) : http://localhost:8080   (admin/admin123)"
    echo "  API Gateway              : http://localhost:8090"
    echo "  Eureka Dashboard         : http://localhost:8761"
    echo "  Config Server            : http://localhost:8888  (config-admin/config123)"
    echo "  RabbitMQ Management      : http://localhost:15672 (pascoa/pascoa123)"
    echo "  Zipkin Tracing           : http://localhost:9411"
    echo
    echo "Logs:    $LOG_DIR/<modulo>.log"
    echo "Parar:   ./start-all.sh stop"
    echo "Status:  ./start-all.sh status"
    ;;
  stop|down)
    stop_services
    ;;
  status|ps)
    status
    ;;
  logs)
    [[ -n "${2:-}" ]] || { err "Uso: ./start-all.sh logs <pascoa-xxx>"; exit 1; }
    tail -f "$LOG_DIR/$2.log"
    ;;
  *)
    err "Comando inválido: $1"
    echo "Uso: $0 [up|stop|status|logs <modulo>]"
    exit 1
    ;;
esac
