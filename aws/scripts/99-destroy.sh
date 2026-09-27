#!/usr/bin/env bash
# Destrói toda a infra do sandbox. Os dados do Postgres vivem no EBS da EC2 e são perdidos.
source "$(dirname "$0")/_common.sh"

exige terraform
log "ATENÇÃO: isto remove EC2, EIP, API Gateway, ECR e o banco de dados do sandbox."
terraform -chdir="$TF_DIR" destroy -input=false "$@"
