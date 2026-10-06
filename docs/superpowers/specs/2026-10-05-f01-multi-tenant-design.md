# F0.1 — Multi-tenant (isolamento de dados por loja)

Data: 2026-10-05 · Branch: `feat/backlog-fase1-encomendas` · Origem: [backlog](2026-10-05-backlog-encomendas-design.md)

## Objetivo

Várias lojas no mesmo banco e na mesma aplicação, sem enxergar dados umas das outras. Cadastro self-service e onboarding ficam no F0.2; aqui entra só o isolamento. O sistema atual continua funcionando como a "Loja Padrão" (id 1).

## Decisões

| Tema | Decisão |
|---|---|
| Mecanismo | `@TenantId` do Hibernate 6 (coluna `loja_id`, discriminador). Filtra JPQL, Criteria e `findById`, e preenche `loja_id` no insert |
| Fonte do tenant | `TenantContext` (ThreadLocal) lido por um `CurrentTenantIdentifierResolver` |
| Sem tenant no contexto | O resolver lança `IllegalStateException`. Nunca assume a loja 1 |
| Login | `usuarios.login` continua único globalmente |
| Tabelas fora do tenant | `lojas`, `shedlock` |
| `usuarios` e `password_reset_token` | Coluna `loja_id` explícita, sem `@TenantId` (são lidos antes de existir tenant) |
| Queries nativas | `AND loja_id = :lojaId` manual, com um teste de isolamento por query |
| Jobs agendados | Iteram as lojas e executam dentro de `TenantContext.executar(lojaId, ...)` |
| Endpoints públicos por token | Query nativa resolve o `loja_id` pelo token, seta o contexto e segue com os repositórios normais |

## Componentes

### Banco — `V16__multi_tenant.sql`

1. Cria `lojas(id BIGSERIAL PK, nome VARCHAR(150) NOT NULL, criada_em TIMESTAMP NOT NULL DEFAULT now())`.
2. Insere a loja 1, "Loja Padrão".
3. Em cada tabela abaixo, adiciona `loja_id BIGINT NOT NULL DEFAULT 1 REFERENCES lojas(id)`, cria `idx_<tabela>_loja_id` e depois remove o DEFAULT (insert sem tenant passa a falhar em vez de cair na loja 1).
4. Os UNIQUE de configuração passam a incluir `loja_id` (abaixo).
5. Ajusta a sequência de `lojas` após o insert explícito do id 1.

Tabelas com `loja_id` e `@TenantId`: `alertas_internos`, `audit_log`, `campanha_reengajamento`, `checklist_qualidade`, `clientes`, `configuracao_canal`, `configuracao_financeira`, `configuracao_sistema`, `contas_pagar`, `contas_receber`, `despesas_fixas`, `despesas_variaveis`, `fichas_tecnicas`, `fichas_tecnicas_itens`, `fornecedores`, `gastos_variaveis`, `inspecao_qualidade`, `itens_pedido`, `materias_primas`, `movimentacoes_estoque`, `notas_cliente`, `notificacoes_enviadas`, `orcamento_itens`, `orcamentos`, `orcamentos_gasto`, `ordens_producao`, `pagamentos`, `pedidos`, `pontos_fidelidade`, `produtos`, `templates_notificacao`.

Tabelas com `loja_id` explícito, sem `@TenantId`: `usuarios`, `password_reset_token`.

UNIQUE que viram compostos:
- `configuracao_canal.tipo` → `(loja_id, tipo)`
- `orcamentos_gasto (categoria, referencia_mes, referencia_ano)` → inclui `loja_id`
- `fichas_tecnicas.produto_id` já é único por produto, que pertence a uma loja. Mantém.
- `token_acompanhamento`, `token_aprovacao` e `password_reset_token.token` (UUID) continuam únicos globalmente.
- Índices parciais de idempotência de notificação (`uq_notif_*`) incluem `loja_id`.

`configuracao_financeira` e `configuracao_sistema` hoje funcionam como linha única. Cada loja passa a ter a sua; a criação da linha padrão acontece no F0.2 (nova loja) e na migration (loja 1 já tem as suas).

### Java

- `common/tenant/TenantContext`: `ThreadLocal<Long>`, com `set`, `get` (lança se vazio), `limpar` e `executar(lojaId, Runnable)`.
- `common/tenant/TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long>`, registrado em `spring.jpa.properties.hibernate.tenant_identifier_resolver`.
- `common/entity/TenantEntity` (`@MappedSuperclass`) com `@TenantId @Column(name="loja_id", nullable=false, updatable=false) Long lojaId`. As 31 entidades abaixo herdam dela, direta ou via `BaseEntity`. Entidades que hoje não herdam `BaseEntity` herdam só `TenantEntity`.
- `seguranca/entity/Loja` e `LojaRepository`.
- `seguranca/service/UsuarioPrincipal extends User` com `lojaId`. `UsuarioService.loadUserByUsername` devolve `UsuarioPrincipal`.
- `config/TenantFilter` (`OncePerRequestFilter`), depois da autenticação: lê o `lojaId` do `UsuarioPrincipal` e preenche o `TenantContext`. Limpa em `finally`. Rotas públicas sem token não precisam de tenant (login, estáticos).
- `TwoFactorAuthenticationSuccessHandler` e qualquer ponto que recria o `Authentication` precisam preservar o `UsuarioPrincipal`.
- `UsuarioRepository`: `findAllByLojaIdOrderByNomeAsc`. `UsuarioService.listarTodos`/`buscarPorId` filtram por `loja_id` explícito. Cadastro de usuário grava o `loja_id` do ADMIN logado.
- `DataInitializer`: cria o admin com `loja_id = 1`.
- Públicos: `AcompanhamentoController` e `OrcamentoService.buscarPorToken` resolvem a loja por query nativa (`SELECT loja_id FROM pedidos WHERE token_acompanhamento = :t`) e executam o restante dentro de `TenantContext.executar`. O mesmo para `PasswordResetService` (o token guarda o `loja_id` do usuário).
- Jobs (`CrmService`, `CampanhaService`, `NotificacaoAgendadaService`, `ClienteRepository` via aniversário): um loop sobre `LojaRepository.findAll()` com `TenantContext.executar` por loja. ShedLock fica global, uma execução por job.
- Listeners de evento síncronos herdam o ThreadLocal da requisição. Qualquer `@Async` ou `@TransactionalEventListener(AFTER_COMMIT)` que rode em outra thread precisa propagar o tenant. Verificar na implementação e cobrir com teste.

### Queries nativas a ajustar

| Repositório | Método |
|---|---|
| `PedidoRepository` | `faturamentoPorMes`, `totalPorAno`, `countPorAno`, `anosComPedidos` |
| `ItemPedidoRepository` | `rankingProdutosPorAno` (junta `itens_pedido` com `pedidos`) |
| `PontoFidelidadeRepository` | `saldoPorCliente` |
| `NotificacaoEnviadaRepository` | `jaEnviouAniversarioNoAno` |
| `ClienteRepository` | `findAniversariantesHoje` |

Cada uma ganha `AND loja_id = :lojaId`, e o `lojaId` vem de `TenantContext.get()` no service (não do controller). Em joins nativos, filtrar todas as tabelas envolvidas.

## Fluxo

1. Login → `UsuarioPrincipal(lojaId)` na sessão.
2. Cada requisição autenticada → `TenantFilter` seta o contexto → Hibernate filtra e preenche `loja_id` → `finally` limpa.
3. Rota pública por token → resolve loja pelo token → `executar` → resposta.
4. Job → para cada loja, `executar`.

## Erros

- Acesso a repositório sem tenant: `IllegalStateException("Tenant não definido")`. Vira 500 com log; nunca devolve dados.
- Registro de outra loja por id: `findById` devolve vazio, e o service lança `RecursoNaoEncontradoException` (404). Não revelar que o id existe.

## Testes

- `TenantIsolamentoTest` (H2, dois tenants): para `Cliente`, `Produto`, `Pedido`, `MateriaPrima`, `MovimentacaoEstoque` e `GastoVariavel`, os dados da loja A não aparecem na loja B em `findAll`, `findById` e contagens. Insert grava o `loja_id` do contexto.
- Um teste por query nativa listada: duas lojas com dados, resultado só da loja do contexto.
- Resolver sem contexto lança exceção.
- Login resolve o `UsuarioPrincipal` com `lojaId`; `TenantFilter` preenche e limpa o contexto, inclusive em exceção.
- Token público de pedido e de orçamento resolve a loja certa com duas lojas no banco.
- Job de aniversário roda para cada loja sem vazar clientes entre elas.
- Testes existentes: ganham um `TenantContext` fixo na loja 1 (setup comum), sem mudar asserts. `RolePermissionsTest` não muda neste item.
- Validação da migration: rodar `V16` em cópia do banco do sandbox (seed da massa de teste) e conferir `SELECT COUNT(*) WHERE loja_id IS NULL` = 0 em todas as tabelas.

## Fora do escopo

Cadastro de loja nova e onboarding (F0.2), roles Dono/Equipe (F0.4), uploads por loja em S3 (F0.6; enquanto isso os arquivos ficam num diretório compartilhado, então prefixar por `loja_id` no caminho entra no F0.6), planos e cobrança (F2.5).

## Riscos

- **Esquecer uma query nativa nova:** mitigado pelo teste por query e pela regra de revisão "nativeQuery exige `loja_id`". O `V16` não cobre queries futuras.
- **Threads fora da requisição** (`@Async`, eventos pós-commit): sem propagação, a operação falha com a exceção do resolver. É o comportamento seguro, mas pode quebrar notificações existentes. Cobrir com teste de integração do fluxo de notificação.
- **Sessão serializada com `UsuarioPrincipal`:** sessões abertas antes do deploy trazem o `User` antigo, sem `lojaId`. O `TenantFilter` trata `principal` sem `lojaId` como sessão inválida (invalida e redireciona para `/login`).
- **Migration em tabela grande:** `ADD COLUMN ... DEFAULT 1` no Postgres 16 é metadado (rápido). A criação de 31 índices é o trecho lento; aceitável no volume atual (5k pedidos).
