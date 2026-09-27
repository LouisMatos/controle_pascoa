#!/usr/bin/env bash
# Builda o jar do monólito, empacota em imagem linux/amd64 e envia para o ECR.
source "$(dirname "$0")/_common.sh"

exige mvn
exige docker
exige aws

REGION="$(tf_out region)"
ECR_URL="$(tf_out ecr_repository_url)"
TAG="$(git -C "$REPO_DIR" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)"

log "build do jar (pascoa-monolith, sem testes)"
mvn -q -f "$REPO_DIR/pom.xml" -DskipTests -pl pascoa-monolith -am package

JAR=$(ls -1 "$REPO_DIR"/pascoa-monolith/target/pascoa-monolith-*.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || erro "jar não encontrado em pascoa-monolith/target"
log "jar: $(basename "$JAR") ($(du -h "$JAR" | cut -f1))"

log "login no ECR"
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${ECR_URL%%/*}"

log "docker build (linux/amd64) tags: $TAG, latest"
docker build --platform linux/amd64 \
  -f "$AWS_DIR/Dockerfile" \
  -t "$ECR_URL:$TAG" -t "$ECR_URL:latest" \
  "$REPO_DIR"

log "push"
docker push "$ECR_URL:$TAG"
docker push "$ECR_URL:latest"

log "imagem publicada: $ECR_URL:$TAG"
