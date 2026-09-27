#!/usr/bin/env bash
# Cria/atualiza a infra do sandbox (ECR, IAM, SG, EIP, EC2, API Gateway).
source "$(dirname "$0")/_common.sh"

exige terraform
exige aws

[ -f "$TF_DIR/terraform.tfvars" ] \
  || erro "crie $TF_DIR/terraform.tfvars a partir de terraform.tfvars.example"

aws sts get-caller-identity >/dev/null || erro "credenciais AWS não configuradas"

log "terraform init"
terraform -chdir="$TF_DIR" init -input=false

log "terraform apply"
terraform -chdir="$TF_DIR" apply -input=false ${TF_AUTO_APPROVE:+-auto-approve} "$@"

log "infra pronta"
terraform -chdir="$TF_DIR" output
