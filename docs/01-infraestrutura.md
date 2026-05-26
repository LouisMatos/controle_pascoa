# Documentação de Infraestrutura — Sistema Controle Páscoa

> **Projeto:** Sistema de Gestão de Ovos de Páscoa Artesanal  
> **Versão:** Spring Boot 3.3.4 / Java 21  
> **Atualizado em:** 2026-05-26

---

## 1. Stack Tecnológica

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 21 (LTS) |
| Framework web | Spring Boot | 3.3.4 |
| ORM | Spring Data JPA / Hibernate | via Boot BOM |
| Banco de dados | PostgreSQL | 12+ |
| Migrations | Flyway | via Boot BOM |
| Template engine | Thymeleaf | 3.x |
| CSS/UI | Bootstrap | 5.3.2 |
| Ícones | Bootstrap Icons | 1.11.3 |
| Segurança | Spring Security | 6.x |
| Validação | Bean Validation / Hibernate Validator | via Boot BOM |
| PDF | OpenPDF | Maven |
| Excel | Apache POI | Maven |
| QR Code | ZXing (Google) | Maven |
| E-mail | Spring Mail (SMTP) | via Boot BOM |
| Build | Maven | 3.x |
| Testes | JUnit 5 + H2 in-memory | via Boot BOM |

---

## 2. Banco de Dados

### 2.1 Configuração

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pascoa_db
spring.datasource.username=postgres
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

- DDL gerenciado **exclusivamente pelo Flyway** (`ddl-auto=validate` apenas verifica o schema).
- O schema nunca é gerado pelo Hibernate em produção.

### 2.2 Migrations Flyway

Localização: `src/main/resources/db/migration/`

| Arquivo | Conteúdo |
|---------|----------|
| `V1__baseline.sql` | Tabelas base: fornecedores, clientes, produtos, matérias-primas, fichas_técnicas |
| `V2__novas_tabelas_v3.sql` | Gastos, orçamentos de cliente, pontos de fidelidade, ordens de produção, contas a pagar/receber, despesas, pedidos, pagamentos |
| `V3__crm_notas.sql` | Notas de cliente, pontos fidelidade, campos de segmentação |
| `V4__alertas_internos.sql` | Tabela de alertas internos do sistema |
| `v2_item1_campos_novos.sql` | Novos campos adicionais em tabelas existentes |
| `v2_item2_entidades_financeiras.sql` | Tabela de configuração financeira (meta mensal) |
| `v2_item4_notificacoes.sql` | Templates de notificação, configurações de canal, histórico de envios |

> **Convenção de nomenclatura:** `V{numero}__{descricao}.sql` (dois underscores no nome Flyway padrão).

### 2.3 Tabelas Principais por Módulo

| Módulo | Tabelas |
|--------|---------|
| Cadastro | `clientes`, `produtos`, `fornecedores`, `materias_primas` |
| Fichas | `fichas_tecnicas`, `fichas_tecnicas_itens` |
| Pedidos | `pedidos`, `itens_pedido`, `pagamentos` |
| Orçamentos | `orcamentos`, `orcamentos_itens` |
| Produção | `ordens_producao` |
| Qualidade | `inspecoes_qualidade` (usa coluna JSONB para checklist) |
| Estoque | `movimentacoes_estoque` |
| Financeiro | `contas_receber`, `contas_pagar`, `despesas_fixas`, `despesas_variaveis`, `configuracao_financeira` |
| CRM | `notas_cliente`, `pontos_fidelidade` |
| Notificações | `templates_notificacao`, `configuracoes_canal`, `notificacoes_enviadas`, `alertas_internos` |
| Gastos | `gastos_variaveis`, `orcamentos_gasto` |
| Segurança | `usuarios` |

### 2.4 Recursos PostgreSQL Utilizados

- **JSONB** — coluna `itens_verificados` em `InspecaoQualidade` (checklist dinâmico sem schema fixo).
- **Soft-delete** — tabelas `clientes` e `produtos` usam `@SQLDelete` + `@SQLRestriction` do Hibernate (coluna `deletado`). Nenhum registro é fisicamente removido.

---

## 3. Gerenciamento de Dependências (pom.xml)

Dependências declaradas explicitamente (versões gerenciadas pelo BOM do Spring Boot 3.3.4):

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-thymeleaf
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-mail

<!-- Database -->
postgresql (driver)
flyway-core

<!-- Lombok -->
lombok

<!-- PDF / Excel / QR -->
openpdf
poi-ooxml (Apache POI)
zxing core + javase

<!-- Testes -->
spring-boot-starter-test
spring-security-test
h2 (scope test)
```

---

## 4. Upload de Arquivos

```properties
app.upload.dir=${user.home}/pascoa-uploads
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
```

- Imagens de produtos são salvas em disco no diretório `~/pascoa-uploads`.
- Servidas estaticamente pelo Spring via `WebMvcConfig` (resource handler mapeado em `/uploads/**`).
- Não há integração com cloud storage (S3, GCS etc.) — armazenamento local.

---

## 5. Serviço de E-mail (SMTP)

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<configurar>
spring.mail.password=<configurar>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

- Integração com Gmail SMTP via STARTTLS.
- Configuração de **modo de teste** por canal disponível via tela `/notificacoes/configuracao`.
- `EmailService` usa `JavaMailSender` do Spring.

---

## 6. Integração WhatsApp (Evolution API)

```properties
app.whatsapp.base-url=http://localhost:8080/evolution
```

- Integração via HTTP com **Evolution API** (self-hosted).
- URL e token configuráveis pelo painel de administração (`/notificacoes/configuracao`), sem necessidade de alterar `application.properties`.
- `WhatsAppService` realiza chamadas REST ao endpoint da Evolution API.

---

## 7. Segurança

### 7.1 Spring Security 6

Configurado em `SecurityConfig.java`:

```
Rotas públicas (sem autenticação):
  /login, /logout
  /acompanhamento/**   → rastreamento de pedido por token
  /orcamento-publico/** → aprovação de orçamento por token
  /catalogo/**         → catálogo de produtos
  /uploads/**          → arquivos estáticos
  /manifest.json, /sw.js, /icons/** → PWA

Todas as outras rotas exigem autenticação.
```

- **Autenticação**: Formulário de login padrão Spring Security.
- **Senhas**: Hash BCrypt.
- **Autorização**: RBAC por role (ver seção 7.2).

### 7.2 Roles e Permissões

| Role | Escopo de acesso |
|------|-----------------|
| `ADMIN` | Tudo (incluindo usuários, notificações, configurações) |
| `FINANCEIRO` | Módulos financeiro e gastos |
| `ATENDENTE` | CRM, clientes, atendimento |
| `CONFEITEIRO` | Produção, filas de produção |
| `GESTOR_QUALIDADE` | Qualidade e inspeção |
| `ANALISTA` | Analytics, relatórios (leitura) |

Roles são definidos na enum `Role.java` e armazenados em `usuarios.role`.

### 7.3 Auditoria

`BaseEntity.java` (superclasse de todas as entidades):
- `criadoEm` / `atualizadoEm` — timestamps automáticos (`@CreatedDate`, `@LastModifiedDate`)
- `criadoPor` / `atualizadoPor` — usuário logado via `AuditorAwareImpl` (retorna `Authentication.getName()`)
- Spring Data Auditing habilitado com `@EnableJpaAuditing`.

---

## 8. Templates e Front-end

### 8.1 Motor de Templates

- **Thymeleaf 3** com suporte a fragmentos (`th:replace`, `th:insert`).
- **Cache desabilitado** em desenvolvimento: `spring.thymeleaf.cache=false`.
- Encoding UTF-8 garantido: `spring.thymeleaf.encoding=UTF-8`.

### 8.2 Layout Master

`src/main/resources/templates/fragments/layout.html`:
- Navbar responsiva com menu diferenciado por role (via `sec:authorize` do Thymeleaf-Extras-Security).
- Sidebar com links de módulos.
- Modal global de feedback de erros/sucesso.
- Importação de Bootstrap 5.3.2 + Bootstrap Icons 1.11.3.
- Meta tags PWA + link para `manifest.json`.

### 8.3 PWA (Progressive Web App)

- `manifest.json` e `sw.js` servidos como estáticos.
- `PwaController` gerencia o service worker.
- Permite instalação como app nativo em dispositivos móveis.

---

## 9. Exportação de Dados

| Formato | Biblioteca | Funcionalidade |
|---------|-----------|----------------|
| PDF | OpenPDF | Orçamentos para clientes (`OrcamentoPdfService`) |
| Excel (.xlsx) | Apache POI | Exportação de pedidos/relatórios (`ExportService`) |
| QR Code | ZXing (Google) | QR de rastreamento de pedido |
| CSV | Leitura nativa | Importação de gastos variáveis |

---

## 10. Testes

### 10.1 Configuração de Teste

```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

- Profile `test` usa H2 in-memory.
- Schema criado e destruído a cada execução.
- Não usa Flyway em testes.

### 10.2 Suíte de Testes de Integração

| Classe | Cobertura |
|--------|-----------|
| `CustoRealServiceIntegrationTest` | Cálculo de custo real baseado em Ficha Técnica |
| `FluxoCaixaGastosIntegrationTest` | Fluxo de caixa incluindo gastos variáveis |
| `NotificacaoEventListenerTest` | Processamento de eventos e envio de notificações |
| `AlertaInternoIntegrationTest` | Criação e consulta de alertas internos |
| `OrcamentoServiceIntegrationTest` | CRUD de orçamento e conversão em pedido |
| `RolePermissionsTest` | Validação de autorização por role (Spring Security Test) |

---

## 11. Inicialização da Aplicação

`DataInitializer.java` (`@Component` + `CommandLineRunner`):
- Verifica se existe usuário admin no banco.
- Se não existir, cria automaticamente:
  - Login: `admin`
  - Senha: `admin123` (BCrypt)
  - Role: `ADMIN`

> **Produção:** Alterar a senha padrão imediatamente após o primeiro deploy.

---

## 12. Configuração de Desenvolvimento Local

### Pré-requisitos

1. Java 21 JDK instalado
2. PostgreSQL 12+ rodando em `localhost:5432`
3. Banco `pascoa_db` criado
4. Maven 3.x

### Passos

```bash
# 1. Criar banco de dados
psql -U postgres -c "CREATE DATABASE pascoa_db;"

# 2. Configurar application.properties com senha do postgres

# 3. Executar a aplicação (Flyway roda migrations automaticamente)
mvn spring-boot:run

# 4. Acessar
http://localhost:8080
# Login: admin / Senha: admin123
```

### Upload de imagens em dev

O diretório `~/pascoa-uploads` é criado automaticamente pelo `WebMvcConfig` se não existir.

---

## 13. Estrutura de Diretórios do Projeto

```
controle_pascoa/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/br/com/seuprojeto/pascoa/
│   │   │   ├── PascoaApplication.java
│   │   │   ├── config/
│   │   │   ├── common/
│   │   │   ├── cadastro/
│   │   │   ├── pedido/
│   │   │   ├── orcamento/
│   │   │   ├── financeiro/
│   │   │   ├── producao/
│   │   │   ├── estoque/
│   │   │   ├── qualidade/
│   │   │   ├── fichaTecnica/
│   │   │   ├── notificacao/
│   │   │   ├── crm/
│   │   │   ├── gastos/
│   │   │   ├── analytics/
│   │   │   ├── catalogo/
│   │   │   ├── pwa/
│   │   │   ├── seguranca/
│   │   │   └── shared/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-test.properties
│   │       ├── db/migration/
│   │       └── templates/
│   └── test/
│       └── java/ (6 testes de integração)
└── docs/          ← documentação do projeto
```
