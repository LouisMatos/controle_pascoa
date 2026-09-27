# Documentação de Arquitetura Técnica — Sistema Controle Páscoa

> **Projeto:** Sistema de Gestão de Ovos de Páscoa Artesanal  
> **Versão:** Spring Boot 3.3.4 / Java 21 / Spring Cloud 2023.0.3  
> **Atualizado em:** 2026-05-29

---

## 1. Visão Geral da Arquitetura

O sistema possui **duas camadas arquiteturais coexistentes** (padrão Strangler Fig):

### 1.1 Monólito (`pascoa-monolith`) — MVC em camadas

```
Browser / Thymeleaf
        │
        ▼
  [Controller Layer]     ← recebe requisições, valida input, delega ao service
        │
        ▼
  [Service Layer]        ← lógica de negócio, transações, eventos Spring
        │
        ▼
  [Repository Layer]     ← acesso a dados via Spring Data JPA
        │
        ▼
  [PostgreSQL — pascoa_monolith]
```

**Padrões do monólito:**
- MVC (Spring Web MVC + Thymeleaf)
- Service Layer (lógica de negócio isolada)
- Repository/DAO (Spring Data JPA)
- DTO (objetos de transferência Controller ↔ View)
- Event-Driven interno (Spring `ApplicationEventPublisher`)
- Soft-delete (Hibernate `@SQLDelete` + `@SQLRestriction`)
- Spring Data Auditing (rastreabilidade automática)

### 1.2 Microsserviços v5 — Arquitetura Hexagonal (Ports & Adapters)

Cada microsserviço segue a estrutura:

```
[REST / RabbitMQ Consumer]  ← adapter IN
          │
          ▼
  [Application — Use Cases] ← lógica de negócio (sem Spring)
          │
     ports/in  ports/out
          │
          ▼
  [Domain Model]            ← entidades, VOs, exceções — ZERO dependências externas
          │
          ▼
[JPA / Redis / RabbitMQ]    ← adapters OUT
```

**Regra de ouro:** O domain **nunca** importa Spring, JPA ou qualquer framework.  
O use case **nunca** conhece HTTP, mensageria ou banco — só as interfaces de port.

**Padrões dos microsserviços:**
- Hexagonal Architecture (Ports & Adapters)
- Domain-Driven Design (bounded contexts isolados)
- Event-Driven assíncrono (RabbitMQ topic exchanges)
- CQRS leve (read/write separados nos use cases)
- Idempotência via `eventId` + banco ou Redis
- Strangler Fig (migração gradual do monólito)
- Stateless + JWT (cada serviço valida o token independentemente)

---

## 2. Pacote Raiz e Módulos

Pacote base: `br.com.seuprojeto.pascoa`

Cada módulo de negócio é um **sub-pacote independente** contendo suas próprias camadas:

```
br.com.seuprojeto.pascoa/
├── PascoaApplication.java          ← entry point (@SpringBootApplication)
├── config/                         ← configurações transversais
├── common/                         ← classes base compartilhadas
├── shared/                         ← exceções e handlers globais
├── seguranca/                      ← autenticação e usuários
│
├── cadastro/                       ← módulo de cadastros base
├── pedido/                         ← módulo de pedidos
├── orcamento/                      ← módulo de orçamentos
├── producao/                       ← módulo de produção
├── qualidade/                      ← módulo de qualidade
├── estoque/                        ← módulo de estoque
├── fichaTecnica/                   ← módulo de fichas técnicas
├── financeiro/                     ← módulo financeiro
├── crm/                            ← módulo de relacionamento com cliente
├── notificacao/                    ← módulo de notificações
├── gastos/                         ← módulo de controle de gastos
├── analytics/                      ← módulo de relatórios e análises
├── catalogo/                       ← catálogo público de produtos
└── pwa/                            ← suporte Progressive Web App
```

---

## 3. Camada de Configuração (`config/`)

### AppConfig.java
- Configura beans de infraestrutura geral (`RestTemplate` usado por `WhatsAppService` e `SmsService`).

### SecurityConfig.java
- Configura Spring Security 6:
  - Rotas públicas e protegidas
  - Formulário de login (`/login`)
  - Encoder de senha BCrypt
  - Integração com `UsuarioService` (UserDetailsService)

### WebMvcConfig.java
- Configura handler de recursos estáticos para uploads:
  ```java
  registry.addResourceHandler("/uploads/**")
          .addResourceLocations("file:" + uploadDir + "/");
  ```
- Garante que imagens em `~/pascoa-uploads` sejam servidas via HTTP.

### AuditorAwareImpl.java
- Implementa `AuditorAware<String>` para Spring Data Auditing.
- Retorna o nome do usuário autenticado (`SecurityContextHolder`).
- Alimenta os campos `criadoPor` e `atualizadoPor` em `BaseEntity`.

### DataInitializer.java
- `CommandLineRunner` que cria o usuário `admin` se nenhum usuário existir.

### MaintenanceFilter.java
- `OncePerRequestFilter` que intercepta todas as requisições.
- Se `ConfiguracaoSistema.modoManutencao = true`, redireciona para `/manutencao` (exceto ADMIN e rotas de login).

### ManutencaoController.java / SistemaController.java
- `GET /manutencao` — página de manutenção pública.
- `GET/POST /admin/sistema` — ADMIN ativa/desativa modo manutenção e edita mensagem.

### WhatsAppHealthIndicator.java
- Implementa `HealthIndicator` do Spring Boot Actuator.
- Verifica conectividade com a Evolution API e reporta no `/actuator/health`.

---

## 4. Classe Base de Entidades (`common/`)

### BaseEntity.java
Superclasse abstrata (`@MappedSuperclass`) herdada por todas as entidades JPA:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime criadoEm;

    @LastModifiedDate
    private LocalDateTime atualizadoEm;

    @CreatedBy
    private String criadoPor;

    @LastModifiedBy
    private String atualizadoPor;
}
```

- Fornece rastreabilidade automática em todas as entidades do sistema.

---

## 5. Módulo de Segurança (`seguranca/`)

### Entidades

**Usuario.java**
```
campos: id, nome, login (único), senha (BCrypt), email (V11 — para reset), role (enum), ativo
```

**Role.java (enum)**
```
ADMIN, FINANCEIRO, ATENDENTE, CONFEITEIRO, GESTOR_QUALIDADE, ANALISTA
```

**PasswordResetToken.java** (V11)
```
campos: id, usuario, token (UUID), expiraEm, usado, criadoEm
```
Token de uso único, válido por 30 minutos, para recuperação de senha via e-mail.

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `UsuarioController` | CRUD de usuários via `/usuarios` (ADMIN only) |
| `UsuarioService` | Implementa `UserDetailsService`, gerencia usuários |
| `UsuarioRepository` | `findByLogin(String login)` para autenticação |
| `PasswordResetService` | Gera token, envia e-mail com link, valida e aplica reset |
| `PasswordResetTokenRepository` | `findByToken(String)` para validação |

**DTOs:** `UsuarioForm` (nome, login, senha, role)

---

## 6. Módulo de Cadastro (`cadastro/`)

### Entidades

**Cliente.java** (soft-delete)
```
campos: id, nome, email, telefone, CPF, endereço,
        preferenciaCanal (enum), optIn (boolean),
        dataConsentimento (LGPD — V9), anonimizado (LGPD — V9),
        segmento (SegmentoCliente — recalculado diariamente pelo job às 02h — V13),
        dataNascimento (LocalDate — para job de aniversário às 08h — V14),
        excluidoEm (soft-delete)
```
- `@SQLDelete(sql="UPDATE clientes SET excluido_em = NOW() WHERE id=?")` — nunca deletado fisicamente.
- `@SQLRestriction("excluido_em IS NULL")` — filtro automático em todas as queries.
- `@AttributeOverride(name="criadoEm", column=@Column(name="data_cadastro"))` — mapeia herança de BaseEntity para coluna legada.

**Produto.java** (soft-delete)
```
campos: id, nome, descricao, categoria (enum), precoVenda, 
        margemDesejada, pesoGramas, dataInicioSafra, dataFimSafra, fotoUrl, ativo
```

**Fornecedor.java**
```
campos: id, nome, CNPJ, telefone, email, contato
```

**MateriaPrima.java**
```
campos: id, nome, unidade (enum), quantidadeAtual, quantidadeMinima,
        custoUnitario, custoMedioUnitario, fornecedorPreferencial
```

### Enums

| Enum | Valores |
|------|---------|
| `Categoria` | TRUFADO, RECHEADO, DIET, VEGANO, TRADICIONAL, ESPECIAL |
| `Unidade` | KG, G, L, ML, UN, CX |
| `PreferenciaCanal` | NENHUM, EMAIL, WHATSAPP, AMBOS |
| `SegmentoCliente` | NOVO, VIP, FREQUENTE, INATIVO |

### Classes por entidade (padrão)

Cada entidade segue o padrão:
```
{Entidade}Controller   → @Controller, @RequestMapping, trata GET/POST
{Entidade}Service      → @Service, @Transactional, lógica de negócio
{Entidade}Repository   → extends JpaRepository<Entidade, Long>
```

**Dashboard:** `DashboardController` monta KPIs para `GET /` e `GET /dashboard`.

---

## 7. Módulo de Pedidos (`pedido/`)

### Entidades

**Pedido.java**
```
campos: id, cliente, itens, status, dataEntrega, dataCriacao,
        tokenAcompanhamento (UUID), slotEntrega, custoReal, margem,
        observacoes, pagamentos
```

**ItemPedido.java**
```
campos: id, pedido, produto, quantidade, precoUnitario (fixado no momento), subtotal
```

**Pagamento.java**
```
campos: id, pedido, tipo (enum), valor, data, observacoes
```

### Enums

| Enum | Valores |
|------|---------|
| `StatusPedido` | NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE (ou CANCELADO) |
| `TipoPagamento` | PIX, DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, FIADO |

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `PedidoController` | CRUD, máquina de estados, itens dinâmicos, wizard, pagamentos |
| `PedidoService` | Transições de status, cálculo de custo/margem, adição/remoção de itens, pagamentos |
| `PedidoRepository` | Queries: faturamento por mês/ano, estatísticas por cliente, busca por status |
| `ItemPedidoRepository` | Queries customizadas de itens por pedido |
| `ExportService` | Exportação de dados do pedido para Excel/PDF |

**DTOs:** `PedidoForm`, `ItemPedidoForm`, `PagamentoForm`

### Máquina de Estados

```
NOVO ──► CONFIRMADO ──► EM_PRODUCAO ──► PRONTO ──► ENTREGUE
  └─────────────────────────────────────────────► CANCELADO
```

Cada transição é um endpoint POST:
- `POST /pedidos/{id}/confirmar`
- `POST /pedidos/{id}/iniciar-producao`
- `POST /pedidos/{id}/marcar-pronto`
- `POST /pedidos/{id}/registrar-entrega`
- `POST /pedidos/{id}/cancelar`

---

## 8. Módulo de Orçamentos (`orcamento/`)

### Entidades

**Orcamento.java**
```
campos: id, cliente, itens, status, validade, total,
        tokenAprovacao (UUID), pedidoVinculado, observacoes
```

**OrcamentoItem.java**
```
campos: id, orcamento, produto, quantidade, precoUnitario, subtotal
```

### Enum `StatusOrcamento`
```
PENDENTE → APROVADO
         → RECUSADO
         → EXPIRADO
```

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `OrcamentoController` | CRUD, detalhe, geração de PDF, conversão em pedido |
| `OrcamentoPublicoController` | Aprovação/recusa via token (sem autenticação) |
| `OrcamentoService` | Criação, cálculo de total, conversão em Pedido |
| `OrcamentoPdfService` | Geração de PDF com OpenPDF |
| `OrcamentoRepository` | `findByTokenAprovacao`, filtros por status |

**DTOs:** `OrcamentoForm`, `OrcamentoItemForm`

---

## 9. Módulo de Produção (`producao/`)

### Entidades

**OrdemProducao.java**
```
campos: id, pedido, produto, quantidade, status,
        dataCriacao, dataInicio, dataConclusao, observacoes
```

### Enum `StatusOrdem`
```
PENDENTE → EM_ANDAMENTO → CONCLUIDA
```

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `ProducaoController` | Kanban (`/producao/kanban`), fila, detalhes |
| `ProducaoService` | Criação de ordens, transições de status |
| `OrdemProducaoRepository` | Queries por status, por pedido |

---

## 10. Módulo de Qualidade (`qualidade/`)

### Entidades

**InspecaoQualidade.java**
```
campos: id, ordemProducao, inspetor (usuario), aprovado (boolean),
        dataInspecao, observacoes, itensVerificados (JSONB)
```

- `itensVerificados` é uma coluna **JSONB** no PostgreSQL — armazena lista dinâmica de itens do checklist.

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `QualidadeController` | Lista inspeções, formulário de inspeção, aprovação/reprovação |
| `QualidadeService` | Persistência da inspeção, publicação de `InspecaoReprovadaEvent` |
| `InspecaoQualidadeRepository` | Queries por ordem, por inspetor |

**DTOs:** `ItemVerificadoDto` (representa um item do checklist com nome e status)

---

## 11. Módulo de Estoque (`estoque/`)

### Entidades

**MovimentacaoEstoque.java**
```
campos: id, materiaPrima, tipo (enum), quantidade, saldoApos,
        custoUnitario, data, observacoes, responsavel
```

### Enum `TipoMovimentacao`
```
ENTRADA, SAIDA, AJUSTE
```
Cada valor tem um `cssClass` associado para badge colorido na UI.

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `EstoqueController` | Telas de entrada, saída, ajuste, movimentações |
| `EstoqueService` | Calcula saldo atual, registra movimentações, atualiza `quantidadeAtual` em `MateriaPrima` |
| `MovimentacaoEstoqueRepository` | Histórico filtrado por matéria-prima e período |

**DTOs:** `EntradaEstoqueForm`, `AjusteEstoqueForm`

---

## 12. Módulo de Ficha Técnica (`fichaTecnica/`)

### Entidades

**FichaTecnica.java**
```
campos: id, produto (1:1), rendimento, unidadeRendimento (enum), itens
```

**FichaTecnicaItem.java**
```
campos: id, fichaTecnica, materiaPrima, quantidade, unidade (enum)
```

Relação: `Produto 1 ↔ 1 FichaTecnica → N FichaTecnicaItem → MateriaPrima`

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `FichaTecnicaController` | CRUD fichas técnicas com itens |
| `FichaTecnicaService` | Persistência, cálculo de custo de produção via ficha |
| `FichaTecnicaRepository` | `findByProdutoId` |

---

## 13. Módulo Financeiro (`financeiro/`)

### Entidades

**ContaReceber.java**
```
campos: id, pedido, valorOriginal, valorPago, vencimento, status (enum), observacoes
```

**ContaPagar.java**
```
campos: id, fornecedor, descricao, valor, vencimento, status (enum),
        categoria (enum), observacoes
```

**DespesaFixa.java**
```
campos: id, descricao, valor, periodicidade (enum), ativo
```

**DespesaVariavel.java**
```
campos: id, descricao, valor, mes, ano
```

**ConfiguracaoFinanceira.java**
```
campos: id, metaFaturamentoMensal
```

### Enums

| Enum | Valores |
|------|---------|
| `StatusConta` | ABERTA, PAGA, VENCIDA |
| `CategoriaContaPagar` | (categorias de despesas operacionais) |
| `Periodicidade` | MENSAL, ANUAL, CUSTOMIZADA |

### Services e DTOs

| Service | DTO associado | Funcionalidade |
|---------|--------------|----------------|
| `FinanceiroService` | `RelatorioFinanceiro` | Consolidação de receitas, despesas, lucro |
| `CustoRealService` | `CustoRealDto` | Custo de produção por pedido via FichaTécnica |
| `FluxoCaixaService` | `FluxoCaixaDto` | Projeção de entradas/saídas por período |
| `BreakevenService` | `BreakevenDto` | Ponto de equilíbrio e projeção de safra |

**DTOs financeiros:**

| DTO | Campos |
|-----|--------|
| `CustoRealDto` | custoVariavel, custoFixo, total, margem |
| `FluxoCaixaDto` | periodo, receitas, despesas, saldo, projecao |
| `BreakevenDto` | pontoEquilibrioUnidades, pontoEquilibrioValor, margemContribuicao |
| `RelatorioFinanceiro` | faturamento, custos, lucro, margemLucro, inadimplencia |
| `AgingDto` | bucket0a30, bucket30a60, bucket60a90, bucket90mais |
| `MargemProdutoDto` | produto, receita, custo, margem |
| `TopProdutoDto` | produto, quantidade, receita |

### Controller

`FinanceiroController` expõe:
- `GET /financeiro/dashboard` — visão consolidada
- `GET /financeiro/custo-real` — custo por pedido
- `GET /financeiro/fluxo-caixa` — fluxo com filtro de período
- `GET /financeiro/breakeven` — análise de ponto de equilíbrio
- `GET /financeiro/projecao-safra` — projeção de vendas/lucro
- `GET /financeiro/aging` — análise de vencimento de contas
- `GET /financeiro/contas-pagar` — gestão de contas a pagar

---

## 14. Módulo de CRM (`crm/`)

### Entidades

**NotaCliente.java**
```
campos: id, cliente, texto, dataCriacao, autor
```

**PontoFidelidade.java**
```
campos: id, cliente, tipo (enum), valor, data, descricao
```

### Enums

| Enum | Valores |
|------|---------|
| `SegmentoCliente` | (segmentações como VIP, FREQUENTE, NOVO, INATIVO) |
| `TipoPonto` | GANHO, RESGATADO |

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `CrmController` | Perfil de cliente, histórico de pedidos, notas, segmentação, ranking |
| `CrmService` | Calcula LTV, `gerarRanking()` para campanhas, gerencia notas e pontos |
| `CampanhaService` | Dispara campanhas em lote via `CampanhaQueue` (rate-limited: 10 envios/min); `@Scheduled` worker com ShedLock |
| `NotaClienteRepository`, `PontoFidelidadeRepository` | Queries por cliente |

> **Job de Segmentação:** `CrmService.recalcularSegmentos()` roda às 02h via `@Scheduled` com ShedLock, atualiza `clientes.segmento` para todos os clientes (VIP/FREQUENTE/INATIVO/NOVO) e persiste em batch.

**DTOs:** `ClienteCrmDto` (LTV, total pedidos, último pedido, segmento)

---

## 15. Módulo de Notificações (`notificacao/`)

### Entidades

**TemplateNotificacao.java**
```
campos: id, evento (enum), canal (enum), assunto, corpo, ativo, variaveis (documentação)
```
Corpo usa `{variavel}` como placeholders substituídos em runtime por `NotificacaoService`.

**ConfiguracaoCanal.java**
```
campos: id, canal (enum), ativo, testMode, configuracoes (JSON/Map)
```

**NotificacaoEnviada.java**
```
campos: id, pedido (nullable — V14), cliente (nullable — V14), orcamento (nullable — V14),
        evento (V12), canal, status (enum), dataEnvio, destinatario, mensagemErro, templateId
```
- `pedido` é nullable desde V14 para suportar notificações sem contexto de pedido.
- `cliente` e `orcamento` são FKs opcionais adicionadas em V14.

**AlertaInterno.java**
```
campos: id, mensagem, link, icone, cor, lido, criadoEm
```

### Enums

| Enum | Valores |
|------|---------|
| `EventoNotificacao` | PEDIDO_CONFIRMADO, PRODUCAO_INICIADA, PEDIDO_PRONTO, PEDIDO_ENTREGUE, PAGAMENTO_RECEBIDO, PEDIDO_CANCELADO, ORCAMENTO_APROVADO, ORCAMENTO_RECUSADO, **ANIVERSARIO_CLIENTE** (V14), **ORCAMENTO_EXPIRANDO** (V14) |
| `CanalNotificacao` | EMAIL, WHATSAPP, **SMS** (V14 — fallback automático quando WhatsApp falha) |
| `StatusEnvio` | ENVIADA, FALHA |

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `NotificacaoService` | Orquestra envios: 3 entry-points (`processar`, `processarParaCliente`, `processarParaOrcamento`), substitui variáveis, delega ao canal correto, persiste `NotificacaoEnviada` |
| `EmailService` | Envio via JavaMailSender (SMTP Gmail) |
| `WhatsAppService` | Envio via HTTP para Evolution API; falha dispara SMS fallback |
| `SmsService` | **V14** — Envio via HTTP POST para webhook SMS (Twilio, Zenvia, AWS SNS ou compatível). `formatarNumero()` normaliza para `+55XXXXXXXXXXX` |
| `NotificacaoAgendadaService` | **V14** — Jobs proativos: `notificarAniversariantes()` às 08h e `notificarOrcamentosExpirando()` às 09h, ambos com ShedLock |
| `AlertaInternoService` | Cria/lista alertas para usuários do sistema |
| `NotificacaoController` | Configuração de canais, templates, histórico de envios, teste de conexão WhatsApp |
| `AlertaInternoController` | Visualização e marcação de alertas como lidos |

### Arquitetura Event-Driven

```
[PedidoService]  ──► publishEvent(PedidoStatusEvent)
                            │
                            ▼
                 [NotificacaoEventListener]
                   ├── NotificacaoService.processar(pedido, evento)
                   │     ├── EmailService
                   │     ├── WhatsAppService ──(falha)──► SmsService (fallback V14)
                   │     └── persiste NotificacaoEnviada
                   └── AlertaInternoService (alertas UI)

[NotificacaoAgendadaService] ──► (V14: jobs proativos)
  ├── notificarAniversariantes() @08h
  │     └── NotificacaoService.processarParaCliente(cliente, ANIVERSARIO_CLIENTE)
  └── notificarOrcamentosExpirando() @09h
        └── NotificacaoService.processarParaOrcamento(orcamento, ORCAMENTO_EXPIRANDO)
```

**Eventos publicados via Spring ApplicationEventPublisher:**

| Evento | Publicado quando |
|--------|-----------------|
| `PedidoStatusEvent` | Status do pedido muda (confirmado, pronto, entregue, etc.) |
| `OrcamentoAcaoEvent` | Orçamento é aprovado ou recusado |
| `InspecaoReprovadaEvent` | Inspeção de qualidade reprova uma ordem |

**Notificações proativas (sem evento Spring — disparadas diretamente por `@Scheduled`):**

| Job | Horário | EventoNotificacao |
|-----|---------|-------------------|
| `notificarAniversariantes` | 08h diário | `ANIVERSARIO_CLIENTE` |
| `notificarOrcamentosExpirando` | 09h diário | `ORCAMENTO_EXPIRANDO` (2 dias antes) |

---

## 16. Módulo de Gastos (`gastos/`)

### Entidades

**GastoVariavel.java**
```
campos: id, descricao, valor, data, categoria (enum), mes, ano
```

**OrcamentoGasto.java**
```
campos: id, categoria (enum), valorOrcado, mes, ano
```

### Enum `CategoriaGasto`
```
INGREDIENTES, EMBALAGENS, MARKETING, LOGISTICA, OUTROS, (etc.)
```

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `GastoVariavelController` | Lista, formulário, importação de CSV/Excel |
| `GastoVariavelService` | CRUD, leitura de CSV/Excel via Apache POI |
| `GastoVariavelRepository`, `OrcamentoGastoRepository` | Queries por mês/ano/categoria |

**DTOs:** `GastoVariavelForm`, `GastoDashboardDto`, `OrcamentoGastoForm`

---

## 17. Módulo de Analytics (`analytics/`)

Sem entidades próprias — consume dados dos outros módulos.

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `AnalyticsController` | Dashboard de analytics (`/analytics/dashboard`) |
| `AnalyticsService` | Agrega dados de pedidos, produtos e safras para relatórios |

**DTOs:**

| DTO | Conteúdo |
|-----|----------|
| `SafraDto` | Dados de uma safra (ano, faturamento, quantidade, margem) |
| `ComparativoSafraDto` | Comparação entre safras |
| `RankingProdutoDto` | Produto + posição + receita + quantidade |
| `MesDto` | Estatísticas mensais (faturamento, pedidos, ticket médio) |

---

## 18. Módulos Auxiliares

### Catálogo (`catalogo/`)
- `CatalogoController` — serve páginas públicas de catálogo de produtos.
- Não requer autenticação (rota pública configurada no `SecurityConfig`).

### PWA (`pwa/`)
- `PwaController` — serve o `manifest.json` e `sw.js` para suporte a Progressive Web App.

---

## 19. Tratamento de Erros (`shared/`)

### GlobalExceptionHandler.java
`@ControllerAdvice` que intercepta exceções e retorna páginas de erro amigáveis:

| Exceção | Handler |
|---------|---------|
| `RecursoNaoEncontradoException` | 404 com mensagem |
| `EstoqueInsuficienteException` | Erro de negócio (400) |
| `Exception` (genérico) | Página de erro 500 |

---

## 20. Diagrama de Dependências entre Módulos

```
[seguranca] ◄── todos os módulos (autenticação/auditoria)

[cadastro]
  ├── alimenta ──► [pedido]
  ├── alimenta ──► [orcamento]
  ├── alimenta ──► [estoque]
  └── alimenta ──► [fichaTecnica]

[pedido]
  ├── dispara ──► [producao] (cria OrdemProducao)
  ├── dispara ──► [notificacao] (eventos de status)
  └── alimenta ──► [financeiro] (ContasReceber)

[orcamento]
  ├── dispara ──► [notificacao] (eventos de aprovação)
  └── converte ──► [pedido]

[producao]
  └── alimenta ──► [qualidade] (InspecaoQualidade)

[fichaTecnica]
  └── alimenta ──► [financeiro] (CustoRealService)

[estoque]
  └── alimenta ──► [financeiro] (custo de matérias-primas)

[gastos]
  └── alimenta ──► [financeiro] (FluxoCaixaService)

[pedido] + [financeiro]
  └── alimentam ──► [analytics]

[pedido] + [notificacao]
  └── alimentam ──► [crm]
```

---

## 21. Convenções de Código

| Aspecto | Convenção |
|---------|-----------|
| Nomenclatura | camelCase para variáveis/métodos, PascalCase para classes |
| Injeção de dependência | `@RequiredArgsConstructor` (Lombok) + campos `final` |
| Transações | `@Transactional` nos services |
| Queries customizadas | `@Query` nos repositories (JPQL ou SQL nativo) |
| Validação de input | Bean Validation (`@NotBlank`, `@NotNull`, `@Valid`) nos controllers |
| Erros de negócio | Exceções customizadas em `shared/` |
| Soft-delete | `@SQLDelete` + `@SQLRestriction` no Hibernate 6 |
| Auditoria | Herança de `BaseEntity` em todas as entidades |

---

## 22. Estrutura Interna Padrão de um Módulo

Cada módulo segue a estrutura:

```
{modulo}/
├── controller/
│   └── {Modulo}Controller.java       → @Controller, endpoints HTTP
├── service/
│   └── {Modulo}Service.java          → @Service, @Transactional, lógica
├── repository/
│   └── {Modulo}Repository.java       → extends JpaRepository
├── entity/
│   └── {Entidade}.java               → @Entity, @Table, campos JPA
└── dto/
    └── {Modulo}Form.java             → formulários de entrada
    └── {Modulo}Dto.java              → objetos de saída para view
```

> Exceções: módulos simples podem ter tudo em um único sub-pacote.

---

## 14. Arquitetura Hexagonal — Microsserviços v5

Cada microsserviço segue a estrutura abaixo. O pacote base é `br.com.seuprojeto.pascoa.{servico}`.

### 14.1 Estrutura de Pacotes

```
{servico}/
├── {Servico}Application.java              ← @SpringBootApplication
│
├── domain/                                ← ZERO dependências de framework
│   ├── model/
│   │   ├── {Entidade}.java               ← @Builder + @With (Lombok) — imutável
│   │   └── {Enum}.java                   ← enums de estado com tabela de transições
│   ├── exception/
│   │   └── {Entidade}NotFoundException.java
│   └── service/                           ← domain services (lógica pura)
│
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── {Entidade}UseCase.java    ← interface + records de Command/Query
│   │   └── out/
│   │       ├── {Entidade}RepositoryPort.java
│   │       └── {Entidade}EventPublisherPort.java
│   └── usecase/
│       └── {Entidade}UseCaseImpl.java    ← @Service @Transactional, implementa port/in
│
├── adapter/
│   ├── in/
│   │   └── rest/
│   │       ├── {Entidade}Controller.java ← @RestController, delega ao UseCase
│   │       └── dto/                      ← Request + Response records
│   └── out/
│       ├── persistence/
│       │   ├── {Entidade}JpaEntity.java  ← @Entity separado do domain model
│       │   ├── {Entidade}JpaRepository.java ← extends JpaRepository
│       │   ├── {Entidade}Mapper.java     ← @Mapper MapStruct (domain ↔ JPA)
│       │   └── {Entidade}RepositoryAdapter.java ← implementa RepositoryPort
│       └── messaging/
│           ├── {Entidade}EventPublisherAdapter.java ← RabbitTemplate
│           └── {Servico}EventConsumer.java          ← @RabbitListener + idempotência
│
└── config/
    ├── JwtAuthFilter.java                ← OncePerRequestFilter stateless
    ├── SecurityConfig.java               ← STATELESS, @EnableMethodSecurity
    └── RabbitConfig.java                 ← Queues, Exchanges, Bindings, DLX
```

### 14.2 Regras Invioláveis

| # | Regra | Motivo |
|---|-------|--------|
| 1 | Domain **nunca** importa Spring, JPA, Lombok `@Data` | Testabilidade pura |
| 2 | Use case **nunca** conhece HTTP nem RabbitMQ | Só fala com ports |
| 3 | Adapter IN (`Controller`) **nunca** acessa repository diretamente | Só via use case |
| 4 | JPA Entity é **separada** do domain model | Domain imutável, JPA mutável |
| 5 | Idempotência obrigatória em todo `@RabbitListener` | Garantia at-least-once |
| 6 | `@Transactional` apenas no use case (nunca no adapter) | Consistência de fronteira |

### 14.3 Consumer com Idempotência (padrão)

```java
@RabbitListener(queues = "#{T(Config).QUEUE_NAME}")
public void onEvent(Map<String, Object> event) {
    String eventId = (String) event.get("eventId");
    if (eventId == null || !processados.add(eventId)) return; // in-memory
    // ou: if (repository.existsByEventId(eventId)) return;   // via banco
    // processar...
}
```

### 14.4 JWT Stateless (padrão em todos os serviços)

Todos os microsserviços validam o JWT **localmente** (sem chamar o auth-service):

```java
// JwtAuthFilter.java — padrão copiado em cada serviço
Claims claims = Jwts.parser()
    .verifyWith(secretKey)    // chave compartilhada via Config Server
    .build()
    .parseSignedClaims(token)
    .getPayload();
List<String> roles = claims.get("roles", List.class);
// → authorities → SecurityContextHolder
```

> A chave `auth.jwt.secret` é compartilhada via `pascoa-config-server`. Em produção usar variável de ambiente `JWT_SECRET`.

### 14.5 Domain Imutável com `@With` (Lombok)

```java
@Getter @Builder @With
public class Pedido {
    private final Long id;
    private final StatusPedido status;
    // ...

    // Transição imutável — retorna nova instância
    public Pedido transicionarPara(StatusPedido novoStatus) {
        if (!status.podeTransicionarPara(novoStatus))
            throw new TransicaoInvalidaException(id, status, novoStatus);
        return this.withStatus(novoStatus);
    }
}
```

