#!/usr/bin/env bash
# Faz a EC2 puxar a imagem mais recente e reiniciar a stack, via SSM (sem SSH).
source "$(dirname "$0")/_common.sh"

exige aws

REGION="$(tf_out region)"
INSTANCE="$(tf_out instance_id)"
URL="$(tf_out url_publica)"; URL="${URL%/}"

log "aguardando a instância ficar registrada no SSM"
for i in $(seq 1 40); do
  estado=$(aws ssm describe-instance-information --region "$REGION" \
    --filters "Key=InstanceIds,Values=$INSTANCE" \
    --query 'InstanceInformationList[0].PingStatus' --output text 2>/dev/null || echo None)
  if [ "$estado" = "Online" ]; then break; fi
  if [ "$i" = 40 ]; then
    erro "instância não apareceu no SSM (bootstrap ainda rodando? veja /var/log/pascoa-bootstrap.log)"
  fi
  sleep 15
done
log "SSM online"

CMD_ID=$(aws ssm send-command --region "$REGION" \
  --instance-ids "$INSTANCE" \
  --document-name AWS-RunShellScript \
  --comment "deploy pascoa sandbox" \
  --timeout-seconds 600 \
  --parameters 'commands=[
    "set -euxo pipefail",
    "test -f /opt/pascoa/docker-compose.yml",
    "/opt/pascoa/ecr-login.sh",
    "cd /opt/pascoa",
    "docker compose pull",
    "docker compose up -d --remove-orphans",
    "docker image prune -f",
    "docker compose ps"
  ]' \
  --query 'Command.CommandId' --output text)

log "comando SSM: $CMD_ID"
aws ssm wait command-executed --region "$REGION" \
  --command-id "$CMD_ID" --instance-id "$INSTANCE" || true

STATUS=$(aws ssm get-command-invocation --region "$REGION" \
  --command-id "$CMD_ID" --instance-id "$INSTANCE" --query Status --output text)
aws ssm get-command-invocation --region "$REGION" \
  --command-id "$CMD_ID" --instance-id "$INSTANCE" \
  --query 'StandardErrorContent' --output text | tail -20
[ "$STATUS" = "Success" ] || erro "deploy falhou no SSM (status: $STATUS)"

log "esperando /actuator/health/readiness responder UP em $URL"
for i in $(seq 1 40); do
  if curl -fsS --max-time 10 "$URL/actuator/health/readiness" 2>/dev/null | grep -q '"status":"UP"'; then
    log "aplicação no ar: $URL"
    log "login inicial: admin / (valor de admin_senha_inicial no terraform.tfvars)"
    exit 0
  fi
  sleep 15
done
erro "health não ficou UP. Logs: aws ssm start-session --region $REGION --target $INSTANCE, depois 'docker compose -f /opt/pascoa/docker-compose.yml logs app'"
