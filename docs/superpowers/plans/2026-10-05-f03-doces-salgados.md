# F0.3 Doces e salgados Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trocar a suposição "só ovos de Páscoa" por categorias livres por loja, unidade de venda (unidade, dúzia, cento, pacote, kg) e quantidade decimal.

**Architecture:** Três fases independentes, cada uma com uma migration e a suíte verde ao final: A (categorias livres, V17), B (unidade de venda e sazonal, V18), C (quantidade decimal, V19). Tudo respeita o multi-tenant do F0.1: entidades novas herdam `TenantEntity`, queries nativas filtram por `loja_id`.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Hibernate 6.5, Spring Data JPA, Thymeleaf, Flyway, PostgreSQL 16 (H2 nos testes), JUnit 5 + AssertJ + MockMvc.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-10-05-f03-doces-salgados-design.md`. Pré-requisito: F0.1 concluído e a revisão final dele fechada.
- Pacote base `br.com.seuprojeto.pascoa`; módulo `pascoa-monolith`. Caminhos abaixo: `<main>` = `pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa`, `<res>` = `pascoa-monolith/src/main/resources`, `<test>` = `pascoa-monolith/src/test/java/br/com/seuprojeto/pascoa`.
- `@RequiredArgsConstructor`, nunca `@Autowired` em código de produção. Services `@Transactional`. Entidades nunca mexem em `ddl-auto`.
- Migration Flyway `V{N}__{descricao_snake_case}.sql` em `<res>/db/migration/`; próximas: V17 (A), V18 (B), V19 (C). Coluna NOT NULL nova exige DEFAULT.
- Multi-tenant: entidade nova herda `TenantEntity` e a tabela ganha `loja_id BIGINT NOT NULL DEFAULT 1 REFERENCES lojas(id)`; `nativeQuery` filtra com `TenantContext.LOJA_ATUAL_SPEL`.
- Código sem comentários, exceto regra de negócio não óbvia. Thymeleaf: nunca JS inline (CSP); scripts em `<res>/static/js/` incluídos dentro do `#pageContent`; ícones decorativos com `aria-hidden="true"`, `<label for>` literal, sem `aria-hidden` duplicado no mesmo `<i>`.
- Quantidade: `NUMERIC(10,3)`; entrada de formulário com `type="number"` (o navegador envia ponto decimal); exibição em pt-BR com vírgula e sem zeros à direita.
- Testes: `mvn test -pl pascoa-monolith -Dtest=<Classe>`; suíte completa: `mvn test -pl pascoa-monolith -Dsurefire.excludes="**/*IT.java,**/*IntegrationTest.java,**/*IT.class,**/*IntegrationTest.class"`. Os testes rodam com o tenant 1 definido pelo `TenantTestExecutionListener`; testes que alternam lojas não são `@Transactional` e usam `TenantContext.executar/calcular`.
- Commits em português, tipo convencional, terminando com `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Branch: `feat/backlog-fase1-encomendas`.
- Validação em PostgreSQL usa banco descartável (`pascoa_f03*`), nunca o banco `pascoa_monolith` do dev; o dev pode ter uma instância rodando na porta 8080, então a aplicação de teste usa outra porta (ex.: 8086) e `--spring.datasource.url`.

## File Structure

| Arquivo | Responsabilidade |
|---|---|
| `<main>/cadastro/entity/CategoriaProduto.java` | Categoria livre por loja (A) |
| `<main>/cadastro/repository/CategoriaProdutoRepository.java`, `cadastro/service/CategoriaProdutoService.java`, `cadastro/controller/CategoriaProdutoController.java` | CRUD das categorias (A) |
| `<res>/templates/categorias/lista.html`, `form.html` | Telas de categorias (A) |
| `<main>/cadastro/entity/UnidadeVenda.java` | Enum de unidade de venda (B) |
| `<main>/common/quantidade/Quantidades.java`, `QuantidadeFormatter.java` | Validação e formatação de quantidade (C) |
| `<res>/db/migration/V17__categorias_produto.sql`, `V18__unidade_venda_sazonal.sql`, `V19__quantidade_decimal.sql` | Schema |
| `<res>/static/js/produto-form.js` | Alterna os campos de temporada (B) |

---

# FASE A — Categorias livres (V17)

### Task 1: Categorias no domínio, migration e produto

**Files:**
- Create: `<res>/db/migration/V17__categorias_produto.sql`
- Create: `<main>/cadastro/entity/CategoriaProduto.java`, `<main>/cadastro/repository/CategoriaProdutoRepository.java`, `<main>/cadastro/service/CategoriaProdutoService.java`
- Modify: `<main>/cadastro/entity/Produto.java`, `<main>/cadastro/service/ProdutoService.java:55-63`, `<main>/cadastro/controller/ProdutoController.java`, `<main>/catalogo/CatalogoController.java:3,38-58`, `<main>/pedido/repository/ItemPedidoRepository.java` (query `rankingProdutosPorAno`)
- Delete: `<main>/cadastro/entity/Categoria.java`
- Modify templates: `<res>/templates/produtos/form.html:37-48`, `produtos/lista.html:55-56`, `catalogo/index.html:21-59`, `catalogo/produto.html:40-42`, `pedidos/detalhe.html:171-172`, `pedidos/wizard.html:150`; `<res>/static/css/tokens.css:162-167`
- Modify seeds: `infra/seed/seed-fluxo-pedido.sql`, `seed-massa-teste.sql`, `seed-cenarios.sql`, `seed-mei-realista.sql`
- Modify tests: `<test>/producao/service/ProducaoStatusPedidoTest.java:80`, `<test>/pedido/service/PedidoStateMachineTest.java:70`, `<test>/common/tenant/TenantIsolamentoTest.java`, `<test>/common/tenant/QueriesNativasIsolamentoTest.java` (remover `.categoria(Categoria.X)` e o import `Categoria`)
- Create tests: `<test>/cadastro/CategoriaProdutoTest.java`, `<test>/cadastro/controller/CategoriaTelasTest.java`

**Interfaces:**
- Produces: `CategoriaProduto` (`Long getId()`, `String getNome()`, `Boolean getAtivo()`, builder com `id`, `nome`, `ativo`); `CategoriaProdutoRepository` com `findAllByOrderByNomeAsc()`, `findByAtivoTrueOrderByNomeAsc()`, `existsByNomeIgnoreCase(String)`, `existsByNomeIgnoreCaseAndIdNot(String, Long)`; `CategoriaProdutoService.listarAtivas()` e `buscarPorId(Long)`; `Produto.getCategoria()` devolve `CategoriaProduto` (pode ser `null`).

- [ ] **Step 1: Escrever os testes**

`<test>/cadastro/CategoriaProdutoTest.java`:

```java
package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CategoriaProdutoTest {

    @Autowired private CategoriaProdutoRepository categorias;
    @Autowired private ProdutoService produtoService;

    private CategoriaProduto nova(String nome) {
        return CategoriaProduto.builder().nome(nome).build();
    }

    private List<String> nomes(long loja) {
        return TenantContext.calcular(loja, () -> categorias.findAll().stream().map(CategoriaProduto::getNome).toList());
    }

    @Test
    void categoria_daLojaA_naoApareceNaLojaB() {
        String nome = "Cat-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> categorias.save(nova(nome)));

        assertThat(nomes(1L)).contains(nome);
        assertThat(nomes(2L)).doesNotContain(nome);
    }

    @Test
    void mesmoNome_emLojasDiferentes_epermitido_emDuplicidadeNaMesmaLoja_falha() {
        String nome = "Cat-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> categorias.save(nova(nome)));
        TenantContext.executar(2L, () -> categorias.save(nova(nome)));

        assertThatThrownBy(() -> TenantContext.executar(1L, () -> categorias.saveAndFlush(nova(nome))))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void salvarProduto_comCategoriaDeOutraLoja_falha() {
        Long categoriaDaLoja2 = TenantContext.calcular(2L,
            () -> categorias.save(nova("Cat-" + UUID.randomUUID())).getId());
        Produto produto = Produto.builder().nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN)
            .categoria(CategoriaProduto.builder().id(categoriaDaLoja2).build()).build();

        assertThatThrownBy(() -> TenantContext.executar(1L, () -> produtoService.salvar(produto)))
            .hasStackTraceContaining("Categoria não encontrada");
    }

    @Test
    void editarProdutoDestacado_mantemLojaCategoriaENome() {
        String sufixo = UUID.randomUUID().toString();
        Long categoriaId = TenantContext.calcular(1L, () -> categorias.save(nova("Cat-" + sufixo)).getId());
        Long produtoId = TenantContext.calcular(1L, () -> produtoService.salvar(
            Produto.builder().nome("A-" + sufixo).precoVenda(BigDecimal.TEN).build()).getId());
        Produto editado = Produto.builder().id(produtoId).nome("B-" + sufixo).precoVenda(BigDecimal.TEN)
            .categoria(CategoriaProduto.builder().id(categoriaId).build()).build();

        TenantContext.executar(1L, () -> produtoService.salvar(editado));
        Produto lido = TenantContext.calcular(1L, () -> produtoService.buscarPorId(produtoId));

        assertThat(lido.getNome()).isEqualTo("B-" + sufixo);
        assertThat(lido.getLojaId()).isEqualTo(1L);
        assertThat(lido.getCategoria().getId()).isEqualTo(categoriaId);
    }

    @Test
    void produtoSemCategoria_epermitido() {
        Produto salvo = TenantContext.calcular(1L, () -> produtoService.salvar(
            Produto.builder().nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN).build()));

        assertThat(salvo.getCategoria()).isNull();
    }
}
```

`<test>/cadastro/controller/CategoriaTelasTest.java`:

```java
package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoriaTelasTest {

    @Autowired private MockMvc mvc;
    @Autowired private CategoriaProdutoRepository categorias;
    @Autowired private ProdutoService produtoService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listaFormECatalogo_mostramACategoriaDoProduto() throws Exception {
        String nomeCategoria = "Salgados-" + UUID.randomUUID();
        String nomeProduto = "Coxinha-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> {
            CategoriaProduto categoria = categorias.save(CategoriaProduto.builder().nome(nomeCategoria).build());
            produtoService.salvar(Produto.builder().nome(nomeProduto).precoVenda(new BigDecimal("8.00"))
                .categoria(categoria).build());
            produtoService.salvar(Produto.builder().nome(nomeProduto + "-sem").precoVenda(BigDecimal.TEN).build());
        });

        mvc.perform(get("/produtos")).andExpect(status().isOk()).andExpect(content().string(containsString(nomeCategoria)));
        mvc.perform(get("/produtos/novo")).andExpect(status().isOk()).andExpect(content().string(containsString(nomeCategoria)));
        mvc.perform(get("/catalogo")).andExpect(status().isOk()).andExpect(content().string(containsString(nomeProduto)));
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn test -pl pascoa-monolith -Dtest='CategoriaProdutoTest,CategoriaTelasTest'`
Expected: erro de compilação (`CategoriaProduto` ausente).

- [ ] **Step 3: Migration `V17__categorias_produto.sql`**

```sql
CREATE TABLE categorias_produto (
    id      BIGSERIAL    PRIMARY KEY,
    loja_id BIGINT       NOT NULL DEFAULT 1 REFERENCES lojas(id),
    nome    VARCHAR(60)  NOT NULL,
    ativo   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_categorias_produto_loja_nome UNIQUE (loja_id, nome)
);

CREATE INDEX idx_categorias_produto_loja_id ON categorias_produto (loja_id);

INSERT INTO categorias_produto (loja_id, nome)
SELECT l.id, c.nome
FROM lojas l
CROSS JOIN (VALUES ('Trufado'), ('Recheado'), ('Diet'), ('Vegano'), ('Tradicional'), ('Especial')) AS c(nome);

ALTER TABLE produtos ADD COLUMN categoria_id BIGINT REFERENCES categorias_produto(id);

UPDATE produtos p
SET categoria_id = c.id
FROM categorias_produto c
WHERE c.loja_id = p.loja_id AND upper(c.nome) = p.categoria;

CREATE INDEX idx_produtos_categoria_id ON produtos (categoria_id);

ALTER TABLE produtos DROP COLUMN categoria;
```

- [ ] **Step 4: Entidade, repositório e service mínimo**

`<main>/cadastro/entity/CategoriaProduto.java`:

```java
package br.com.seuprojeto.pascoa.cadastro.entity;

import br.com.seuprojeto.pascoa.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categorias_produto",
       uniqueConstraints = @UniqueConstraint(columnNames = {"loja_id", "nome"}))
@Data
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProduto extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 60, message = "Nome deve ter no máximo 60 caracteres")
    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
```

`<main>/cadastro/repository/CategoriaProdutoRepository.java`:

```java
package br.com.seuprojeto.pascoa.cadastro.repository;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaProdutoRepository extends JpaRepository<CategoriaProduto, Long> {

    List<CategoriaProduto> findAllByOrderByNomeAsc();

    List<CategoriaProduto> findByAtivoTrueOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
```

`<main>/cadastro/service/CategoriaProdutoService.java`:

```java
package br.com.seuprojeto.pascoa.cadastro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaProdutoService {

    private final CategoriaProdutoRepository repository;

    @Transactional(readOnly = true)
    public List<CategoriaProduto> listarAtivas() {
        return repository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public CategoriaProduto buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
    }
}
```

- [ ] **Step 5: `Produto`, `ProdutoService`, `ProdutoController`**

Em `<main>/cadastro/entity/Produto.java`: remover os imports `jakarta.persistence.EnumType` e `jakarta.persistence.Enumerated` (só serviam à categoria), acrescentar `jakarta.persistence.JoinColumn` e `jakarta.persistence.ManyToOne`, e substituir:

```java
    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;
```

por:

```java
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private CategoriaProduto categoria;
```

Em `<main>/cadastro/service/ProdutoService.java`: acrescentar o campo `private final CategoriaProdutoRepository categoriaRepository;` (import `br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository`) e, em `salvar(Produto, MultipartFile)`, logo depois do bloco `if (produto.getAtivo() == null) { ... }`:

```java
        if (produto.getCategoria() != null) {
            Long categoriaId = produto.getCategoria().getId();
            produto.setCategoria(categoriaId == null ? null : categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + categoriaId)));
        }
```

Em `<main>/cadastro/controller/ProdutoController.java`: remover o import de `Categoria`, acrescentar o campo `private final CategoriaProdutoService categoriaService;` (import `br.com.seuprojeto.pascoa.cadastro.service.CategoriaProdutoService`) e trocar as 4 ocorrências de `Categoria.values()` por `categoriaService.listarAtivas()`.

- [ ] **Step 6: `CatalogoController`**

Em `<main>/catalogo/CatalogoController.java`: trocar o import `br.com.seuprojeto.pascoa.cadastro.entity.Categoria` por `br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto` e acrescentar o import de `br.com.seuprojeto.pascoa.cadastro.service.CategoriaProdutoService`; acrescentar o campo `private final CategoriaProdutoService categoriaService;`; substituir o método `index` inteiro por:

```java
    @GetMapping
    public String index(@RequestParam(required = false) Long categoria, Model model) {
        List<Produto> produtos = produtoService.listarAtivas();
        List<CategoriaProduto> categorias = categoriaService.listarAtivas();

        CategoriaProduto categoriaAtiva = categoria == null ? null : categorias.stream()
            .filter(c -> c.getId().equals(categoria))
            .findFirst()
            .orElse(null);
        if (categoriaAtiva != null) {
            Long idAtivo = categoriaAtiva.getId();
            produtos = produtos.stream()
                .filter(p -> p.getCategoria() != null && idAtivo.equals(p.getCategoria().getId()))
                .toList();
        }

        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("categoriaAtiva", categoriaAtiva);
        return "catalogo/index";
    }
```

(O método do service chama-se `listarAtivos` em `ProdutoService`; use `produtoService.listarAtivos()` na primeira linha, como o código atual.)

- [ ] **Step 7: Remover o enum e ajustar o ranking nativo**

Apagar `<main>/cadastro/entity/Categoria.java`. Em `<main>/pedido/repository/ItemPedidoRepository.java`, na query `rankingProdutosPorAno`, trocar a primeira linha do SELECT e o JOIN de produtos e o GROUP BY:

```java
    @Query(value = "SELECT pr.nome, c.nome, SUM(i.quantidade)::bigint, COALESCE(SUM(i.subtotal), 0) " +
                   "FROM itens_pedido i " +
                   "JOIN produtos pr ON i.produto_id = pr.id " +
                   "LEFT JOIN categorias_produto c ON pr.categoria_id = c.id " +
                   "AND c.loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
                   "JOIN pedidos ped ON i.pedido_id = ped.id " +
                   "WHERE EXTRACT(YEAR FROM ped.data_pedido) = :ano AND ped.status != 'CANCELADO' " +
                   "AND i.loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
                   "AND ped.loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
                   "AND pr.loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
                   "GROUP BY pr.id, pr.nome, c.nome " +
                   "ORDER BY SUM(i.quantidade) DESC LIMIT 15",
           nativeQuery = true)
```

(Conferir que o resto da anotação — `nativeQuery = true)` e a assinatura — permanece igual. `AnalyticsService.rankingProdutos` já trata categoria nula como "—".)

- [ ] **Step 8: Templates e CSS**

Em cada template, o badge de categoria deixa de usar a classe por enum e passa a mostrar o nome (ou nada quando o produto não tem categoria). Aplicar:

- `produtos/lista.html:55-56`, `catalogo/index.html:57-59`, `catalogo/produto.html:40-42`, `pedidos/detalhe.html:171-172`: apagar a linha `th:classappend="'badge-' + ${...categoria.name()}"`, acrescentar `text-bg-secondary` à lista de classes estáticas do mesmo `<span class="badge ...">`, e trocar o `th:text` de `${X.categoria.descricao}` para `${X.categoria != null ? X.categoria.nome : ''}` (com `X` = `p`, `p`, `produto`, `item.produto`, respectivamente). Envolver o `<span>` com `th:if="${X.categoria != null}"` quando ele estiver numa célula que ficaria com badge vazio.
- `pedidos/wizard.html:150`: `th:data-categoria="${p.categoria}"` vira `th:data-categoria="${p.categoria != null ? p.categoria.nome : ''}"`.
- `produtos/form.html:37-48`: substituir o bloco da categoria por:

```html
                    <div class="col-md-4">
                        <label class="form-label fw-semibold" for="categoria">Categoria</label>
                        <select class="form-select" id="categoria" name="categoria.id">
                            <option value="">Sem categoria</option>
                            <option th:each="cat : ${categorias}"
                                    th:value="${cat.id}"
                                    th:text="${cat.nome}"
                                    th:selected="${produto.categoria != null and produto.categoria.id == cat.id}"></option>
                        </select>
                        <div class="form-text"><a th:href="@{/categorias}">Gerenciar categorias</a></div>
                    </div>
```

- `catalogo/index.html:21-39`: os links de filtro passam a usar o id:

```html
        <a th:each="cat : ${categorias}"
           th:href="@{/catalogo(categoria=${cat.id})}"
           th:classappend="${categoriaAtiva != null and categoriaAtiva.id == cat.id} ? 'active'"
           th:text="${cat.nome}"></a>
```

(manter as classes e atributos estáticos que o `<a>` já tem; só trocar os três `th:*` acima.)

- `<res>/static/css/tokens.css:162-167`: apagar as seis regras `.badge-TRUFADO` … `.badge-ESPECIAL`.

- [ ] **Step 9: Seeds**

Nos quatro seeds, a coluna `categoria` do INSERT em `produtos` vira `categoria_id`, com o valor resolvido pelo nome da categoria da loja 1 (os nomes das seis categorias são o enum antigo em caixa baixa com inicial maiúscula, então `upper(nome)` casa com o texto antigo):

- `infra/seed/seed-fluxo-pedido.sql:37-38`: no cabeçalho `INSERT INTO produtos (nome, descricao, categoria, ...` trocar `categoria` por `categoria_id`; no SELECT trocar `v.categoria` por `(SELECT cp.id FROM categorias_produto cp WHERE cp.loja_id = 1 AND upper(cp.nome) = v.categoria)`.
- `infra/seed/seed-cenarios.sql:166-167`: mesma troca (cabeçalho e `v.categoria` no SELECT).
- `infra/seed/seed-massa-teste.sql:114-122`: cabeçalho `categoria` → `categoria_id`; a expressão `(ARRAY['TRUFADO','RECHEADO','DIET','VEGANO','TRADICIONAL','ESPECIAL'])[1 + (g % 6)],` vira `(SELECT cp.id FROM categorias_produto cp WHERE cp.loja_id = 1 AND upper(cp.nome) = (ARRAY['TRUFADO','RECHEADO','DIET','VEGANO','TRADICIONAL','ESPECIAL'])[1 + (g % 6)]),`.
- `infra/seed/seed-mei-realista.sql:70-80`: cabeçalho `categoria` → `categoria_id`; nos 8 VALUES, cada literal `'TRADICIONAL'`, `'RECHEADO'`, `'TRUFADO'`, `'ESPECIAL'` vira `(SELECT id FROM categorias_produto WHERE loja_id = 1 AND upper(nome) = '<LITERAL>')`. Rodar: `perl -pi -e "s/'(TRADICIONAL|RECHEADO|TRUFADO|ESPECIAL)'(,\s+\d)/(SELECT id FROM categorias_produto WHERE loja_id = 1 AND upper(nome) = '\$1')\$2/" infra/seed/seed-mei-realista.sql` e conferir com `grep -n "categorias_produto" infra/seed/seed-mei-realista.sql` (8 linhas).

- [ ] **Step 10: Testes existentes**

Em `<test>/producao/service/ProducaoStatusPedidoTest.java:80`, `<test>/pedido/service/PedidoStateMachineTest.java:70`, `<test>/common/tenant/TenantIsolamentoTest.java` e `<test>/common/tenant/QueriesNativasIsolamentoTest.java`: apagar as linhas `.categoria(Categoria.TRADICIONAL)` / `.categoria(Categoria.TRUFADO)` (categoria agora é opcional) e o `import ...cadastro.entity.Categoria;`. Localizar qualquer outro uso com `grep -rn "entity.Categoria;" pascoa-monolith/src/test`.

- [ ] **Step 11: Rodar os testes novos e a suíte**

Run: `mvn test -pl pascoa-monolith -Dtest='CategoriaProdutoTest,CategoriaTelasTest'`
Expected: 5 + 1 testes passando. Se `editarProdutoDestacado_mantemLojaCategoriaENome` falhar com erro do Hibernate sobre o `@TenantId` (merge de entidade destacada com `lojaId` nulo), **parar e reportar BLOCKED** com o erro exato: é um defeito do F0.1 que afeta a edição de qualquer entidade por formulário.

Run: a suíte completa. Expected: 0 falhas.

- [ ] **Step 12: Commit**

```bash
git add pascoa-monolith/src infra/seed
git commit -m "feat(produto): categorias livres por loja (V17)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Tela de categorias

**Files:**
- Modify: `<main>/cadastro/service/CategoriaProdutoService.java`, `<main>/config/LayoutAdvice.java:14`, `<res>/templates/fragments/layout.html` (menu Cadastros, depois do item Produtos)
- Create: `<main>/cadastro/controller/CategoriaProdutoController.java`, `<res>/templates/categorias/lista.html`, `<res>/templates/categorias/form.html`
- Create test: `<test>/cadastro/CategoriaProdutoServiceTest.java`

**Interfaces:**
- Consumes: `CategoriaProdutoRepository` (Task 1).
- Produces: `CategoriaProdutoService.listarTodas()`, `salvar(CategoriaProduto)` (lança `IllegalArgumentException("Já existe uma categoria com este nome.")` em duplicidade), `alternarAtivo(Long)`.

- [ ] **Step 1: Escrever o teste**

`<test>/cadastro/CategoriaProdutoServiceTest.java`:

```java
package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.service.CategoriaProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CategoriaProdutoServiceTest {

    @Autowired private CategoriaProdutoService service;

    private CategoriaProduto form(String nome) {
        CategoriaProduto c = new CategoriaProduto();
        c.setNome(nome);
        return c;
    }

    @Test
    void salvar_aparaOsEspacos_eRejeitaDuplicataIgnorandoCaixa() {
        String base = "Doces-" + UUID.randomUUID();
        CategoriaProduto salva = TenantContext.calcular(1L, () -> service.salvar(form("  " + base + "  ")));

        assertThat(salva.getNome()).isEqualTo(base);
        assertThatThrownBy(() -> TenantContext.executar(1L, () -> service.salvar(form(base.toUpperCase()))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Já existe uma categoria com este nome.");
    }

    @Test
    void editar_semMudarONome_naoContaComoDuplicata() {
        String nome = "Bolos-" + UUID.randomUUID();
        CategoriaProduto salva = TenantContext.calcular(1L, () -> service.salvar(form(nome)));

        CategoriaProduto edicao = form(nome);
        edicao.setId(salva.getId());
        CategoriaProduto editada = TenantContext.calcular(1L, () -> service.salvar(edicao));

        assertThat(editada.getId()).isEqualTo(salva.getId());
    }

    @Test
    void alternarAtivo_inativaEReativa_eInativaSaiDaListaDeAtivas() {
        String nome = "Tortas-" + UUID.randomUUID();
        Long id = TenantContext.calcular(1L, () -> service.salvar(form(nome)).getId());

        TenantContext.executar(1L, () -> service.alternarAtivo(id));
        assertThat(TenantContext.calcular(1L, () -> service.listarAtivas()))
            .noneMatch(c -> c.getId().equals(id));
        assertThat(TenantContext.calcular(1L, () -> service.listarTodas()))
            .anyMatch(c -> c.getId().equals(id) && !c.getAtivo());

        TenantContext.executar(1L, () -> service.alternarAtivo(id));
        assertThat(TenantContext.calcular(1L, () -> service.listarAtivas()))
            .anyMatch(c -> c.getId().equals(id));
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn test -pl pascoa-monolith -Dtest=CategoriaProdutoServiceTest`
Expected: erro de compilação (`salvar`, `listarTodas`, `alternarAtivo` ausentes).

- [ ] **Step 3: Completar o service**

Em `CategoriaProdutoService`, acrescentar:

```java
    @Transactional(readOnly = true)
    public List<CategoriaProduto> listarTodas() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public CategoriaProduto salvar(CategoriaProduto form) {
        String nome = form.getNome().trim();
        boolean duplicada = form.getId() == null
            ? repository.existsByNomeIgnoreCase(nome)
            : repository.existsByNomeIgnoreCaseAndIdNot(nome, form.getId());
        if (duplicada) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome.");
        }
        CategoriaProduto categoria = form.getId() == null ? new CategoriaProduto() : buscarPorId(form.getId());
        categoria.setNome(nome);
        return repository.save(categoria);
    }

    @Transactional
    public void alternarAtivo(Long id) {
        CategoriaProduto categoria = buscarPorId(id);
        categoria.setAtivo(!categoria.getAtivo());
        repository.save(categoria);
    }
```

- [ ] **Step 4: Controller**

`<main>/cadastro/controller/CategoriaProdutoController.java`:

```java
package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.service.CategoriaProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaProdutoController {

    private final CategoriaProdutoService service;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", service.listarTodas());
        return "categorias/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("categoria", new CategoriaProduto());
        return "categorias/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("categoria", service.buscarPorId(id));
        return "categorias/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("categoria") CategoriaProduto categoria,
                         BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "categorias/form";
        }
        try {
            service.salvar(categoria);
        } catch (IllegalArgumentException e) {
            result.rejectValue("nome", "duplicada", e.getMessage());
            return "categorias/form";
        }
        ra.addFlashAttribute("sucesso", "Categoria salva com sucesso!");
        return "redirect:/categorias";
    }

    @PostMapping("/{id}/ativar")
    public String alternarAtivo(@PathVariable Long id, RedirectAttributes ra) {
        service.alternarAtivo(id);
        ra.addFlashAttribute("sucesso", "Status da categoria alterado!");
        return "redirect:/categorias";
    }
}
```

- [ ] **Step 5: Templates, menu e agrupamento**

`<res>/templates/categorias/lista.html`:

```html
<!DOCTYPE html>
<html th:replace="~{fragments/layout :: layout(~{::title}, ~{::#pageContent})}"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Categorias — Sistema Páscoa</title>
</head>
<body>
<div id="pageContent">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0"><i class="bi bi-tags me-2 text-success" aria-hidden="true"></i>Categorias de produto</h2>
        <a th:href="@{/categorias/novo}" class="btn btn-success">
            <i class="bi bi-plus-lg me-1" aria-hidden="true"></i>Nova categoria
        </a>
    </div>

    <div class="card">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0 tabela-cadastro">
                <thead>
                    <tr>
                        <th scope="col" class="ps-3">Nome</th>
                        <th scope="col">Situação</th>
                        <th scope="col" class="text-center pe-3">Ações</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="c : ${categorias}">
                        <td class="ps-3 fw-semibold" th:text="${c.nome}"></td>
                        <td>
                            <span class="badge" th:classappend="${c.ativo} ? 'text-bg-success' : 'text-bg-secondary'"
                                  th:text="${c.ativo} ? 'Ativa' : 'Inativa'"></span>
                        </td>
                        <td class="text-center pe-3">
                            <a th:href="@{/categorias/{id}/editar(id=${c.id})}"
                               class="btn btn-sm btn-outline-primary me-1" title="Editar" aria-label="Editar">
                                <i class="bi bi-pencil" aria-hidden="true"></i>
                            </a>
                            <form th:action="@{/categorias/{id}/ativar(id=${c.id})}" method="post" class="d-inline">
                                <button type="submit" class="btn btn-sm btn-outline-secondary"
                                        th:title="${c.ativo} ? 'Inativar' : 'Reativar'"
                                        th:aria-label="${c.ativo} ? 'Inativar' : 'Reativar'">
                                    <i class="bi bi-power" aria-hidden="true"></i>
                                </button>
                            </form>
                        </td>
                    </tr>
                    <tr th:if="${#lists.isEmpty(categorias)}">
                        <td colspan="3" class="text-center text-muted py-5">
                            <i class="bi bi-tags fs-3 d-block mb-2 opacity-50" aria-hidden="true"></i>
                            Nenhuma categoria cadastrada.
                        </td>
                    </tr>
                </tbody>
            </table>
          </div>
        </div>
    </div>

</div>
</body>
</html>
```

`<res>/templates/categorias/form.html`:

```html
<!DOCTYPE html>
<html th:replace="~{fragments/layout :: layout(~{::title}, ~{::#pageContent})}"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${categoria.id == null ? 'Nova categoria' : 'Editar categoria'} + ' — Sistema Páscoa'">Formulário</title>
</head>
<body>
<div id="pageContent">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0">
            <i class="bi bi-tags me-2 text-success" aria-hidden="true"></i>
            <span th:text="${categoria.id == null ? 'Nova categoria' : 'Editar categoria'}"></span>
        </h2>
        <a th:href="@{/categorias}" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left me-1" aria-hidden="true"></i>Voltar
        </a>
    </div>

    <div class="card">
        <div class="card-body">
            <form th:action="@{/categorias/salvar}" th:object="${categoria}" method="post">
                <input type="hidden" th:field="*{id}">

                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label fw-semibold" for="nome">Nome <span class="text-danger" aria-hidden="true">*</span></label>
                        <input type="text" class="form-control"
                               th:field="*{nome}" required autocomplete="off" maxlength="60"
                               th:classappend="${#fields.hasErrors('nome')} ? 'is-invalid'"
                               placeholder="Ex.: Salgados fritos, Doces de festa">
                        <div class="invalid-feedback" th:errors="*{nome}"></div>
                    </div>
                </div>

                <hr class="my-4">

                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-success px-4">
                        <i class="bi bi-check-lg me-1" aria-hidden="true"></i>Salvar
                    </button>
                    <a th:href="@{/categorias}" class="btn btn-outline-secondary">Cancelar</a>
                </div>
            </form>
        </div>
    </div>

</div>
</body>
</html>
```

Em `<res>/templates/fragments/layout.html`, no menu Cadastros, logo depois do `<li>` de Produtos (o que contém `th:href="@{/produtos}"`), inserir:

```html
                        <li>
                            <a class="dropdown-item" th:href="@{/categorias}">
                                <i class="bi bi-tags me-2" aria-hidden="true"></i>Categorias
                            </a>
                        </li>
```

Em `<main>/config/LayoutAdvice.java:14`, trocar `uri.startsWith("/materias") || uri.startsWith("/fornecedores")` por `uri.startsWith("/materias") || uri.startsWith("/fornecedores") || uri.startsWith("/categorias")`.

- [ ] **Step 6: Rodar os testes**

Run: `mvn test -pl pascoa-monolith -Dtest='CategoriaProdutoServiceTest,CategoriaProdutoTest,CategoriaTelasTest'`
Expected: todos passando. Acrescentar a `CategoriaTelasTest` as verificações `get("/categorias")` e `get("/categorias/novo")` com status 200 (mesmo padrão do teste existente) e rodar de novo.

- [ ] **Step 7: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(produto): tela para gerenciar categorias

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Validação da Fase A em PostgreSQL e seeds

**Files:**
- Modify: `docs/05-estado-implementacao.md`, `docs/06-schema-banco.md`

- [ ] **Step 1: Migration V17 sobre dados reais (cópia do banco do dev)**

```bash
docker compose up -d postgres
docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f03"
docker compose exec -T postgres psql -U postgres -c "CREATE DATABASE pascoa_f03"
docker compose exec -T postgres pg_dump -U postgres pascoa_monolith | docker compose exec -T postgres psql -U postgres -d pascoa_f03 -q
cd pascoa-monolith && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8086 --security.2fa.enabled=false --spring.datasource.url=jdbc:postgresql://localhost:5432/pascoa_f03"
```

Expected no log: `Migrating schema "public" to version "17 - categorias produto"` e sem erro de `ddl-auto=validate`. Em outro terminal:

```bash
docker compose exec -T postgres psql -U postgres -d pascoa_f03 -c "SELECT count(*) AS produtos_sem_categoria FROM produtos WHERE categoria_id IS NULL AND excluido_em IS NULL;"
docker compose exec -T postgres psql -U postgres -d pascoa_f03 -c "SELECT nome, count(*) FROM categorias_produto cp JOIN produtos p ON p.categoria_id = cp.id GROUP BY nome ORDER BY nome;"
```

Expected: `0` produtos sem categoria e as seis categorias com contagens que somam o total de produtos. Parar a aplicação.

- [ ] **Step 2: Verificação manual (porta 8086, banco `pascoa_f03`)**

Reiniciar a aplicação como acima, entrar como `admin` (`admin123`) e conferir: `/produtos` lista com o nome da categoria, `/produtos/novo` oferece as seis categorias e "Sem categoria", criar e editar um produto mantendo a foto e a categoria, `/categorias` cria "Salgados fritos", inativa e reativa, `/catalogo` filtra por categoria com `?categoria=<id>`, `/analytics/dashboard` mostra o ranking com a categoria. Registrar o que foi observado (usar `curl` com cookie jar ou a skill `agent-browser`). Parar a aplicação.

- [ ] **Step 3: Seeds em bancos novos**

Para cada seed, num banco descartável com o schema até V17:

```bash
for seed in seed-massa-teste seed-cenarios seed-fluxo-pedido seed-mei-realista; do
  docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f03_seed"
  docker compose exec -T postgres psql -U postgres -c "CREATE DATABASE pascoa_f03_seed"
  (cd pascoa-monolith && timeout 120 mvn -q spring-boot:run -Dspring-boot.run.arguments="--server.port=8087 --spring.datasource.url=jdbc:postgresql://localhost:5432/pascoa_f03_seed" > /tmp/f03-seed-boot.log 2>&1 &)
  until curl -fs localhost:8087/actuator/health/readiness > /dev/null; do sleep 3; done
  pkill -f "server.port=8087"
  docker compose exec -T postgres psql -U postgres -d pascoa_f03_seed -v ON_ERROR_STOP=1 < infra/seed/$seed.sql && echo "$seed OK"
done
```

Expected: `OK` nos quatro. Se algum falhar, corrigir o seed (não a migration) e repetir. `seed-cenarios` e `seed-fluxo-pedido` pedem as tabelas da massa? Se o cabeçalho do arquivo disser que precisa rodar depois de `seed-massa-teste`, aplicar nessa ordem no mesmo banco.

- [ ] **Step 4: Limpeza e docs**

```bash
docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f03"
docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f03_seed"
```

Em `docs/05-estado-implementacao.md`, acrescentar a seção `## 28. F0.3 — Doces e salgados` com a Fase A (categorias livres por loja, V17, tela `/categorias`, enum `Categoria` removido, seeds atualizados) e atualizar a lista de migrations e "próxima V18". Em `docs/06-schema-banco.md`, descrever `categorias_produto` e `produtos.categoria_id`. Em `CLAUDE.md`, trocar "próxima: V17" por "próxima: V18".

- [ ] **Step 5: Suíte completa, commit e push**

Run a suíte completa; Expected: 0 falhas.

```bash
git add docs CLAUDE.md
git commit -m "docs: F0.3 fase A validada em PostgreSQL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
git push
```

---

# FASE B — Unidade de venda e sazonal (V18)

### Task 4: Unidade de venda e sazonal

**Files:**
- Create: `<res>/db/migration/V18__unidade_venda_sazonal.sql`, `<main>/cadastro/entity/UnidadeVenda.java`, `<res>/static/js/produto-form.js`
- Modify: `<main>/cadastro/entity/Produto.java`, `<main>/fichaTecnica/entity/FichaTecnica.java:56-59`, `<main>/fichaTecnica/service/FichaTecnicaService.java:45-65`, `<main>/fichaTecnica/controller/FichaTecnicaController.java:53-66`, `<main>/producao/service/ProducaoPdfService.java:41-42`
- Modify templates: `<res>/templates/produtos/form.html`, `produtos/lista.html`, `catalogo/index.html`, `catalogo/produto.html`, `fichas/detalhe.html:44-56,84-90`, `producao/detalhe.html:131-132`, `analytics/dashboard.html`
- Modify tests: `<test>/producao/service/ProducaoStatusPedidoTest.java:88`, `<test>/financeiro/service/CustoRealServiceIntegrationTest.java:90` (remover `.unidadeRendimento(Unidade.UN)`)
- Create tests: `<test>/cadastro/UnidadeVendaTest.java`, `<test>/cadastro/ProdutoUnidadeSazonalTest.java`

**Interfaces:**
- Produces: `UnidadeVenda` com `getDescricao()`, `getSimbolo()`, `isFracionavel()`; `Produto.getUnidadeVenda()`, `Produto.getSazonal()`; `FichaTecnicaService.salvarInfo(Long produtoId, BigDecimal rendimento, String observacoes)` (sem unidade).

- [ ] **Step 1: Testes**

`<test>/cadastro/UnidadeVendaTest.java`:

```java
package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnidadeVendaTest {

    @Test
    void somenteKgAceitaFracao() {
        assertThat(UnidadeVenda.KG.isFracionavel()).isTrue();
        assertThat(UnidadeVenda.UNIDADE.isFracionavel()).isFalse();
        assertThat(UnidadeVenda.DUZIA.isFracionavel()).isFalse();
        assertThat(UnidadeVenda.CENTO.isFracionavel()).isFalse();
        assertThat(UnidadeVenda.PACOTE.isFracionavel()).isFalse();
    }

    @Test
    void simbolosParaExibicao() {
        assertThat(UnidadeVenda.CENTO.getSimbolo()).isEqualTo("cento");
        assertThat(UnidadeVenda.DUZIA.getSimbolo()).isEqualTo("dz");
        assertThat(UnidadeVenda.KG.getSimbolo()).isEqualTo("kg");
    }
}
```

`<test>/cadastro/ProdutoUnidadeSazonalTest.java`:

```java
package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProdutoUnidadeSazonalTest {

    @Autowired private ProdutoService service;

    @Test
    void produtoNovo_nasceUnidadeNaoSazonal() {
        Produto salvo = TenantContext.calcular(1L, () -> service.salvar(
            Produto.builder().nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN).build()));

        assertThat(salvo.getUnidadeVenda()).isEqualTo(UnidadeVenda.UNIDADE);
        assertThat(salvo.getSazonal()).isFalse();
    }

    @Test
    void salvarSazonalComUnidadeCento_persisteOsCampos() {
        LocalDate inicio = LocalDate.of(2026, 3, 1);
        Long id = TenantContext.calcular(1L, () -> service.salvar(Produto.builder()
            .nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN)
            .unidadeVenda(UnidadeVenda.CENTO).sazonal(true).inicioSafra(inicio).build()).getId());

        Produto lido = TenantContext.calcular(1L, () -> service.buscarPorId(id));

        assertThat(lido.getUnidadeVenda()).isEqualTo(UnidadeVenda.CENTO);
        assertThat(lido.getSazonal()).isTrue();
        assertThat(lido.getInicioSafra()).isEqualTo(inicio);
    }

    @Test
    void naoSazonal_limpaAsDatasDeTemporada() {
        Long id = TenantContext.calcular(1L, () -> service.salvar(Produto.builder()
            .nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN)
            .sazonal(false).inicioSafra(LocalDate.of(2026, 3, 1)).fimSafra(LocalDate.of(2026, 4, 30)).build()).getId());

        Produto lido = TenantContext.calcular(1L, () -> service.buscarPorId(id));

        assertThat(lido.getInicioSafra()).isNull();
        assertThat(lido.getFimSafra()).isNull();
    }
}
```

Run: `mvn test -pl pascoa-monolith -Dtest='UnidadeVendaTest,ProdutoUnidadeSazonalTest'`
Expected: erro de compilação (`UnidadeVenda` ausente).

- [ ] **Step 2: Enum e migration**

`<main>/cadastro/entity/UnidadeVenda.java`:

```java
package br.com.seuprojeto.pascoa.cadastro.entity;

public enum UnidadeVenda {
    UNIDADE("Unidade", "un", false),
    DUZIA("Dúzia", "dz", false),
    CENTO("Cento", "cento", false),
    PACOTE("Pacote", "pct", false),
    KG("Quilo", "kg", true);

    private final String descricao;
    private final String simbolo;
    private final boolean fracionavel;

    UnidadeVenda(String descricao, String simbolo, boolean fracionavel) {
        this.descricao = descricao;
        this.simbolo = simbolo;
        this.fracionavel = fracionavel;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public boolean isFracionavel() {
        return fracionavel;
    }
}
```

`<res>/db/migration/V18__unidade_venda_sazonal.sql`:

```sql
ALTER TABLE produtos ADD COLUMN unidade_venda VARCHAR(10) NOT NULL DEFAULT 'UNIDADE';
ALTER TABLE produtos ADD COLUMN sazonal BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE produtos p
SET unidade_venda = CASE f.unidade_rendimento
        WHEN 'KG' THEN 'KG'
        WHEN 'CX' THEN 'PACOTE'
        ELSE 'UNIDADE'
    END
FROM fichas_tecnicas f
WHERE f.produto_id = p.id;

UPDATE produtos SET sazonal = TRUE WHERE inicio_safra IS NOT NULL OR fim_safra IS NOT NULL;

ALTER TABLE fichas_tecnicas ALTER COLUMN unidade_rendimento DROP NOT NULL;
```

- [ ] **Step 3: `Produto`, `ProdutoService` e ficha técnica**

Em `<main>/cadastro/entity/Produto.java`: reacrescentar os imports `jakarta.persistence.EnumType` e `jakarta.persistence.Enumerated` (removidos na Fase A) e `org.springframework.format.annotation.DateTimeFormat`; acrescentar depois do campo `precoVenda`:

```java
    @NotNull(message = "Unidade de venda é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_venda", nullable = false, length = 10)
    @Builder.Default
    private UnidadeVenda unidadeVenda = UnidadeVenda.UNIDADE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean sazonal = false;
```

e anotar os dois campos de temporada com `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` (o `<input type="date">` envia `yyyy-MM-dd`):

```java
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "inicio_safra")
    private LocalDate inicioSafra;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fim_safra")
    private LocalDate fimSafra;
```

Em `ProdutoService.salvar(Produto, MultipartFile)`, depois do bloco da categoria:

```java
        if (produto.getSazonal() == null) {
            produto.setSazonal(false);
        }
        if (!produto.getSazonal()) {
            produto.setInicioSafra(null);
            produto.setFimSafra(null);
        }
```

Em `FichaTecnica.java:56-59`, apagar o campo `unidadeRendimento` (com `@NotNull`, `@Enumerated` e `@Column`) e os imports que ficarem sem uso (`Enumerated`, `EnumType`, `Unidade`). A coluna fica no banco, agora opcional.

Em `FichaTecnicaService`: no `buscarOuCriar` apagar `.unidadeRendimento(Unidade.UN)`; trocar `salvarInfo` por:

```java
    @Transactional
    public void salvarInfo(Long produtoId, BigDecimal rendimento, String observacoes) {
        FichaTecnica ficha = buscarOuCriar(produtoId);
        ficha.setRendimento(rendimento);
        ficha.setObservacoes(observacoes);
        fichaRepository.save(ficha);
    }
```

removendo o import de `Unidade` se ficar sem uso. Em `FichaTecnicaController.salvarInfo`: apagar o parâmetro `@RequestParam Unidade unidadeRendimento` e chamar `fichaService.salvarInfo(produtoId, rendimento, observacoes)`; remover `unidades` do model se o método `detalhe` o expõe só para esse select (`grep -n "unidades" FichaTecnicaController.java`).

Em `<main>/producao/service/ProducaoPdfService.java:41-42`:

```java
                pdf.addInfo(info, "Rendimento da receita:",
                    qtd(ficha.getRendimento()) + " " + ordem.getProduto().getUnidadeVenda().getSimbolo());
```

- [ ] **Step 4: Templates e JS**

`produtos/form.html`: depois do bloco do preço, acrescentar a unidade de venda e a temporada (substituir o `<div class="col-md-4 d-flex align-items-end">` do "Produto Ativo" mantendo-o, e inserir antes dele):

```html
                    <div class="col-md-4">
                        <label class="form-label fw-semibold" for="unidadeVenda">Vendido por *</label>
                        <select class="form-select" th:field="*{unidadeVenda}" required>
                            <option th:each="un : ${unidadesVenda}" th:value="${un.name()}" th:text="${un.descricao}"></option>
                        </select>
                        <div class="form-text">Preço e quantidade do pedido usam esta unidade.</div>
                    </div>

                    <div class="col-md-12">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" th:field="*{sazonal}" id="sazonal">
                            <label class="form-check-label fw-semibold" for="sazonal">Produto sazonal (vendido só em uma temporada)</label>
                        </div>
                        <div id="camposTemporada" class="row g-3 mt-1">
                            <div class="col-md-3">
                                <label class="form-label" for="inicioSafra">Início da temporada</label>
                                <input type="date" class="form-control" th:field="*{inicioSafra}">
                            </div>
                            <div class="col-md-3">
                                <label class="form-label" for="fimSafra">Fim da temporada</label>
                                <input type="date" class="form-control" th:field="*{fimSafra}">
                            </div>
                        </div>
                    </div>
```

e, no fim do `#pageContent` do mesmo arquivo (antes de `</div>` que o fecha), a inclusão do script como as outras telas fazem (`<script th:src="@{/js/produto-form.js}"></script>`; copiar o formato de `estoque/entrada.html`).

`<res>/static/js/produto-form.js`:

```js
(function () {
  var sazonal = document.getElementById('sazonal');
  var campos = document.getElementById('camposTemporada');
  if (!sazonal || !campos) { return; }
  function atualizar() {
    campos.classList.toggle('d-none', !sazonal.checked);
  }
  sazonal.addEventListener('change', atualizar);
  atualizar();
})();
```

Em `ProdutoController`, nos quatro lugares que fazem `model.addAttribute("categorias", ...)`, acrescentar `model.addAttribute("unidadesVenda", UnidadeVenda.values());` (import `br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda`).

Exibição da unidade ao lado do preço:
- `produtos/lista.html`: na célula do preço, acrescentar depois do valor `<small class="text-muted"> / <span th:text="${p.unidadeVenda.simbolo}"></span></small>`.
- `catalogo/index.html` e `catalogo/produto.html`: no preço do produto acrescentar o mesmo trecho com `p`/`produto`. Em `catalogo/produto.html:53`, trocar `th:if="${produto.inicioSafra != null}"` por `th:if="${produto.sazonal and produto.inicioSafra != null}"`.
- `fichas/detalhe.html:44-56`: o input group do rendimento perde o `<select name="unidadeRendimento">` e ganha o rótulo da unidade do produto:

```html
                            <label class="form-label fw-semibold" for="rendimento">Rendimento (em <span th:text="${produto.unidadeVenda.descricao}"></span>) <span class="text-danger" aria-hidden="true">*</span></label>
                            <div class="input-group">
                                <input type="number" id="rendimento" name="rendimento" class="form-control" required inputmode="decimal"
                                       th:value="${ficha != null ? ficha.rendimento : '1'}"
                                       min="0.001" step="0.001" placeholder="Ex: 3">
                                <span class="input-group-text" th:text="${produto.unidadeVenda.simbolo}"></span>
                            </div>
                            <div class="form-text">Quantas unidades de venda esta receita produz.</div>
```

  e em `fichas/detalhe.html:87`, trocar `${ficha.unidadeRendimento.simbolo}` por `${produto.unidadeVenda.simbolo}`.
- `producao/detalhe.html:131-132`: `<span th:text="${ficha.unidadeRendimento.simbolo}"></span>` vira `<span th:text="${ordem.produto.unidadeVenda.simbolo}"></span>`.
- `analytics/dashboard.html`: onde o texto disser "Safra"/"safra" (conferir com `grep -n -i "safra" <res>/templates/analytics/dashboard.html`), trocar por "Período"/"período" nos rótulos exibidos; não mexer em nomes de variável nem na lógica.

- [ ] **Step 5: Testes existentes**

Em `ProducaoStatusPedidoTest.java:88` e `CustoRealServiceIntegrationTest.java:90`, apagar `.unidadeRendimento(Unidade.UN)` (e o import `Unidade` se ficar sem uso). Procurar outros usos: `grep -rn "unidadeRendimento\|UnidadeRendimento" pascoa-monolith/src`.

- [ ] **Step 6: Rodar testes**

Run: `mvn test -pl pascoa-monolith -Dtest='UnidadeVendaTest,ProdutoUnidadeSazonalTest,CategoriaTelasTest'` e depois a suíte completa. Expected: 0 falhas. Acrescentar a `CategoriaTelasTest` (ou a uma classe nova de telas) as requisições `get("/produtos/novo")` e `get("/fichas/{id}")` de um produto com ficha, status 200.

- [ ] **Step 7: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(produto): unidade de venda e produto sazonal (V18)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Validação da Fase B em PostgreSQL e docs

**Files:** `docs/05-estado-implementacao.md`, `docs/06-schema-banco.md`, `CLAUDE.md`

- [ ] **Step 1: Conferir os dados antes da migration**

```bash
docker compose exec -T postgres psql -U postgres -d pascoa_monolith -c "SELECT unidade_rendimento, count(*) FROM fichas_tecnicas GROUP BY 1;"
```

Registrar o resultado no relatório. Se houver G, L ou ML, **parar e reportar** (esses produtos viram UNIDADE e o rendimento muda de sentido; exige decisão do responsável).

- [ ] **Step 2: Migration V18 sobre cópia do dev**

Repetir o procedimento da Task 3 Step 1 (banco `pascoa_f03` copiado do dev; o dev já está em V17 se a Fase A foi aplicada nele, senão a cópia migra V17 e V18 juntas). Expected: `Migrating ... version "18 - unidade venda sazonal"`, `validate` ok, e:

```bash
docker compose exec -T postgres psql -U postgres -d pascoa_f03 -c "SELECT unidade_venda, sazonal, count(*) FROM produtos GROUP BY 1,2 ORDER BY 1,2;"
```

Expected: nenhum produto com `unidade_venda` nula; produtos que tinham `inicio_safra` ou `fim_safra` com `sazonal = t`.

- [ ] **Step 3: Verificação manual**

Na aplicação (porta 8086): criar produto "Coxinha" vendido por Cento, sazonal desmarcado (campos de temporada ocultos) e depois marcado (campos aparecem), salvar; abrir a ficha técnica dele: o rendimento mostra "cento" como unidade e não há mais seletor de unidade; abrir uma ordem de produção do produto e o PDF dela ("Rendimento da receita: N cento"). Registrar o observado. Parar a aplicação e apagar `pascoa_f03`.

- [ ] **Step 4: Docs e commit**

Atualizar a seção 28 de `docs/05-estado-implementacao.md` (Fase B: `UnidadeVenda`, `sazonal`, rendimento na unidade de venda, `unidade_rendimento` sem uso e opcional), `docs/06-schema-banco.md` (V18), e `CLAUDE.md` ("próxima: V19").

```bash
git add docs CLAUDE.md
git commit -m "docs: F0.3 fase B validada em PostgreSQL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
git push
```

---

# FASE C — Quantidade decimal (V19)

### Task 6: Regras de quantidade (validação e formatação)

**Files:**
- Create: `<main>/common/quantidade/Quantidades.java`, `<main>/common/quantidade/QuantidadeFormatter.java`
- Create test: `<test>/common/quantidade/QuantidadesTest.java`

**Interfaces:**
- Produces: `Quantidades.validar(BigDecimal quantidade, UnidadeVenda unidade)` (lança `IllegalArgumentException` com mensagem em português), `Quantidades.formatar(BigDecimal)` (pt-BR, vírgula, sem zeros à direita, `""` para nulo), `Quantidades.formatar(BigDecimal, UnidadeVenda)` (`"2 cento"`); bean `fmt` com `quantidade(BigDecimal)` e `quantidade(BigDecimal, UnidadeVenda)` para o Thymeleaf (`${@fmt.quantidade(x)}`).

- [ ] **Step 1: Teste**

```java
package br.com.seuprojeto.pascoa.common.quantidade;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantidadesTest {

    @Test
    void inteiroEhValidoParaTodasAsUnidades() {
        for (UnidadeVenda un : UnidadeVenda.values()) {
            assertThatCode(() -> Quantidades.validar(new BigDecimal("2"), un)).doesNotThrowAnyException();
            assertThatCode(() -> Quantidades.validar(new BigDecimal("2.000"), un)).doesNotThrowAnyException();
        }
    }

    @Test
    void fracaoSoParaKg() {
        assertThatCode(() -> Quantidades.validar(new BigDecimal("1.5"), UnidadeVenda.KG)).doesNotThrowAnyException();
        assertThatCode(() -> Quantidades.validar(new BigDecimal("0.001"), UnidadeVenda.KG)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Quantidades.validar(new BigDecimal("1.5"), UnidadeVenda.CENTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cento só aceita quantidade inteira.");
    }

    @Test
    void zeroNegativoENuloSaoRejeitados() {
        assertThatThrownBy(() -> Quantidades.validar(BigDecimal.ZERO, UnidadeVenda.KG))
            .hasMessage("Quantidade deve ser maior que zero.");
        assertThatThrownBy(() -> Quantidades.validar(new BigDecimal("-1"), UnidadeVenda.UNIDADE))
            .hasMessage("Quantidade deve ser maior que zero.");
        assertThatThrownBy(() -> Quantidades.validar(null, UnidadeVenda.UNIDADE))
            .hasMessage("Quantidade deve ser maior que zero.");
    }

    @Test
    void maisDeTresCasasEhRejeitado() {
        assertThatThrownBy(() -> Quantidades.validar(new BigDecimal("1.2345"), UnidadeVenda.KG))
            .hasMessage("Quantidade aceita no máximo 3 casas decimais.");
    }

    @Test
    void formatar_naoMostraZerosAEsquerdaDaVirgulaNemADireita() {
        assertThat(Quantidades.formatar(new BigDecimal("2.000"))).isEqualTo("2");
        assertThat(Quantidades.formatar(new BigDecimal("1.500"))).isEqualTo("1,5");
        assertThat(Quantidades.formatar(new BigDecimal("0.250"))).isEqualTo("0,25");
        assertThat(Quantidades.formatar(new BigDecimal("100"))).isEqualTo("100");
        assertThat(Quantidades.formatar(new BigDecimal("1E+2"))).isEqualTo("100");
        assertThat(Quantidades.formatar(null)).isEmpty();
    }

    @Test
    void formatar_comUnidade() {
        assertThat(Quantidades.formatar(new BigDecimal("2.000"), UnidadeVenda.CENTO)).isEqualTo("2 cento");
        assertThat(Quantidades.formatar(new BigDecimal("1.5"), UnidadeVenda.KG)).isEqualTo("1,5 kg");
    }
}
```

Run: `mvn test -pl pascoa-monolith -Dtest=QuantidadesTest`
Expected: erro de compilação.

- [ ] **Step 2: Implementar**

`<main>/common/quantidade/Quantidades.java`:

```java
package br.com.seuprojeto.pascoa.common.quantidade;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;

import java.math.BigDecimal;

public final class Quantidades {

    private static final int CASAS_MAXIMAS = 3;

    private Quantidades() {
    }

    public static void validar(BigDecimal quantidade, UnidadeVenda unidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        int casas = Math.max(quantidade.stripTrailingZeros().scale(), 0);
        if (casas > CASAS_MAXIMAS) {
            throw new IllegalArgumentException("Quantidade aceita no máximo 3 casas decimais.");
        }
        if (casas > 0 && !unidade.isFracionavel()) {
            throw new IllegalArgumentException(unidade.getDescricao() + " só aceita quantidade inteira.");
        }
    }

    public static String formatar(BigDecimal quantidade) {
        if (quantidade == null) {
            return "";
        }
        BigDecimal limpa = quantidade.stripTrailingZeros();
        if (limpa.scale() < 0) {
            limpa = limpa.setScale(0);
        }
        return limpa.toPlainString().replace('.', ',');
    }

    public static String formatar(BigDecimal quantidade, UnidadeVenda unidade) {
        return formatar(quantidade) + " " + unidade.getSimbolo();
    }
}
```

`<main>/common/quantidade/QuantidadeFormatter.java`:

```java
package br.com.seuprojeto.pascoa.common.quantidade;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("fmt")
public class QuantidadeFormatter {

    public String quantidade(BigDecimal valor) {
        return Quantidades.formatar(valor);
    }

    public String quantidade(BigDecimal valor, UnidadeVenda unidade) {
        return Quantidades.formatar(valor, unidade);
    }
}
```

- [ ] **Step 3: Rodar e commitar**

Run: `mvn test -pl pascoa-monolith -Dtest=QuantidadesTest` — Expected: 6 testes passando.

```bash
git add pascoa-monolith/src
git commit -m "feat(quantidade): validação por unidade de venda e formatação pt-BR

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Migration V19 e tipos decimais no domínio e nos serviços

**Files:**
- Create: `<res>/db/migration/V19__quantidade_decimal.sql`
- Modify entidades: `<main>/pedido/entity/ItemPedido.java:46-47,60-65`, `<main>/orcamento/entity/OrcamentoItem.java:47-48,59-64`, `<main>/producao/entity/OrdemProducao.java:49-50`
- Modify DTOs: `<main>/pedido/dto/ItemPedidoForm.java`, `<main>/orcamento/dto/OrcamentoItemForm.java`, `<main>/financeiro/dto/CustoRealDto.java:31`, `<main>/financeiro/dto/TopProdutoDto.java`, `<main>/analytics/dto/RankingProdutoDto.java`
- Modify services: `PedidoService` (`criarComItens`, `adicionarItem`, `avisosProducao`), `OrcamentoService` (`adicionarItens`), `ProducaoService` (`calcularReceita`, concluir/`verificar`), `CustoRealService`, `BreakevenService`, `FinanceiroService`, `AnalyticsService`, `PedidoController:77`
- Modify query: `<main>/pedido/repository/ItemPedidoRepository.java` (`rankingProdutosPorAno`)
- Modify tests (compile): `PedidoStateMachineTest:86-92`, `ProducaoStatusPedidoTest:100`, `AgingDerivadoTest:66`, `SoftDeleteHistoricoTest:67,136`, `PainelDoDiaTest:64`, `OrcamentoServiceIntegrationTest:69`, `OrcamentoPdfServiceTest:29`, `ProducaoReceitaTest:28`, `CustoRealServiceIntegrationTest:106,142`, `QueriesNativasIsolamentoTest:100`
- Create tests: `<test>/pedido/service/QuantidadeDecimalTest.java`; novo caso em `<test>/producao/service/ProducaoReceitaTest.java`

**Interfaces:**
- Consumes: `Quantidades.validar` (Task 6), `Produto.getUnidadeVenda()` (Task 4).
- Produces: `ItemPedido`, `OrcamentoItem`, `OrdemProducao` com `BigDecimal getQuantidade()`; `PedidoService.criarComItens(Long, LocalDate, LocalTime, String, List<Long>, List<BigDecimal>)`; `PedidoService.adicionarItem(Long, Long, BigDecimal)`; `OrcamentoItemForm.getQuantidade()` e `ItemPedidoForm.getQuantidade()` como `BigDecimal`.

- [ ] **Step 1: Testes novos**

`<test>/pedido/service/QuantidadeDecimalTest.java` (siga o setup de `PedidoStateMachineTest`: `@SpringBootTest @ActiveProfiles("test") @Transactional`, `EntityManager em`, `ClienteRepository`, `ProdutoRepository`, `PedidoService`; copiar de lá a criação do `cliente` no `@BeforeEach`):

```java
    @Test
    void itemEmKg_aceitaFracao_eCalculaSubtotalEmDuasCasas() {
        Produto bolo = produtos.save(Produto.builder().nome("Bolo-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());

        Pedido pedido = pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(bolo.getId()), List.of(new BigDecimal("1.5")));
        em.flush();
        em.clear();

        Pedido lido = pedidoService.buscarPorId(pedido.getId());
        assertThat(lido.getItens()).hasSize(1);
        assertThat(lido.getItens().get(0).getQuantidade()).isEqualByComparingTo("1.5");
        assertThat(lido.getItens().get(0).getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(lido.getTotalPedido()).isEqualByComparingTo("60.00");
    }

    @Test
    void itemEmCento_rejeitaFracao() {
        Produto coxinha = produtos.save(Produto.builder().nome("Coxinha-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("80.00")).unidadeVenda(UnidadeVenda.CENTO).build());

        assertThatThrownBy(() -> pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(coxinha.getId()), List.of(new BigDecimal("1.5"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cento só aceita quantidade inteira.");
    }

    @Test
    void adicionarItem_validaPelaUnidadeDoProduto() {
        Produto brigadeiro = produtos.save(Produto.builder().nome("Brigadeiro-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("30.00")).unidadeVenda(UnidadeVenda.DUZIA).build());
        Produto bolo = produtos.save(Produto.builder().nome("Bolo-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());
        Pedido pedido = pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(brigadeiro.getId()), List.of(new BigDecimal("2")));

        assertThatThrownBy(() -> pedidoService.adicionarItem(pedido.getId(), bolo.getId(), BigDecimal.ZERO))
            .hasMessage("Quantidade deve ser maior que zero.");
        pedidoService.adicionarItem(pedido.getId(), bolo.getId(), new BigDecimal("0.75"));
        em.flush();
        em.clear();

        assertThat(pedidoService.buscarPorId(pedido.getId()).getTotalPedido()).isEqualByComparingTo("90.00");
    }
```

(60,00 do brigadeiro em dúzia: 2 × 30,00 = 60,00; mais 0,75 × 40,00 = 30,00; total 90,00.)

Em `<test>/producao/service/ProducaoReceitaTest.java`, novo caso depois do existente:

```java
    @Test
    void calcularReceita_aceitaQuantidadeFracionada() {
        MateriaPrima comCusto = MateriaPrima.builder().nome("Chocolate").unidade(Unidade.KG)
            .quantidadeAtual(new BigDecimal("3")).custoUnitario(new BigDecimal("10")).build();
        FichaTecnica ficha = FichaTecnica.builder().rendimento(new BigDecimal("5")).itens(List.of(
            FichaTecnicaItem.builder().materiaPrima(comCusto).quantidade(new BigDecimal("2")).build())).build();
        OrdemProducao ordem = OrdemProducao.builder().quantidade(new BigDecimal("2.5")).build();

        var r = service.calcularReceita(ordem, ficha);

        assertThat(r.linhas().get(0).qtdNecessaria()).isEqualByComparingTo("1.000");
        assertThat(r.linhas().get(0).custo()).isEqualByComparingTo("10.00");
        assertThat(r.linhas().get(0).estoqueOk()).isTrue();
        assertThat(r.custoPorUnidade()).isEqualByComparingTo("4.00");
    }
```

e ajustar o caso existente: `OrdemProducao.builder().quantidade(10)` vira `.quantidade(BigDecimal.TEN)`.

- [ ] **Step 2: Migration**

`<res>/db/migration/V19__quantidade_decimal.sql`:

```sql
ALTER TABLE itens_pedido ALTER COLUMN quantidade TYPE NUMERIC(10,3);
ALTER TABLE orcamento_itens ALTER COLUMN quantidade TYPE NUMERIC(10,3);
ALTER TABLE ordens_producao ALTER COLUMN quantidade TYPE NUMERIC(10,3);
```

- [ ] **Step 3: Entidades e DTOs**

`ItemPedido.java`: `@Column(nullable = false) private Integer quantidade;` vira

```java
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidade;
```

e o cálculo do subtotal:

```java
    @PrePersist
    @PreUpdate
    private void calcularSubtotal() {
        if (quantidade != null && precoUnitario != null) {
            this.subtotal = precoUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
        }
    }
```

(import `java.math.RoundingMode`). `OrcamentoItem.java`: mesma troca do campo e do cálculo (`subtotal = precoUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);`). `OrdemProducao.java`: o campo vira `@Column(nullable = false, precision = 10, scale = 3) private BigDecimal quantidade;` (import `java.math.BigDecimal`).

`ItemPedidoForm.java`:

```java
    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;
```

(trocar os imports `Min`/`Integer` por `DecimalMin` e `java.math.BigDecimal`). `OrcamentoItemForm.java`: mesma troca no campo `quantidade`. `CustoRealDto.LinhaCustoMpDto`: `private int quantidade;` vira `private BigDecimal quantidade;`. `TopProdutoDto`: `record TopProdutoDto(String nome, BigDecimal quantidadeVendida, BigDecimal faturamento)`. `RankingProdutoDto`: `record RankingProdutoDto(String nome, String categoria, BigDecimal quantidade, BigDecimal faturamento)`.

- [ ] **Step 4: Serviços**

`PedidoService`:
- `criarComItens`: o parâmetro `List<Integer> quantidades` vira `List<BigDecimal> quantidades`; no laço trocar o corpo por

```java
            Long produtoId = produtoIds.get(i);
            BigDecimal qtd = (quantidades != null && i < quantidades.size())
                    ? quantidades.get(i) : BigDecimal.ONE;
            if (produtoId == null || qtd == null || qtd.signum() <= 0) { continue; }

            Produto produto = produtosPorId.get(produtoId);
            Quantidades.validar(qtd, produto.getUnidadeVenda());
            ItemPedido item = ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidade(qtd)
                .precoUnitario(produto.getPrecoVenda())
                .build();
            itens.add(item);
            total = total.add(produto.getPrecoVenda().multiply(qtd).setScale(2, RoundingMode.HALF_UP));
```

  (import `br.com.seuprojeto.pascoa.common.quantidade.Quantidades` e `java.math.RoundingMode`; apagar a antiga linha `total = total.add(...BigDecimal.valueOf(qtd))` e a construção duplicada do `ItemPedido`).
- `adicionarItem(Long pedidoId, Long produtoId, BigDecimal quantidade)`: depois de carregar o `produto` e antes do `ItemPedido.builder()`, `Quantidades.validar(quantidade, produto.getUnidadeVenda());`.
- `avisosProducao`: `BigDecimal qtd = BigDecimal.valueOf(item.getQuantidade());` vira `BigDecimal qtd = item.getQuantidade();`.

`PedidoController:77`: `List<Integer> quantidades` vira `List<BigDecimal> quantidades` (import `java.math.BigDecimal`).

`OrcamentoService.adicionarItens`: trocar

```java
            Integer qtd = itemForm.getQuantidade() != null ? itemForm.getQuantidade() : 1;
            if (qtd <= 0) {
                continue;
            }
```

por

```java
            BigDecimal qtd = itemForm.getQuantidade() != null ? itemForm.getQuantidade() : BigDecimal.ONE;
            if (qtd.signum() <= 0) {
                continue;
            }
```

e, depois de carregar o `produto`, `Quantidades.validar(qtd, produto.getUnidadeVenda());` (o resto do método — preço e `OrcamentoItem.builder()...quantidade(qtd)` — continua).

`ProducaoService`:
- `calcularReceita` (linha 83): `BigDecimal qtdOrdem = ordem.getQuantidade();` e (linha 100) `BigDecimal porUnidade = qtdOrdem.signum() > 0 ? total.divide(qtdOrdem, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;`.
- No método que conclui a ordem (linha ~171): `BigDecimal qtdOrdem = ordem.getQuantidade();`; o `motivoBase` (linha ~183) vira

```java
        String motivoBase = "Produção: " + Quantidades.formatar(ordem.getQuantidade(), ordem.getProduto().getUnidadeVenda()) +
                            " de " + ordem.getProduto().getNome() +
                            " | Ordem #" + ordem.getId();
```

(import `br.com.seuprojeto.pascoa.common.quantidade.Quantidades`).

`CustoRealService`: linha 48 `BigDecimal subtotal = custoUnit.multiply(item.getQuantidade());`; linha 68 `BigDecimal qtdPedido = pedido.getItens().stream().map(ItemPedido::getQuantidade).reduce(BigDecimal.ZERO, BigDecimal::add);` e `rateioFixo = rateioUnit.multiply(qtdPedido).setScale(2, RoundingMode.HALF_UP);`; `contarUnidadesMes` passa a devolver `BigDecimal` (`.map(ItemPedido::getQuantidade).reduce(BigDecimal.ZERO, BigDecimal::add)` no lugar de `mapToLong(...).sum()`), e o uso `totalUnidadesMes > 0` vira `totalUnidadesMes.signum() > 0` com `totalFixoMensal.divide(totalUnidadesMes, 4, RoundingMode.HALF_UP)`. (import `br.com.seuprojeto.pascoa.pedido.entity.ItemPedido` se faltar.)

`BreakevenService:60`: `long totalUnidades` vira `BigDecimal totalUnidades = ...map(ItemPedido::getQuantidade).reduce(BigDecimal.ZERO, BigDecimal::add);` e cada uso seguinte (`totalUnidades > 0`, `BigDecimal.valueOf(totalUnidades)`) vira `totalUnidades.signum() > 0` / `totalUnidades` (compilar e corrigir cada ocorrência; `grep -n totalUnidades` no arquivo).

`FinanceiroService` (linha ~49-54): `((Number) r[1]).longValue()` vira `toBigDecimal(r[1])`, usando o mesmo helper `toBigDecimal(Object)` que `AnalyticsService` tem (copiar o método privado: devolve `BigDecimal.ZERO` para nulo, `(BigDecimal)` direto, ou `new BigDecimal(value.toString())`). `AnalyticsService.rankingProdutos`: `((Number) r[2]).longValue()` vira `toBigDecimal(r[2])`.

`ItemPedidoRepository.rankingProdutosPorAno`: trocar `SUM(i.quantidade)::bigint` por `SUM(i.quantidade)`.

- [ ] **Step 5: Corrigir os testes existentes (compilação)**

- `List.of(2)` / `List.of(1)` passados a `criarComItens` viram `List.of(new BigDecimal("2"))` / `List.of(BigDecimal.ONE)` (`PedidoStateMachineTest:92`, `ProducaoStatusPedidoTest:100+`, `AgingDerivadoTest:66+`, `SoftDeleteHistoricoTest:67+`, `PainelDoDiaTest:64+`).
- `SoftDeleteHistoricoTest:136`: `adicionarItem(pedidoId, outro.getId(), 1)` vira `... , BigDecimal.ONE)`.
- `OrcamentoServiceIntegrationTest:69`: `itemForm.setQuantidade(qtd)` — mudar o parâmetro/variável `qtd` do helper para `BigDecimal` e os chamadores (`BigDecimal.valueOf(n)`).
- `OrcamentoPdfServiceTest:29`, `QueriesNativasIsolamentoTest:100`: `.quantidade(2)` vira `.quantidade(new BigDecimal("2"))`.
- `CustoRealServiceIntegrationTest:106,142`: a variável `qtd` passa a `BigDecimal`; a asserção `isEqualTo(2)` vira `isEqualByComparingTo("2")`.

- [ ] **Step 6: Rodar**

Run: `mvn test -pl pascoa-monolith -Dtest='QuantidadeDecimalTest,ProducaoReceitaTest'` — Expected: passando. Depois a suíte completa — Expected: 0 falhas. Falhas de compilação restantes são usos de `Integer`/`int` da quantidade: corrigir com os mesmos padrões deste passo (`grep -rn "getQuantidade()" pascoa-monolith/src/main/java` e `grep -rn "Integer quantidade\|int quantidade" pascoa-monolith/src`).

- [ ] **Step 7: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(quantidade): quantidade decimal em pedidos, orçamentos e produção (V19)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Telas, JavaScript e PDFs

**Files:**
- Modify JS: `<res>/static/js/pedido-wizard.js:72-92,111-116,200-201,223-227`, `<res>/static/js/orcamento-form.js:13`
- Modify templates: `dashboard.html:86`, `acompanhamento/pedido.html:126,185`, `pedidos/detalhe.html:175,219-221,349`, `pedidos/wizard.html:150-161,175,301`, `producao/fila.html:53`, `producao/kanban.html:36,87,139`, `producao/detalhe.html:39,133`, `qualidade/inspecao-detalhe.html:47,104,115`, `qualidade/inspecao-form.html:31`, `financeiro/custo-real.html:73`, `financeiro/dashboard.html:310`, `orcamentos/detalhe.html:98`, `orcamentos/form.html:100-104,151`, `orcamentos/aprovacao.html:102`, `analytics/dashboard.html:111`
- Modify PDFs: `<main>/pedido/service/ExportService.java:208`, `<main>/producao/service/ProducaoPdfService.java:31`, `<main>/orcamento/service/OrcamentoPdfService.java:38`
- Create test: `<test>/cadastro/controller/QuantidadeTelasTest.java`

**Interfaces:**
- Consumes: bean `fmt` (`${@fmt.quantidade(x)}`, `${@fmt.quantidade(x, unidade)}`) e `Quantidades.formatar` (Task 6).

- [ ] **Step 1: Teste de renderização**

`<test>/cadastro/controller/QuantidadeTelasTest.java`: `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")`, `@WithMockUser(roles = "ADMIN")`, mesma estrutura de `CategoriaTelasTest`. Criar (dentro de `TenantContext.executar(1L, ...)`) um cliente, um produto em KG (`Bolo-<uuid>`, preço 40,00) e um pedido com `1,5` desse produto via `PedidoService.criarComItens`, e uma ordem de produção gerada ao confirmar o pedido não é necessária. Assertar:

```java
        mvc.perform(get("/pedidos/" + pedidoId)).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")));
        mvc.perform(get("/pedidos/novo")).andExpect(status().isOk());
        mvc.perform(get("/orcamentos/novo")).andExpect(status().isOk());
```

(O texto `1,5 kg` vem do `fmt.quantidade(item.quantidade, item.produto.unidadeVenda)` em `pedidos/detalhe.html`.) Usar a rota real do detalhe do pedido (`grep -n "@GetMapping" pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa/pedido/controller/PedidoController.java`).

Run: `mvn test -pl pascoa-monolith -Dtest=QuantidadeTelasTest` — Expected: falha (a tela ainda mostra `1.500`).

- [ ] **Step 2: Templates**

Trocar a exibição de quantidade pelo bean `fmt`, mostrando a unidade quando houver o produto:

- `pedidos/detalhe.html:175`, `orcamentos/detalhe.html:98`, `orcamentos/aprovacao.html:102`, `acompanhamento/pedido.html:126`: `th:text="${item.quantidade}"` vira `th:text="${@fmt.quantidade(item.quantidade, item.produto.unidadeVenda)}"`.
- `pedidos/detalhe.html:349`, `acompanhamento/pedido.html:185`, `producao/fila.html:53`: `th:text="${o.quantidade}"` vira `th:text="${@fmt.quantidade(o.quantidade, o.produto.unidadeVenda)}"`.
- `producao/kanban.html:36,87,139`: `<span th:text="${o.quantidade}"></span> unid.` vira `<span th:text="${@fmt.quantidade(o.quantidade, o.produto.unidadeVenda)}"></span>` (remover o texto "unid." que segue o `</span>`).
- `producao/detalhe.html:39`: `${ordem.quantidade}` vira `${@fmt.quantidade(ordem.quantidade, ordem.produto.unidadeVenda)}`; linha 133: `<strong th:text="${ordem.quantidade}"></strong> unidade(s)` vira `<strong th:text="${@fmt.quantidade(ordem.quantidade, ordem.produto.unidadeVenda)}"></strong>`.
- `qualidade/inspecao-detalhe.html:47`: `${inspecao.ordemProducao.quantidade} + ' un.'` vira `${@fmt.quantidade(inspecao.ordemProducao.quantidade, inspecao.ordemProducao.produto.unidadeVenda)}`; linhas 104 e 115: `th:text="${inspecao.ordemProducao.quantidade}"` vira o mesmo bean com a unidade, e o texto que segue o `</strong>` ("unidades", se houver) é removido.
- `qualidade/inspecao-form.html:31`: `${ordem.quantidade} + ' un.'` vira `${@fmt.quantidade(ordem.quantidade, ordem.produto.unidadeVenda)}`.
- `dashboard.html:86`: `${o.quantidade} + 'x ' + ${o.produto.nome}` vira `${@fmt.quantidade(o.quantidade, o.produto.unidadeVenda)} + ' · ' + ${o.produto.nome}`.
- `financeiro/custo-real.html:73`, `analytics/dashboard.html:111`, `financeiro/dashboard.html:310`: `th:text="${linha.quantidade}"` / `${r.quantidade()}` / `${tp.quantidadeVendida}` viram `${@fmt.quantidade(linha.quantidade)}` / `${@fmt.quantidade(r.quantidade())}` / `${@fmt.quantidade(tp.quantidadeVendida)}` (somas mistas de unidades: sem rótulo).
- Entrada de quantidade, `pedidos/detalhe.html:219-221`: o `<input ... th:field="*{quantidade}" min="1" placeholder="1">` vira `min="0.001" step="any" placeholder="1"`; `orcamentos/form.html:100-104` e `:151`: nos dois `<input type="number" min="1" ...>` trocar `min="1"` por `min="0.001" step="any"` (e no `th:value` da linha 103 usar `${@fmt.quantidade(item.quantidade).replace(',', '.')}`).
- `pedidos/wizard.html:150-161`: o `<option>` do produto ganha os atributos `th:data-fracionavel="${p.unidadeVenda.fracionavel}"` e `th:data-unidade="${p.unidadeVenda.simbolo}"`; o input `#inpQtd` troca `min="1" max="999" value="1"` por `min="0.001" step="1" value="1"`, e o rótulo `Qtd` ganha um `<span id="lblUnidade" class="text-muted small"></span>` depois do texto.

- [ ] **Step 3: JavaScript**

`<res>/static/js/pedido-wizard.js`: no trecho de adicionar item (linhas 72-92):

```js
  const qtd = parseFloat(qtdInput.value);
  ...
  if (!qtd || qtd <= 0) { qtdInput.classList.add('is-invalid'); return; }
```

(trocar `parseInt` por `parseFloat` e `qtd < 1` por `qtd <= 0`; manter o restante). Acrescentar, no mesmo arquivo, depois da definição de `qtdInput` no escopo do módulo, o ajuste por unidade ao trocar de produto:

```js
const selProduto = document.getElementById('selProduto');
const lblUnidade = document.getElementById('lblUnidade');
selProduto.addEventListener('change', function () {
  const opt = selProduto.selectedOptions[0];
  const fracionavel = opt && opt.dataset.fracionavel === 'true';
  const qtdEl = document.getElementById('inpQtd');
  qtdEl.step = fracionavel ? '0.001' : '1';
  qtdEl.min = fracionavel ? '0.001' : '1';
  lblUnidade.textContent = opt && opt.dataset.unidade ? '(' + opt.dataset.unidade + ')' : '';
});
```

Em todos os lugares que exibem `item.quantidade` (linhas 116 e 227 do arquivo), trocar `${item.quantidade}` por `${String(item.quantidade).replace('.', ',')}`; o valor enviado no formulário (linha 201, `inp2.value = item.quantidade`) continua com ponto. Confirmar que `sub = item.quantidade * item.preco` (linhas 111 e 223) é exibido com `toFixed(2)` como já era.

`<res>/static/js/orcamento-form.js:13`: `parseInt(qtd && qtd.value || 0, 10)` vira `parseFloat(qtd && qtd.value || 0)`.

- [ ] **Step 4: PDFs**

- `ExportService.java:208` e `OrcamentoPdfService.java:38`: `String.valueOf(item.getQuantidade())` vira `Quantidades.formatar(item.getQuantidade(), item.getProduto().getUnidadeVenda())` (import `br.com.seuprojeto.pascoa.common.quantidade.Quantidades`).
- `ProducaoPdfService.java:31`: `ordem.getQuantidade() + " unidade(s)"` vira `Quantidades.formatar(ordem.getQuantidade(), ordem.getProduto().getUnidadeVenda())`.
- Rodar `OrcamentoPdfServiceTest` e corrigir qualquer asserção que dependa do texto da quantidade.

- [ ] **Step 5: Rodar**

Run: `mvn test -pl pascoa-monolith -Dtest='QuantidadeTelasTest,OrcamentoPdfServiceTest,RolePermissionsTest'` e a suíte completa. Expected: 0 falhas.

- [ ] **Step 6: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(quantidade): telas, JS e PDFs com quantidade decimal e unidade de venda

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Validação final em PostgreSQL, docs e push

**Files:** `docs/05-estado-implementacao.md`, `docs/06-schema-banco.md`, `CLAUDE.md`

- [ ] **Step 1: Migration V19 e suíte**

Rodar a suíte completa (Expected: 0 falhas). Repetir o procedimento da Task 3 Step 1 (banco `pascoa_f03` copiado do dev) e verificar que o log mostra `version "19 - quantidade decimal"` e que o `ddl-auto=validate` passa; conferir:

```bash
docker compose exec -T postgres psql -U postgres -d pascoa_f03 -c "SELECT table_name, data_type, numeric_precision, numeric_scale FROM information_schema.columns WHERE column_name = 'quantidade' AND table_name IN ('itens_pedido','orcamento_itens','ordens_producao');"
```

Expected: três linhas `numeric | 10 | 3`.

- [ ] **Step 2: Verificação manual ponta a ponta (porta 8086)**

Com o banco `pascoa_f03`: criar produto "Bolo" vendido por Quilo (preço 40,00) com ficha técnica de rendimento 2 e um insumo; criar pedido com 1,5 kg (wizard: o campo aceita fração só para o produto em kg e mostra a unidade); confirmar que o detalhe mostra "1,5 kg" e total R$ 60,00; confirmar o pedido e abrir a ordem de produção ("1,5 kg", receita escalada: insumo × 1,5 ÷ 2); gerar o PDF da ordem e do pedido; criar produto "Coxinha" por Cento e tentar 1,5 (deve recusar com "Cento só aceita quantidade inteira."); criar orçamento com os dois itens e converter em pedido; conferir `/financeiro/dashboard`, `/analytics/dashboard` e `/financeiro/custo-real/<id>` sem erro. Registrar o observado.

- [ ] **Step 3: Seeds**

Reaplicar os quatro seeds em bancos novos como na Task 3 Step 3 (agora com V19) e conferir `OK` nos quatro. Se algum seed insere `quantidade` de forma incompatível, corrigir o seed.

- [ ] **Step 4: Limpeza, docs e push**

```bash
docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f03"
docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f03_seed"
```

Atualizar `docs/05-estado-implementacao.md` (seção 28: Fase C, regras de fração por unidade, `fmt`/`Quantidades`, limitação conhecida: o rateio de despesa fixa e o ponto de equilíbrio somam quantidades de unidades de venda diferentes, o que distorce a conta quando a loja mistura cento e kg; usar a receita como base fica para um item futuro), `docs/06-schema-banco.md` (V19) e `CLAUDE.md` ("próxima: V20"). Marcar o F0.3 como concluído em `docs/superpowers/specs/2026-10-05-backlog-encomendas-design.md`.

```bash
git add docs CLAUDE.md
git commit -m "docs: F0.3 concluído (categorias, unidade de venda e quantidade decimal)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
git push
```

---

## Self-Review

**Cobertura da spec:** categorias livres por loja, tabela, vínculo, migration, remoção do enum, tela de gestão, templates e seeds (A1-A3); `UnidadeVenda`, `sazonal`, rendimento na unidade de venda, `unidade_rendimento` opcional, "Safra" para "Período" (B1-B2); quantidade `NUMERIC(10,3)` em itens de pedido, orçamento e ordens, validação por unidade, formatação, pontos de cálculo, query de ranking (C1-C4). A tela e a regra de unidades alternativas, variações e estoque de produto acabado ficam fora, como na spec.

**Divergências deliberadas da spec:** (1) `FinanceiroService:82` não muda: a linha multiplica quantidade de `FichaTecnicaItem` (já decimal), não de `ItemPedido`. (2) A entrada decimal usa `type="number"` (o navegador envia ponto), então não há parsing de vírgula no servidor; a vírgula é só de exibição. (3) Categoria do produto é opcional, como a spec diz; antes era obrigatória.

**Consistência de tipos:** `OrdemProducao.getQuantidade()`, `ItemPedido.getQuantidade()` e `OrcamentoItem.getQuantidade()` passam todos a `BigDecimal` na Task 7; `gerarOrdens` copia `item.getQuantidade()` sem mudança. `Quantidades.validar` e `formatar` têm a mesma assinatura em C1, C2 e C3. `PedidoService.criarComItens` recebe `List<BigDecimal>` em C2 e o `PedidoController` é ajustado na mesma tarefa.

**Pontos que dependem de verificação na execução:** (1) a edição de produto por objeto destacado com `@TenantId` (A1, Step 11, com parada explícita); (2) o dado real de `fichas_tecnicas.unidade_rendimento` antes da V18 (B2, Step 1, com parada explícita); (3) a assinatura dos métodos de `BreakevenService` que usam `totalUnidades` (C2, Step 4: compilar e corrigir ocorrência por ocorrência).
