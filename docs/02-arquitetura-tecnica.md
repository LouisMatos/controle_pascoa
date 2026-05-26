# Documentação de Arquitetura Técnica — Sistema Controle Páscoa

> **Projeto:** Sistema de Gestão de Ovos de Páscoa Artesanal  
> **Versão:** Spring Boot 3.3.4 / Java 21  
> **Atualizado em:** 2026-05-26

---

## 1. Visão Geral da Arquitetura

O sistema segue a arquitetura **MVC em camadas** (Model-View-Controller) do Spring Framework, com separação clara entre:

```
Browser / Cliente HTTP
        │
        ▼
  [Controller Layer]     ← recebe requisições, valida input, delega ao service
        │
        ▼
  [Service Layer]        ← lógica de negócio, transações, eventos
        │
        ▼
  [Repository Layer]     ← acesso a dados via Spring Data JPA
        │
        ▼
  [Database - PostgreSQL]
```

**Padrões aplicados:**
- MVC (Spring Web MVC)
- Service Layer (lógica de negócio isolada)
- Repository/DAO (Spring Data JPA)
- DTO (objetos de transferência Controller ↔ View)
- Event-Driven (Spring ApplicationEventPublisher para notificações)
- Soft-delete (Hibernate `@SQLDelete` + `@SQLRestriction`)
- Spring Data Auditing (rastreabilidade de criação/alteração)

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
- Configura beans de infraestrutura geral (ex: `RestTemplate` para chamadas ao WhatsApp).

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
campos: id, nome, login (único), senha (BCrypt), role (enum), ativo
```

**Role.java (enum)**
```
ADMIN, FINANCEIRO, ATENDENTE, CONFEITEIRO, GESTOR_QUALIDADE, ANALISTA
```

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `UsuarioController` | CRUD de usuários via `/usuarios` (ADMIN only) |
| `UsuarioService` | Implementa `UserDetailsService`, gerencia usuários |
| `UsuarioRepository` | `findByLogin(String login)` para autenticação |

**DTOs:** `UsuarioForm` (nome, login, senha, role)

---

## 6. Módulo de Cadastro (`cadastro/`)

### Entidades

**Cliente.java** (soft-delete)
```
campos: id, nome, email, telefone, CPF, endereço, 
        preferenciaCanal (enum), optIn (boolean), ativo
```
- `@SQLDelete(sql="UPDATE clientes SET deletado=true WHERE id=?")` — nunca deletado fisicamente.
- `@SQLRestriction("deletado=false")` — filtro automático em todas as queries.

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
| `PreferenciaCanal` | EMAIL, WHATSAPP |

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
| `CrmController` | Perfil de cliente, histórico de pedidos, notas, segmentação |
| `CrmService` | Calcula LTV, segmenta clientes, gerencia notas e pontos |
| `NotaClienteRepository`, `PontoFidelidadeRepository` | Queries por cliente |

**DTOs:** `ClienteCrmDto` (LTV, total pedidos, último pedido, segmento)

---

## 15. Módulo de Notificações (`notificacao/`)

### Entidades

**TemplateNotificacao.java**
```
campos: id, evento (enum), canal (enum), assunto, corpo, ativo
```
Corpo usa `{{variavel}}` como placeholders substituídos em runtime.

**ConfiguracaoCanal.java**
```
campos: id, canal (enum), ativo, testMode, configuracoes (JSON/Map)
```

**NotificacaoEnviada.java**
```
campos: id, cliente, evento, canal, status (enum), dataEnvio, conteudo, erro
```

**AlertaInterno.java**
```
campos: id, titulo, mensagem, tipo, lido, dataCriacao, destinatario
```

### Enums

| Enum | Valores |
|------|---------|
| `EventoNotificacao` | PEDIDO_CONFIRMADO, PRODUCAO_INICIADA, PEDIDO_PRONTO, PEDIDO_ENTREGUE, PAGAMENTO_RECEBIDO, PEDIDO_CANCELADO, ORCAMENTO_APROVADO, ORCAMENTO_RECUSADO |
| `CanalNotificacao` | EMAIL, WHATSAPP |
| `StatusEnvio` | ENVIADO, FALHA, PENDENTE |

### Classes

| Classe | Responsabilidade |
|--------|-----------------|
| `NotificacaoService` | Orquestra envios: busca template, substitui variáveis, delega a `EmailService`/`WhatsAppService` |
| `EmailService` | Envio via JavaMailSender (SMTP Gmail) |
| `WhatsAppService` | Envio via HTTP para Evolution API |
| `AlertaInternoService` | Cria/lista alertas para usuários do sistema |
| `NotificacaoController` | Configuração de canais, templates, histórico de envios |
| `AlertaInternoController` | Visualização e marcação de alertas como lidos |

### Arquitetura Event-Driven

```
[PedidoService]  ──► publishEvent(PedidoStatusEvent)
                            │
                            ▼
                 [NotificacaoEventListener]
                   ├── NotificacaoService (email/WhatsApp)
                   └── AlertaInternoService (alertas UI)
```

**Eventos publicados:**

| Evento | Publicado quando |
|--------|-----------------|
| `PedidoStatusEvent` | Status do pedido muda (confirmado, pronto, entregue, etc.) |
| `OrcamentoAcaoEvent` | Orçamento é aprovado ou recusado |
| `InspecaoReprovadaEvent` | Inspeção de qualidade reprova uma ordem |

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
