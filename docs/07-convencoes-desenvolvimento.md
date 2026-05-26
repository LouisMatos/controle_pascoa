# Convenções de Desenvolvimento — Sistema Controle Páscoa

> **Fonte:** Leitura direta de PedidoController, PedidoService, PedidoRepository, StatusPedido, layout.html, GlobalModelAdvice e GlobalExceptionHandler  
> **Atualizado em:** 2026-05-26

---

## 1. Estrutura de um Novo Módulo

Cada novo módulo segue esta estrutura de pacotes e arquivos:

```
src/main/java/br/com/seuprojeto/pascoa/{modulo}/
├── controller/
│   └── {Entidade}Controller.java
├── service/
│   └── {Entidade}Service.java
├── repository/
│   └── {Entidade}Repository.java
├── entity/
│   └── {Entidade}.java
│   └── {StatusEnum}.java          (se tiver máquina de estados)
└── dto/
    └── {Entidade}Form.java        (input do formulário)
    └── {Entidade}Dto.java         (output para a view, se necessário)

src/main/resources/templates/{modulo}/
├── lista.html
├── form.html
└── detalhe.html                   (opcional)
```

**Passos para adicionar um módulo:**
1. Criar migration Flyway (`V{N}__nome_tabela.sql`)
2. Criar entity Java
3. Criar repository
4. Criar service
5. Criar controller
6. Criar templates HTML
7. Adicionar rota em `SecurityConfig.java`
8. Adicionar link no menu em `fragments/layout.html`

---

## 2. Padrão de Entity

```java
@Entity
@Table(name = "nome_tabela_plural")   // snake_case, plural
@Data
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinhaEntidade extends BaseEntity {   // herdar BaseEntity para auditoria

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    // Enum sempre com EnumType.STRING (armazenado como texto no banco)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeuStatus status;

    // FK: sempre LAZY, sempre @ToString.Exclude para evitar ciclos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outro_id")
    @ToString.Exclude
    private OutraEntidade outro;

    // Coleções: sempre LAZY, sempre @ToString.Exclude, @Builder.Default com new ArrayList<>()
    @OneToMany(mappedBy = "minhaEntidade", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<ItemEntidade> itens = new ArrayList<>();

    // Campo calculado: @Transient (não persiste, não cria coluna)
    @Transient
    public BigDecimal getValorCalculado() { ... }
}
```

**Regras:**
- `@EqualsAndHashCode(of = "id")` — nunca usar coleções para equals/hashCode
- `FetchType.LAZY` em todas as relações — sem exceção
- Sem `@CreatedDate`/`@LastModifiedDate` direto: herdar `BaseEntity`
- Soft-delete: usar `@SQLDelete` + `@SQLRestriction` (ver `Cliente.java` ou `Produto.java`)

### Quando NÃO herdar BaseEntity
- Entidades sem necessidade de auditoria de criação/modificação (ex: itens de pedido, movimentações de estoque)
- Entidades com coluna de timestamp com nome legado: usar `@AttributeOverride` (ver `Cliente.java`)

### Enum com comportamento (padrão do projeto)
```java
public enum MeuStatus {
    ATIVO("Ativo"),
    INATIVO("Inativo");

    private final String descricao;

    MeuStatus(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }

    // Regras de transição como métodos booleanos no enum
    public boolean podeDesativar() { return this == ATIVO; }
}
```

---

## 3. Padrão de Repository

```java
@Repository
public interface MinhaEntidadeRepository extends JpaRepository<MinhaEntidade, Long> {

    // Query simples derivada (Spring Data)
    List<MinhaEntidade> findByStatusOrderByNomeAsc(MeuStatus status);

    // JOIN FETCH para evitar N+1 — sempre que a view precisar de relações
    @Query("SELECT e FROM MinhaEntidade e LEFT JOIN FETCH e.outro WHERE e.id = :id")
    Optional<MinhaEntidade> findByIdComOutro(@Param("id") Long id);

    // Agregação JPQL
    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM MinhaEntidade e WHERE e.status = :status")
    BigDecimal sumPorStatus(@Param("status") MeuStatus status);

    // Query nativa PostgreSQL — usar apenas quando JPQL não suportar (EXTRACT, funções PG)
    @Query(value = "SELECT EXTRACT(MONTH FROM data_criacao)::int, COUNT(*) " +
                   "FROM minha_tabela WHERE EXTRACT(YEAR FROM data_criacao) = :ano " +
                   "GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> contarPorMes(@Param("ano") int ano);
}
```

**Regras:**
- Usar `JOIN FETCH` para toda query que precisar navegar relações — nunca deixar o Hibernate fazer N+1
- `nativeQuery = true` apenas para funções PostgreSQL-específicas (`EXTRACT`, `::int`, `JSONB`)
- Sempre `COALESCE(SUM(...), 0)` em agregações para não retornar `null`
- `@Param` obrigatório em todos os parâmetros nomeados

---

## 4. Padrão de Service

```java
@Service
@RequiredArgsConstructor      // injeção via construtor — nunca @Autowired
public class MinhaEntidadeService {

    private final MinhaEntidadeRepository repository;
    private final OutraEntidadeRepository outraRepository;
    private final ApplicationEventPublisher eventPublisher;  // se publicar eventos

    // Leitura: sempre readOnly = true
    @Transactional(readOnly = true)
    public List<MinhaEntidade> listarTodos() {
        return repository.findAll();
    }

    // Busca por ID: sempre com .orElseThrow() — nunca retornar Optional para o controller
    @Transactional(readOnly = true)
    public MinhaEntidade buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("MinhaEntidade não encontrada: " + id));
    }

    // Criação
    @Transactional
    public MinhaEntidade criar(MinhaEntidadeForm form) {
        // Validar regras de negócio antes de persistir
        OutraEntidade outra = outraRepository.findById(form.getOutroId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("OutraEntidade não encontrada"));

        MinhaEntidade entidade = MinhaEntidade.builder()
            .nome(form.getNome())
            .outro(outra)
            .build();
        MinhaEntidade salva = repository.save(entidade);

        // Publicar evento se necessário
        eventPublisher.publishEvent(new MeuEvento(salva, EventoNotificacao.TIPO));
        return salva;
    }

    // Regras de negócio: IllegalStateException para violações
    @Transactional
    public void mudarStatus(Long id) {
        MinhaEntidade entidade = buscarPorId(id);
        if (!entidade.getStatus().podeDesativar()) {
            throw new IllegalStateException("Entidade não pode ser desativada no status atual.");
        }
        entidade.setStatus(MeuStatus.INATIVO);
        repository.save(entidade);
    }
}
```

**Regras:**
- `@Transactional(readOnly = true)` em todo método de leitura
- `@Transactional` (sem `readOnly`) em todo método que escreve
- Nunca expor `Optional` ao controller — sempre `.orElseThrow()`
- Regras de negócio violadas → `throw new IllegalStateException("mensagem clara")`
- Não encontrado → `throw new RecursoNaoEncontradoException("mensagem")`
- Ambas são capturadas pelo `GlobalExceptionHandler` e exibidas como flash `"erro"`

---

## 5. Padrão de Controller

```java
@Controller
@RequestMapping("/minha-entidade")
@RequiredArgsConstructor
public class MinhaEntidadeController {

    private final MinhaEntidadeService service;
    private final OutraEntidadeService outraService;  // para popular selects

    // ── Listagem ──────────────────────────────────────────────────────────

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("itens", service.listarTodos());
        return "minha-entidade/lista";      // → templates/minha-entidade/lista.html
    }

    // ── Formulário novo ───────────────────────────────────────────────────

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new MinhaEntidadeForm());
        model.addAttribute("outras", outraService.listarTodos());  // para selects
        return "minha-entidade/form";
    }

    // ── Formulário editar ─────────────────────────────────────────────────

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        MinhaEntidade entidade = service.buscarPorId(id);
        MinhaEntidadeForm form = new MinhaEntidadeForm();
        form.setId(entidade.getId());
        form.setNome(entidade.getNome());
        // preencher outros campos do form...
        model.addAttribute("form", form);
        model.addAttribute("outras", outraService.listarTodos());
        return "minha-entidade/form";
    }

    // ── Salvar (novo + editar compartilham o mesmo POST) ──────────────────

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("form") MinhaEntidadeForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            // Repopular dados necessários para o form e renderizar de volta
            model.addAttribute("outras", outraService.listarTodos());
            return "minha-entidade/form";
        }
        try {
            MinhaEntidade salva = (form.getId() == null)
                ? service.criar(form)
                : service.atualizar(form.getId(), form);
            ra.addFlashAttribute("sucesso", "Registro salvo com sucesso!");
            return "redirect:/minha-entidade/" + salva.getId();
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/minha-entidade";
        }
    }

    // ── Ação de estado (POST sem form, só @PathVariable) ──────────────────

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.ativar(id);
            ra.addFlashAttribute("sucesso", "Ativado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/minha-entidade/" + id;
    }
}
```

**Regras:**
- `@Controller` (não `@RestController`) — este projeto é Thymeleaf MVC puro
- Retorno de métodos: sempre `String` (nome da view ou redirect)
- Flash messages: `ra.addFlashAttribute("sucesso", "...")` e `ra.addFlashAttribute("erro", "...")`
- Redirect após POST: `return "redirect:/path"` — nunca renderizar direto após POST
- Erros de validação Bean Validation: retornar a view do form (sem redirect), repopulando os dados
- Ações de estado (confirmar, cancelar, etc.): try-catch + flash + redirect

---

## 6. Padrão de DTO/Form

```java
// Form: usado para receber dados do formulário HTML
@Data
@NoArgsConstructor
public class MinhaEntidadeForm {

    private Long id;     // null = criar, não-null = editar

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "Referência é obrigatória")
    private Long outroId;   // ID da entidade relacionada (não o objeto)
}
```

```java
// Dto: usado para output (dados calculados ou projeções para a view)
@Data
@Builder
public class MinhaEntidadeDto {
    private Long id;
    private String nome;
    private BigDecimal valorCalculado;
    // Campos que não existem na entidade ou são calculados
}
```

**Regras:**
- `Form` = entrada do usuário (validações Bean Validation)
- `Dto` = saída para a view (campos calculados/projeções)
- FK no form como `Long {entidade}Id` — não como o objeto JPA inteiro
- `id` no form = null para novo, preenchido para editar (mesma lógica no controller)

---

## 7. Padrão de Template HTML

### Estrutura obrigatória de toda página

```html
<!DOCTYPE html>
<html th:replace="~{fragments/layout :: layout(~{::title}, ~{::#pageContent})}"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title>Título da Página — Sistema Páscoa</title>
</head>
<body>
<div id="pageContent">

    <!-- Cabeçalho padrão de página -->
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0"><i class="bi bi-ICONE me-2 text-CLASSE"></i>Título</h2>
        <a th:href="@{/minha-entidade/novo}" class="btn btn-success">
            <i class="bi bi-plus-lg me-1"></i>Novo
        </a>
    </div>

    <!-- Conteúdo da página -->

</div>
</body>
</html>
```

**Pontos críticos:**
- `th:replace` no `<html>` — não no `<body>`
- `id="pageContent"` no div raiz do conteúdo — deve ser exato
- Flash messages (`sucesso`/`erro`) são exibidos **automaticamente** pelo layout — não adicionar nos templates filhos
- Importações (Bootstrap, Bootstrap Icons) estão no layout — não repetir

### Flash Messages (gerenciado pelo layout)

O `layout.html` exibe automaticamente qualquer atributo `"sucesso"` ou `"erro"` no model:
```html
<!-- Já no layout.html — NÃO repetir nas páginas filhas -->
<div th:if="${sucesso}" class="alert alert-success ...">...</div>
<div th:if="${erro}"    class="alert alert-danger ...">...</div>
```

### Controle de acesso nos templates

```html
<!-- Exibe elemento só para roles específicas -->
<div sec:authorize="hasAnyRole('ADMIN', 'FINANCEIRO')">
    conteúdo restrito
</div>

<!-- Exibe nome do usuário logado -->
<span sec:authentication="name"></span>
```

### Helpers Thymeleaf usados no projeto

```html
<!-- Links e ações -->
th:href="@{/path}"
th:href="@{/path/{id}(id=${item.id})}"
th:action="@{/path/salvar}"

<!-- Formatação de moeda (padrão BR) -->
R$ <span th:text="${#numbers.formatDecimal(valor, 1, 'POINT', 2, 'COMMA')}"></span>
<!-- Resultado: R$ 1.234,56 -->

<!-- Formatação de datas -->
th:text="${#temporals.format(data, 'dd/MM/yyyy')}"
th:text="${#temporals.format(dataHora, 'dd/MM/yyyy HH:mm')}"

<!-- Iteração -->
<tr th:each="item : ${itens}">

<!-- Estado vazio -->
<tr th:if="${#lists.isEmpty(itens)}">
    <td colspan="N" class="text-center text-muted py-5">Nenhum item.</td>
</tr>

<!-- Condicionais inline -->
th:classappend="${condicao} ? 'classe-true' : 'classe-false'"
th:text="${condicao} ? 'texto true' : 'texto false'"

<!-- Formulário POST com CSRF (automático pelo Spring Security) -->
<form th:action="@{/path}" method="post">
    <input type="hidden" th:field="*{campo}">
    <button type="submit" class="btn btn-primary">Salvar</button>
</form>
```

### Formulário com binding Thymeleaf

```html
<form th:action="@{/minha-entidade/salvar}" th:object="${form}" method="post">
    <input type="hidden" th:field="*{id}">

    <div class="mb-3">
        <label class="form-label fw-semibold">Nome <span class="text-danger">*</span></label>
        <input type="text" th:field="*{nome}"
               th:classappend="${#fields.hasErrors('nome')} ? 'is-invalid'"
               class="form-control">
        <div th:if="${#fields.hasErrors('nome')}" class="invalid-feedback"
             th:errors="*{nome}"></div>
    </div>

    <!-- Select para FK -->
    <div class="mb-3">
        <label class="form-label fw-semibold">Referência *</label>
        <select th:field="*{outroId}" class="form-select">
            <option value="">Selecione...</option>
            <option th:each="o : ${outras}" th:value="${o.id}" th:text="${o.nome}"></option>
        </select>
    </div>

    <button type="submit" class="btn btn-success">
        <i class="bi bi-check-lg me-1"></i>Salvar
    </button>
    <a th:href="@{/minha-entidade}" class="btn btn-outline-secondary ms-2">Cancelar</a>
</form>
```

### Tabela padrão do projeto

```html
<div class="card">
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th class="ps-3">#</th>
                        <th>Nome</th>
                        <th class="text-center pe-3">Ações</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="item : ${itens}">
                        <td class="ps-3 fw-bold text-muted" th:text="${item.id}"></td>
                        <td th:text="${item.nome}"></td>
                        <td class="text-center pe-3">
                            <a th:href="@{/minha-entidade/{id}(id=${item.id})}"
                               class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-eye"></i>
                            </a>
                            <a th:href="@{/minha-entidade/{id}/editar(id=${item.id})}"
                               class="btn btn-sm btn-outline-secondary">
                                <i class="bi bi-pencil"></i>
                            </a>
                        </td>
                    </tr>
                    <tr th:if="${#lists.isEmpty(itens)}">
                        <td colspan="3" class="text-center text-muted py-5">
                            <i class="bi bi-inbox fs-3 d-block mb-2 opacity-50"></i>
                            Nenhum registro encontrado.
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
    <div class="card-footer text-muted small" th:if="${!#lists.isEmpty(itens)}">
        <span th:text="${#lists.size(itens)}"></span> registro(s).
    </div>
</div>
```

---

## 8. Adicionando ao Menu de Navegação

O menu está em `src/main/resources/templates/fragments/layout.html`.

O menu é organizado em **7 grupos fixos** — não adicionar links avulsos no topo:

| Grupo | `sec:authorize` | O que contém |
|-------|-----------------|-------------|
| **Cadastros** | qualquer autenticado | Clientes, Produtos, Matérias-Primas, Fornecedores |
| **Comercial** | `ADMIN, ATENDENTE` | Pedidos, Orçamentos, CRM |
| **Produção** | `ADMIN, CONFEITEIRO, GESTOR_QUALIDADE` | Kanban, Ordens, Qualidade |
| **Estoque** | qualquer autenticado | Movimentações, Entrada, Ajuste |
| **Financeiro** | `ADMIN, FINANCEIRO, ANALISTA` | Dashboard, análises, contas; seção Gastos Variáveis (`ADMIN, FINANCEIRO`) |
| **Admin** | `ADMIN` | Notificações, Usuários |
| **Catálogo** | qualquer autenticado | Link externo (target=_blank) |

Para adicionar um link, localize o grupo adequado e insira um `<li>` dentro do `<ul class="dropdown-menu">` correspondente:

```html
<!-- Dentro do grupo correto em layout.html -->
<li sec:authorize="hasAnyRole('ADMIN', 'OUTRO_ROLE')">
    <a class="dropdown-item" th:href="@{/minha-entidade}">
        <i class="bi bi-ICONE me-2"></i>Minha Entidade
    </a>
</li>
```

Para criar um **novo grupo** (caso o módulo não se encaixe em nenhum existente):

```html
<li class="nav-item dropdown" sec:authorize="hasAnyRole('ADMIN', 'OUTRO_ROLE')">
    <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
        <i class="bi bi-ICONE me-1"></i>Novo Grupo
    </a>
    <ul class="dropdown-menu">
        <li><a class="dropdown-item" th:href="@{/minha-entidade}">
            <i class="bi bi-list-ul me-2"></i>Listar</a></li>
        <li><a class="dropdown-item" th:href="@{/minha-entidade/novo}">
            <i class="bi bi-plus-circle me-2 text-success"></i>Novo</a></li>
    </ul>
</li>
```

---

## 9. Adicionando Rota ao SecurityConfig

Arquivo: `src/main/java/br/com/seuprojeto/pascoa/config/SecurityConfig.java`

```java
// Dentro de .authorizeHttpRequests(auth -> auth
//   .requestMatchers(...rotas públicas...).permitAll()
//   .requestMatchers(...) ao resto das rotas

// Rota pública (sem autenticação):
.requestMatchers("/minha-entidade/publica/**").permitAll()

// Rota protegida para roles específicas:
.requestMatchers("/minha-entidade/**").hasAnyRole("ADMIN", "ROLE_NAME")

// Alternativa — proteger na classe (menos comum neste projeto):
.requestMatchers("/minha-entidade/**").authenticated()
```

> **Atenção:** Rotas não declaradas no `SecurityConfig` ficam inacessíveis por padrão (`.anyRequest().authenticated()` no final).

---

## 10. Criando Migration Flyway

Arquivo: `src/main/resources/db/migration/V{N}__{descricao_snake_case}.sql`

**Próxima versão disponível: V5**

```sql
-- V5__adicionar_modulo_exemplo.sql

-- Nova tabela
CREATE TABLE IF NOT EXISTS minha_tabela (
    id           BIGSERIAL PRIMARY KEY,
    nome         VARCHAR(150)  NOT NULL,
    valor        NUMERIC(10,2) NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ATIVO',
    referencia_id BIGINT       REFERENCES outra_tabela(id),
    criado_em    TIMESTAMP     NOT NULL DEFAULT NOW(),
    criado_por   VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_minha_tabela_status ON minha_tabela (status);

-- Adicionar coluna nullable (seguro, sem DEFAULT obrigatório)
ALTER TABLE tabela_existente ADD COLUMN IF NOT EXISTS nova_coluna VARCHAR(100);

-- Adicionar coluna NOT NULL em tabela existente (REQUER DEFAULT ou UPDATE prévio)
ALTER TABLE tabela_existente ADD COLUMN nova_nao_nula VARCHAR(20) NOT NULL DEFAULT 'VALOR';
-- Ou em duas etapas:
ALTER TABLE tabela_existente ADD COLUMN nova_nao_nula VARCHAR(20);
UPDATE tabela_existente SET nova_nao_nula = 'VALOR_PADRAO' WHERE nova_nao_nula IS NULL;
ALTER TABLE tabela_existente ALTER COLUMN nova_nao_nula SET NOT NULL;
```

**Regras obrigatórias:**
- Nome: `V{N}__{descricao}.sql` — dois underscores entre versão e descrição
- **Nunca editar** uma migration já aplicada ao banco
- `CREATE TABLE IF NOT EXISTS` — sempre com IF NOT EXISTS
- Colunas `NOT NULL` novas em tabelas existentes sempre com `DEFAULT` (lição aprendida)
- `ddl-auto=validate` — o Hibernate valida mas nunca cria/altera tabelas automaticamente

---

## 11. Publicando e Consumindo Eventos

### Publicar no Service

```java
// Injetar via @RequiredArgsConstructor
private final ApplicationEventPublisher eventPublisher;

// Publicar após persistência
MinhaEntidade salva = repository.save(entidade);
eventPublisher.publishEvent(new PedidoStatusEvent(pedido, EventoNotificacao.PEDIDO_CONFIRMADO));
```

### Evento existente para notificações de pedido

```java
// src/main/java/br/com/seuprojeto/pascoa/notificacao/event/PedidoStatusEvent.java
new PedidoStatusEvent(pedido, EventoNotificacao.TIPO_DO_EVENTO)
```

Valores disponíveis de `EventoNotificacao`:
```
PEDIDO_CONFIRMADO, PRODUCAO_INICIADA, PEDIDO_PRONTO, PEDIDO_ENTREGUE,
PAGAMENTO_RECEBIDO, PEDIDO_CANCELADO, ORCAMENTO_APROVADO, ORCAMENTO_RECUSADO
```

### Consumir evento em um Listener

```java
@Component
@RequiredArgsConstructor
public class MeuListener {

    @EventListener
    public void onMeuEvento(PedidoStatusEvent event) {
        // event.getPedido() — pedido que gerou o evento
        // event.getEvento() — tipo do evento
        ...
    }
}
```

---

## 12. Tratamento de Erros

### Exceções do projeto (em `shared/exception/`)

| Exceção | Quando usar |
|---------|-------------|
| `RecursoNaoEncontradoException` | Entidade não encontrada no banco (busca por ID) |
| `EstoqueInsuficienteException` | Tentativa de saída de estoque maior que saldo |
| `IllegalStateException` | Violação de regra de negócio (transição inválida, etc.) |

**Todas são capturadas automaticamente** pelo `GlobalExceptionHandler`:
- Exibem a mensagem como flash `"erro"` via `RedirectAttributes`
- Redirecionam para a página anterior (Referer) ou `/dashboard`

```java
// Não encontrado
throw new RecursoNaoEncontradoException("Pedido #" + id + " não encontrado");

// Regra de negócio
throw new IllegalStateException("Pedido com status ENTREGUE não pode ser cancelado.");

// Estoque
throw new EstoqueInsuficienteException("Saldo insuficiente para " + materiaPrima.getNome());
```

**`DataIntegrityViolationException`** (FK violation) também é capturada e exibe mensagem genérica de "não é possível excluir pois está vinculado".

---

## 13. Atributos Globais Disponíveis em Todos os Templates

O `GlobalModelAdvice.java` injeta automaticamente em todas as páginas autenticadas:

| Atributo | Tipo | Conteúdo |
|----------|------|----------|
| `alertasRecentes` | `List<AlertaInterno>` | Últimos alertas internos (para o sino do navbar) |
| `alertasNaoLidos` | `long` | Contagem de alertas não lidos (badge vermelho) |

Disponíveis diretamente em qualquer template — não precisam ser adicionados pelo controller.

---

## 14. Convenções de Nomenclatura

### Java

| Elemento | Convenção | Exemplo |
|----------|-----------|---------|
| Classe | PascalCase | `PedidoController`, `FluxoCaixaService` |
| Método | camelCase | `listarTodos()`, `buscarPorId()` |
| Variável | camelCase | `pedidoRepository`, `totalPago` |
| Constante | UPPER_SNAKE | `MAX_ITENS` |
| Pacote | lowercase | `br.com.seuprojeto.pascoa.pedido` |
| Enum value | UPPER_SNAKE | `EM_PRODUCAO`, `GESTOR_QUALIDADE` |

### Banco de Dados

| Elemento | Convenção | Exemplo |
|----------|-----------|---------|
| Tabela | snake_case plural | `pedidos`, `itens_pedido`, `ordens_producao` |
| Coluna | snake_case | `data_pedido`, `cliente_id`, `preco_unitario` |
| FK | `{entidade_referenciada}_id` | `cliente_id`, `produto_id` |
| Índice | `idx_{tabela}_{coluna(s)}` | `idx_gastos_mes_ano` |

### Templates

| Elemento | Convenção | Exemplo |
|----------|-----------|---------|
| Pasta | kebab-case (nome do módulo) | `pedidos/`, `fichas-tecnicas/` |
| Arquivo | kebab-case | `lista.html`, `form.html`, `inspecao-form.html` |
| Fragmento | kebab-case | `layout.html`, `campos-comuns.html` |

### Rotas HTTP

| Operação | Padrão | Exemplo |
|----------|--------|---------|
| Listar | `GET /{entidades}` | `GET /pedidos` |
| Novo form | `GET /{entidades}/novo` | `GET /pedidos/novo` |
| Editar form | `GET /{entidades}/{id}/editar` | `GET /pedidos/42/editar` |
| Salvar | `POST /{entidades}/salvar` | `POST /pedidos/salvar` |
| Detalhe | `GET /{entidades}/{id}` | `GET /pedidos/42` |
| Ação de estado | `POST /{entidades}/{id}/{acao}` | `POST /pedidos/42/confirmar` |
| Excluir | `POST /{entidades}/{id}/excluir` | `POST /pedidos/42/excluir` |

> **Nota:** O projeto usa apenas `GET` e `POST` — sem `PUT`, `PATCH` ou `DELETE`. Formulários HTML só suportam GET e POST nativamente.

---

## 15. Checklist para Pull Request / Novo Recurso

- [ ] Migration Flyway criada (`V{N}__...sql`) com `IF NOT EXISTS`
- [ ] Entity criada/atualizada com `@EqualsAndHashCode(of = "id")` e `FetchType.LAZY`
- [ ] Repository com `JOIN FETCH` nas queries que carregam relações
- [ ] Service com `@Transactional(readOnly = true)` nas leituras
- [ ] Controller retornando `redirect:` após todo POST
- [ ] Flash messages usando `ra.addFlashAttribute("sucesso"/"erro")`
- [ ] Template usando `th:replace="~{fragments/layout :: layout(...)}"` 
- [ ] Rota adicionada no `SecurityConfig.java`
- [ ] Link adicionado no menu do `layout.html`
- [ ] `ddl-auto=validate` **não alterado**
