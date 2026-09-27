variable "region" {
  description = "Região AWS do sandbox"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Nome do ambiente (compõe o nome dos recursos)"
  type        = string
  default     = "sandbox"
}

variable "instance_type" {
  description = "Tipo da EC2. t3.small (2 GB) é o mínimo viável para app + Postgres com swap."
  type        = string
  default     = "t3.small"
}

variable "root_volume_size" {
  description = "Tamanho do EBS raiz em GB (app + imagens Docker + dados do Postgres)"
  type        = number
  default     = 30
}

variable "db_password" {
  description = "Senha do usuário postgres no container"
  type        = string
  sensitive   = true
}

variable "admin_senha_inicial" {
  description = "Senha do usuário admin criado no primeiro boot (DataInitializer)"
  type        = string
  sensitive   = true
}

variable "mail_username" {
  description = "Usuário SMTP (opcional; vazio desliga envio real de email)"
  type        = string
  default     = ""
}

variable "mail_password" {
  description = "Senha/app-password SMTP"
  type        = string
  default     = ""
  sensitive   = true
}

variable "app_port" {
  description = "Porta HTTP exposta pela aplicação na EC2"
  type        = number
  default     = 8080
}

variable "allowed_app_cidrs" {
  description = <<-EOT
    CIDRs que podem falar direto na porta da aplicação na EC2.
    O API Gateway HTTP API não publica faixa de IPs fixa e sua integração HTTP_PROXY
    sai da internet pública, então 0.0.0.0/0 é necessário para o gateway funcionar.
    O acesso direto ao IP da EC2 é barrado pelo segredo compartilhado do gateway
    (var.gateway_shared_secret), não pelo security group.
    Restrinja aqui se você abrir mão do API Gateway e for acessar só do seu IP.
  EOT
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "gateway_shared_secret" {
  description = <<-EOT
    Segredo enviado pelo API Gateway no header X-Gateway-Secret e exigido pela aplicação.
    Vazio = filtro desligado e a EC2 fica acessível direto pelo IP público.
    Gere com: openssl rand -hex 32
  EOT
  type        = string
  default     = ""
  sensitive   = true
}

variable "image_tag" {
  description = "Tag da imagem no ECR que a EC2 deve rodar"
  type        = string
  default     = "latest"
}
