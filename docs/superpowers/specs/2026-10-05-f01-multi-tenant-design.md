# F0.1 — Multi-tenant (isolamento de dados por loja)

Data: 2026-10-05 · Branch: `feat/backlog-fase1-encomendas` · Origem: [backlog](2026-10-05-backlog-encomendas-design.md)

## Objetivo

Várias lojas no mesmo banco e na mesma aplicação, sem enxergar dados umas das outras. Cadastro self-service e onboarding ficam no F0.2; aqui entra só o isolamento. O sistema atual continua funcionando como a "Loja Padrão" (id 1).

## Decisões

| Tema | Decisão |
|---|---|
| Mecanismo | `@TenantId` do Hibernate 6 (coluna `loja_id`, discriminador). Filtra JPQL e Criteria (o Hibernate 6.5 não filtra `find`/`getReference`/`merge`: o `TenantAwareRepository` cobre `findById`/`getReferenceById`/`save`/`delete`) e preenche `loja_id` no insert |
| Fonte do tenant | `TenantContext` (ThreadLocal) lido por um `CurrentTenantIdentifierResolver` |
| Sem tenant no contexto | O resolver devolve o sentinela `0L` (nenhuma loja existe com esse id): leituras voltam vazias. Escrita falha: `TenantEntity.@PrePersist` lança `IllegalStateException`. Nunca assume a loja 1 |
| Login | `usuarios.login` continua único globalmente |
| Tabelas fora do tenant | `lojas`, `shedlock`, `configuracao_sistema` (modo de manutenção é da plataforma, não da loja) |
| `usuarios` | Coluna `loja_id` explícita, sem `@TenantId` (é lido no login, antes de existir tenant). `password_reset_token` não ganha coluna: o token aponta para o usuário, que carrega a loja |
| Config da plataforma | `/admin/sistema` (manutenção) só para ADMIN da loja 1 (`Loja.PLATAFORMA_ID`). Sem isso, o ADMIN de qualquer loja derrubaria o sistema para todas |
| Queries nativas | `AND loja_id = :#{T(...TenantContext).atual()}` (SpEL do Spring Data, constante `TenantContext.LOJA_ATUAL_SPEL`): sem mudar assinatura nem chamadores. Um teste de isolamento por query |
| Jobs agendados | `TenantJobRunner.porLoja(Runnable)` itera as lojas, seta o contexto e abre a transação com `TransactionTemplate` (a sessão do Hibernate captura o tenant ao abrir, então `@Transactional` no método agendado seria cedo demais) |
| Threads `@Async` | `TaskDecorator` copia o `TenantContext` da thread que publicou o evento |
| Fila de campanha | `CampanhaItem` carrega `lojaId`; o worker executa cada item dentro de `TenantContext.executar` |
| Endpoints públicos por token | O próprio `TenantFilter` resolve o `loja_id` pelo token com `JdbcTemplate` e seta o contexto **antes** do Open-Session-In-View abrir a sessão (a sessão fixa o tenant ao abrir, então resolver dentro do controller seria tarde). `/catalogo/**` fica na loja 1 até o F2.3 introduzir vitrine por loja |

## Componentes

### Banco — `V16__multi_tenant.sql`

1. Cria `lojas(id BIGSERIAL PK, nome VARCHAR(150) NOT NULL, criada_em TIMESTAMP NOT NULL DEFAULT now())`.
2. Insere a loja 1, "Loja Padrão".
3. Em cada tabela abaixo, adiciona `loja_id BIGINT NOT NULL DEFAULT 1 REFERENCES lojas(id)` e cria `idx_<tabela>_loja_id`. O DEFAULT 1 fica: o Hibernate sempre grava `loja_id` explicitamente (a guarda `@PrePersist` impede escrita sem tenant), e os seeds SQL em `infra/seed/` dependem desse DEFAULT 1 (os `ON CONFLICT` de `orcamentos_gasto` foram atualizados para incluir `loja_id`).
4. Os UNIQUE de configuração passam a incluir `loja_id` (abaixo).
5. Ajusta a sequência de `lojas` após o insert explícito do id 1.

Tabelas com `loja_id` e `@TenantId` (29): `alertas_internos`, `audit_log`, `checklist_qualidade`, `clientes`, `configuracao_canal`, `configuracao_financeira`, `contas_pagar`, `contas_receber`, `despesas_fixas`, `despesas_variaveis`, `fichas_tecnicas`, `fichas_tecnicas_itens`, `fornecedores`, `gastos_variaveis`, `inspecao_qualidade`, `itens_pedido`, `materias_primas`, `movimentacoes_estoque`, `notas_cliente`, `notificacoes_enviadas`, `orcamento_itens`, `orcamentos`, `orcamentos_gasto`, `ordens_producao`, `pagamentos`, `pedidos`, `pontos_fidelidade`, `produtos`, `templates_notificacao`.

Tabela com `loja_id` explícito, sem `@TenantId`: `usuarios`.

UNIQUE que viram compostos:
- `configuracao_canal.tipo` → `(loja_id, tipo)`
- `orcamentos_gasto (categoria, referencia_mes, referencia_ano)` → inclui `loja_id`
- `fichas_tecnicas.produto_id` já é único por produto, que pertence a uma loja. Mantém.
- `token_acompanhamento`, `token_aprovacao` e `password_reset_token.token` (UUID) continuam únicos globalmente.
- Índices de idempotência de notificação (`uq_notif_*`) não mudam: usam `pedido_id`, `orcamento_id` e `cliente_id`, que já são únicos entre lojas.
- `campanha_reengajamento` não tem entidade Java e fica fora.

`configuracao_financeira` hoje é linha única (`findAll().findFirst()` cria se faltar). Com o filtro, cada loja passa a ter a sua, criada sob demanda pelo próprio `obter()`. `configuracao_sistema` continua singleton global com id 1.

### Java

- `common/tenant/TenantContext`: `ThreadLocal<Long>`, com `set`, `get` (lança se vazio), `limpar` e `executar(lojaId, Runnable)`.
- `common/tenant/TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long>`, registrado por um `HibernatePropertiesCustomizer` (`hibernate.tenant_identifier_resolver`).
- `common/tenant/TenantJobRunner` e `TenantTaskDecorator` (registrado como bean para o executor do `@Async`).
- `common/entity/TenantEntity` (`@MappedSuperclass`) com `@TenantId @Column(name="loja_id", nullable=false, updatable=false) Long lojaId`. As 29 entidades herdam dela, direta ou via `BaseEntity`. Entidades que hoje não herdam `BaseEntity` herdam só `TenantEntity`.
- `seguranca/entity/Loja` e `LojaRepository`.
- `seguranca/service/UsuarioPrincipal extends User` com `lojaId`. `UsuarioService.loadUserByUsername` devolve `UsuarioPrincipal`.
- `config/TenantFilter` (`OncePerRequestFilter`, instanciado dentro do `SecurityConfig` com `new`, para o Boot não registrá-lo uma segunda vez como filtro de servlet), depois da autenticação: lê o `lojaId` do `UsuarioPrincipal` e preenche o `TenantContext`. Limpa em `finally`. Rotas públicas sem token não precisam de tenant (login, estáticos).
- `TwoFactorAuthenticationSuccessHandler` e qualquer ponto que recria o `Authentication` precisam preservar o `UsuarioPrincipal`.
- `UsuarioRepository`: `findAllByLojaIdOrderByNomeAsc`. `UsuarioService.listarTodos`/`buscarPorId` filtram por `loja_id` explícito. Cadastro de usuário grava o `loja_id` do ADMIN logado.
- `DataInitializer`: cria o admin com `loja_id = 1`.
- Públicos: o `TenantFilter` lê o token do caminho (`/acompanhamento/{token}`, `/orcamento-publico/{token}[/...]`) e consulta `loja_id` por `JdbcTemplate` (`pedidos.token_acompanhamento`, `orcamentos.token_aprovacao`). `PasswordResetService` só toca `usuarios` e `password_reset_token`, sem tenant.
- Jobs (`CrmService.recalcularSegmentos`, `NotificacaoAgendadaService` ×2): o método `@Scheduled` perde o `@Transactional` e delega a `TenantJobRunner.porLoja(this::<logica>)`; a lógica vira método público `@Transactional` para os testes chamarem direto. `CampanhaService.processarProximo` usa o `lojaId` do item. ShedLock fica global, uma execução por job.
- `NotificacaoEventListener` e `AlertaInternoListener` são `@Async`: o `TenantTaskDecorator` propaga o tenant.

### Queries nativas a ajustar

| Repositório | Método |
|---|---|
| `PedidoRepository` | `faturamentoPorMes`, `totalPorAno`, `countPorAno`, `anosComPedidos` |
| `ItemPedidoRepository` | `rankingProdutosPorAno` (junta `itens_pedido` com `pedidos`) |
| `PontoFidelidadeRepository` | `saldoPorCliente` |
| `NotificacaoEnviadaRepository` | `jaEnviouAniversarioNoAno` |
| `ClienteRepository` | `findAniversariantesHoje` |

Cada uma ganha `AND loja_id = <SpEL do tenant atual>`. Em joins nativos (`rankingProdutosPorAno`), filtrar todas as tabelas envolvidas.

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
- Testes existentes: um `TestExecutionListener` (registrado em `META-INF/spring.factories`, ordem anterior ao `TransactionalTestExecutionListener`) seta `TenantContext` = 1 antes de cada método. Precisa vir antes da transação do teste porque a sessão captura o tenant ao abrir. Só `CrmSegmentoTest` muda (chama o método novo da lógica por loja).
- Validação da migration: rodar `V16` em cópia do banco do sandbox (seed da massa de teste) e conferir `SELECT COUNT(*) WHERE loja_id IS NULL` = 0 em todas as tabelas.

## Fora do escopo

Cadastro de loja nova e onboarding (F0.2), roles Dono/Equipe (F0.4), uploads por loja em S3 (F0.6; enquanto isso os arquivos ficam num diretório compartilhado, então prefixar por `loja_id` no caminho entra no F0.6), planos e cobrança (F2.5).

## Riscos

- **Esquecer uma query nativa nova:** mitigado pelo teste por query e pela regra de revisão "nativeQuery exige `loja_id`". O `V16` não cobre queries futuras.
- **Threads fora da requisição:** sem propagação, leituras voltam vazias e escritas falham. Seguro, mas pode silenciar notificações. Coberto pelo `TenantTaskDecorator` e por teste do fluxo de notificação.
- **Sessões antigas:** a sessão HTTP fica em memória, então o restart do deploy descarta todas. Um principal que não seja `UsuarioPrincipal` (ex.: `@WithMockUser` nos testes) não define tenant; em produção isso não ocorre.
- **Contador da fila de campanha** (`CampanhaQueue.registrarEnvio/Falha`) segue global. Mostra só totais agregados; separar por loja fica para quando a campanha for revista.
- **Migration em tabela grande:** `ADD COLUMN ... DEFAULT 1` no Postgres 16 é metadado (rápido). A criação de 30 índices é o trecho lento; aceitável no volume atual (5k pedidos).
