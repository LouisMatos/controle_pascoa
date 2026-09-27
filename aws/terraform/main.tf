locals {
  name = "pascoa-${var.environment}"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-kernel-6.1-x86_64"]
  }
}

# ---------------------------------------------------------------- ECR

resource "aws_ecr_repository" "app" {
  name                 = local.name
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Mantém apenas as 3 imagens mais recentes"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 3 }
      action       = { type = "expire" }
    }]
  })
}

# ---------------------------------------------------------------- IAM (SSM + ECR pull)

resource "aws_iam_role" "ec2" {
  name = "${local.name}-ec2"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "ecr" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${local.name}-ec2"
  role = aws_iam_role.ec2.name
}

# ---------------------------------------------------------------- Rede

resource "aws_security_group" "app" {
  name        = "${local.name}-app"
  description = "pascoa sandbox: HTTP da aplicacao. Sem porta 22 (acesso via SSM Session Manager)."
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP da aplicacao (origem: integracao HTTP_PROXY do API Gateway)"
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = var.allowed_app_cidrs
  }

  egress {
    description = "Saida liberada (ECR, SSM, SMTP, updates)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# EIP standalone: o API Gateway precisa do IP antes da instância existir,
# e a instância precisa da URL do gateway. Separar evita dependência circular.
resource "aws_eip" "app" {
  domain = "vpc"
  tags   = { Name = local.name }
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}

# ---------------------------------------------------------------- EC2

resource "aws_instance" "app" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.instance_type
  subnet_id              = data.aws_subnets.default.ids[0]
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size
    encrypted             = true
    delete_on_termination = true
  }

  metadata_options {
    http_tokens = "required" # IMDSv2 obrigatório
  }

  user_data_replace_on_change = true
  user_data = templatefile("${path.module}/templates/user_data.sh.tftpl", {
    compose_file = templatefile("${path.module}/templates/docker-compose.yml.tftpl", {
      image    = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"
      app_port = var.app_port
    })
    region                = var.region
    ecr_registry          = split("/", aws_ecr_repository.app.repository_url)[0]
    db_password           = var.db_password
    admin_senha_inicial   = var.admin_senha_inicial
    mail_username         = var.mail_username
    mail_password         = var.mail_password
    app_base_url          = "https://${aws_apigatewayv2_api.app.id}.execute-api.${var.region}.amazonaws.com"
    gateway_shared_secret = var.gateway_shared_secret
  })

  tags = { Name = local.name }
}

# ---------------------------------------------------------------- API Gateway (HTTP API)

resource "aws_apigatewayv2_api" "app" {
  name          = local.name
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "root" {
  api_id                 = aws_apigatewayv2_api.app.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_eip.app.public_ip}:${var.app_port}/"
  payload_format_version = "1.0"
  timeout_milliseconds   = 30000

  request_parameters = var.gateway_shared_secret == "" ? {} : {
    "overwrite:header.X-Gateway-Secret" = var.gateway_shared_secret
  }
}

resource "aws_apigatewayv2_integration" "proxy" {
  api_id                 = aws_apigatewayv2_api.app.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_eip.app.public_ip}:${var.app_port}/{proxy}"
  payload_format_version = "1.0"
  timeout_milliseconds   = 30000

  request_parameters = var.gateway_shared_secret == "" ? {} : {
    "overwrite:header.X-Gateway-Secret" = var.gateway_shared_secret
  }
}

resource "aws_apigatewayv2_route" "root" {
  api_id    = aws_apigatewayv2_api.app.id
  route_key = "ANY /"
  target    = "integrations/${aws_apigatewayv2_integration.root.id}"
}

resource "aws_apigatewayv2_route" "proxy" {
  api_id    = aws_apigatewayv2_api.app.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.proxy.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.app.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_burst_limit = 50
    throttling_rate_limit  = 100
  }
}
