output "url_publica" {
  description = "URL do API Gateway — é esta que você divulga para o acesso trial"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "instance_id" {
  value = aws_instance.app.id
}

output "instance_public_ip" {
  value = aws_eip.app.public_ip
}

output "region" {
  value = var.region
}

output "ssm_session" {
  description = "Abrir shell na instância sem SSH"
  value       = "aws ssm start-session --region ${var.region} --target ${aws_instance.app.id}"
}
