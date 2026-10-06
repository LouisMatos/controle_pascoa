# F0.1 Multi-tenant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Isolar os dados por loja no mesmo banco e na mesma aplicação, mantendo o sistema atual funcionando como a loja 1.

**Architecture:** `@TenantId` do Hibernate 6 numa superclasse `TenantEntity` (coluna `loja_id`). Um `TenantContext` (ThreadLocal) alimenta o `CurrentTenantIdentifierResolver`. Um `TenantFilter` dentro da cadeia do Spring Security define o tenant (usuário logado, token público ou catálogo) antes do Open-Session-In-View abrir a sessão, porque a sessão do Hibernate fixa o tenant ao abrir. Jobs, `@Async` e a fila de campanha propagam o tenant explicitamente.

**Tech Stack:** Java 21, Spring Boot 3.3.4 (Hibernate 6.5), Spring Security 6, Spring Data JPA, Flyway, PostgreSQL 16, H2 nos testes, JUnit 5 + AssertJ + Mockito.

## Global Constraints

- Pacote base `br.com.seuprojeto.pascoa`; módulo `pascoa-monolith`. Caminhos abaixo relativos a `pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa` (abreviado `<main>`) e `pascoa-monolith/src/test/java/br/com/seuprojeto/pascoa` (abreviado `<test>`).
- `@RequiredArgsConstructor`, nunca `@Autowired` no código de produção. Services `@Transactional`.
- Nunca mexer em `ddl-auto`. Migration Flyway `V16__multi_tenant.sql` (próxima livre). Coluna NOT NULL nova exige DEFAULT.
- Código sem comentários, exceto regra de negócio não óbvia.
- Sem tenant no contexto: `TenantContext.atual()` devolve o sentinela `0L` (leituras vazias); escrita falha em `TenantEntity.@PrePersist`. Nunca assumir a loja 1.
- Toda `nativeQuery` filtra por `loja_id` com `TenantContext.LOJA_ATUAL_SPEL`.
- Tabelas fora do tenant: `lojas`, `shedlock`, `configuracao_sistema`, `usuarios` (só coluna explícita), `password_reset_token`.
- Testes de unidade: `mvn test -pl pascoa-monolith -Dtest=<Classe>`. Suíte completa: `mvn test -pl pascoa-monolith -Dsurefire.excludes="**/*IT.java,**/*IntegrationTest.java,**/*IT.class,**/*IntegrationTest.class"`.
- Commits em português, tipo convencional, terminando com `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Branch: `feat/backlog-fase1-encomendas`.

## File Structure

| Arquivo | Responsabilidade |
|---|---|
| `<main>/common/tenant/TenantContext.java` | ThreadLocal do tenant, escopo com restauração, sentinela, constante SpEL |
| `<main>/common/tenant/TenantIdentifierResolver.java` | Entrega o tenant ao Hibernate |
| `<main>/common/tenant/TenantHibernateConfig.java` | Registra o resolver no Hibernate |
| `<main>/common/tenant/TenantTaskDecorator.java` | Copia o tenant para threads `@Async` |
| `<main>/common/tenant/TenantJobRunner.java` | Executa um job uma vez por loja, em transação própria |
| `<main>/common/entity/TenantEntity.java` | `@TenantId` + guarda de escrita; `BaseEntity` herda dela |
| `<main>/seguranca/entity/Loja.java`, `seguranca/repository/LojaRepository.java` | Entidade e repositório de lojas |
| `<main>/seguranca/service/UsuarioPrincipal.java` | `User` do Spring Security com `lojaId` |
| `<main>/config/TenantFilter.java` | Define o tenant por requisição |
| `pascoa-monolith/src/main/resources/db/migration/V16__multi_tenant.sql` | Schema |
| `<test>/common/tenant/*Test.java`, `<test>/config/TenantFilterTest.java`, `<test>/common/TenantTestExecutionListener.java` | Testes e infraestrutura de teste |

---

### Task 1: Núcleo do tenant (contexto, resolver, entidade base)

**Files:**
- Create: `<main>/common/tenant/TenantContext.java`
- Create: `<main>/common/tenant/TenantIdentifierResolver.java`
- Create: `<main>/common/tenant/TenantHibernateConfig.java`
- Create: `<main>/common/entity/TenantEntity.java`
- Create: `<test>/common/tenant/TenantContextTest.java`

**Interfaces:**
- Produces:
  - `TenantContext.SEM_TENANT` (`long` = 0), `TenantContext.LOJA_ATUAL_SPEL` (`String`), `static void set(Long)`, `static long atual()`, `static long exigir()`, `static void limpar()`, `static Escopo abrir(Long lojaId)`, `static void executar(Long lojaId, Runnable)`, `static <T> T calcular(Long lojaId, Supplier<T>)`, `interface Escopo extends AutoCloseable { void close(); }`
  - `TenantEntity` com `Long getLojaId()`.

- [ ] **Step 1: Escrever o teste do contexto**

`<test>/common/tenant/TenantContextTest.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    @Test
    void semContexto_atualDevolveSentinela() {
        assertThat(TenantContext.atual()).isEqualTo(TenantContext.SEM_TENANT);
    }

    @Test
    void semContexto_exigirLanca() {
        assertThatThrownBy(TenantContext::exigir).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void abrir_restauraTenantAnterior() {
        TenantContext.set(1L);
        try (var escopo = TenantContext.abrir(2L)) {
            assertThat(TenantContext.atual()).isEqualTo(2L);
        }
        assertThat(TenantContext.atual()).isEqualTo(1L);
    }

    @Test
    void executar_restauraMesmoComExcecao() {
        TenantContext.set(1L);
        assertThatThrownBy(() -> TenantContext.executar(2L, () -> { throw new IllegalArgumentException("x"); }))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(TenantContext.atual()).isEqualTo(1L);
    }

    @Test
    void calcular_devolveValorNoTenantIndicado() {
        long visto = TenantContext.calcular(3L, TenantContext::atual);
        assertThat(visto).isEqualTo(3L);
        assertThat(TenantContext.atual()).isEqualTo(TenantContext.SEM_TENANT);
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn test -pl pascoa-monolith -Dtest=TenantContextTest`
Expected: erro de compilação, `TenantContext` não existe.

- [ ] **Step 3: Implementar `TenantContext`**

`<main>/common/tenant/TenantContext.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import java.util.function.Supplier;

public final class TenantContext {

    public static final long SEM_TENANT = 0L;

    public static final String LOJA_ATUAL_SPEL =
        ":#{T(br.com.seuprojeto.pascoa.common.tenant.TenantContext).atual()}";

    private static final ThreadLocal<Long> ATUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public interface Escopo extends AutoCloseable {
        @Override
        void close();
    }

    public static void set(Long lojaId) {
        ATUAL.set(lojaId);
    }

    public static long atual() {
        Long id = ATUAL.get();
        return id == null ? SEM_TENANT : id;
    }

    public static long exigir() {
        Long id = ATUAL.get();
        if (id == null) {
            throw new IllegalStateException("Nenhuma loja no contexto");
        }
        return id;
    }

    public static void limpar() {
        ATUAL.remove();
    }

    public static Escopo abrir(Long lojaId) {
        Long anterior = ATUAL.get();
        ATUAL.set(lojaId);
        return () -> {
            if (anterior == null) {
                ATUAL.remove();
            } else {
                ATUAL.set(anterior);
            }
        };
    }

    public static void executar(Long lojaId, Runnable acao) {
        try (var escopo = abrir(lojaId)) {
            acao.run();
        }
    }

    public static <T> T calcular(Long lojaId, Supplier<T> acao) {
        try (var escopo = abrir(lojaId)) {
            return acao.get();
        }
    }
}
```

- [ ] **Step 4: Resolver, registro no Hibernate e `TenantEntity`**

`<main>/common/tenant/TenantIdentifierResolver.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.atual();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
```

`<main>/common/tenant/TenantHibernateConfig.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantHibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantResolverCustomizer(TenantIdentifierResolver resolver) {
        return props -> props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
```

`<main>/common/entity/TenantEntity.java`:

```java
package br.com.seuprojeto.pascoa.common.entity;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

@MappedSuperclass
@Getter
public abstract class TenantEntity {

    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @PrePersist
    void exigirLojaNoContexto() {
        if (TenantContext.atual() == TenantContext.SEM_TENANT) {
            throw new IllegalStateException("Escrita sem loja no contexto");
        }
    }
}
```

Em `<main>/common/entity/BaseEntity.java`, trocar a declaração da classe:

```java
public abstract class BaseEntity extends TenantEntity {
```

(`TenantEntity` está no mesmo pacote; não precisa de import.)

- [ ] **Step 5: Rodar o teste**

Run: `mvn test -pl pascoa-monolith -Dtest=TenantContextTest`
Expected: 5 testes passando.

- [ ] **Step 6: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(tenant): contexto, resolver do Hibernate e TenantEntity

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Schema, `Loja`, entidades no tenant e isolamento

**Files:**
- Create: `pascoa-monolith/src/main/resources/db/migration/V16__multi_tenant.sql`
- Create: `<main>/seguranca/entity/Loja.java`, `<main>/seguranca/repository/LojaRepository.java`
- Create: `<test>/common/TenantTestExecutionListener.java`
- Create: `pascoa-monolith/src/test/resources/META-INF/spring.factories`
- Create: `<test>/common/tenant/TenantIsolamentoTest.java`
- Modify: as 25 entidades que hoje não herdam `BaseEntity` (lista abaixo), `OrcamentoGasto.java:21-22`, `ConfiguracaoCanal.java:22`

**Interfaces:**
- Consumes: `TenantEntity`, `TenantContext` (Task 1).
- Produces: `Loja` com `Long getId()`, `String getNome()`, constante `Loja.PLATAFORMA_ID = 1L`; `LojaRepository extends JpaRepository<Loja, Long>`.

- [ ] **Step 1: `Loja` e `LojaRepository`**

`<main>/seguranca/entity/Loja.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lojas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loja {

    public static final long PLATAFORMA_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Builder.Default
    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();
}
```

`<main>/seguranca/repository/LojaRepository.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.repository;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LojaRepository extends JpaRepository<Loja, Long> {
}
```

- [ ] **Step 2: Migration `V16__multi_tenant.sql`**

```sql
CREATE TABLE IF NOT EXISTS lojas (
    id        BIGSERIAL PRIMARY KEY,
    nome      VARCHAR(150) NOT NULL,
    criada_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO lojas (id, nome) VALUES (1, 'Loja Padrão') ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('lojas', 'id'), GREATEST((SELECT MAX(id) FROM lojas), 1));

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'alertas_internos', 'audit_log', 'checklist_qualidade', 'clientes',
        'configuracao_canal', 'configuracao_financeira', 'contas_pagar', 'contas_receber',
        'despesas_fixas', 'despesas_variaveis', 'fichas_tecnicas', 'fichas_tecnicas_itens',
        'fornecedores', 'gastos_variaveis', 'inspecao_qualidade', 'itens_pedido',
        'materias_primas', 'movimentacoes_estoque', 'notas_cliente', 'notificacoes_enviadas',
        'orcamento_itens', 'orcamentos', 'orcamentos_gasto', 'ordens_producao',
        'pagamentos', 'pedidos', 'pontos_fidelidade', 'produtos', 'templates_notificacao',
        'usuarios'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN loja_id BIGINT NOT NULL DEFAULT 1 REFERENCES lojas(id)', t);
        EXECUTE format('CREATE INDEX idx_%s_loja_id ON %I (loja_id)', t, t);
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['configuracao_canal', 'orcamentos_gasto'] LOOP
        FOR r IN SELECT conname FROM pg_constraint WHERE conrelid = t::regclass AND contype = 'u' LOOP
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', t, r.conname);
        END LOOP;
    END LOOP;
END $$;

ALTER TABLE configuracao_canal ADD CONSTRAINT uq_configuracao_canal_loja_tipo UNIQUE (loja_id, tipo);
ALTER TABLE orcamentos_gasto ADD CONSTRAINT uq_orcamentos_gasto_loja_cat_mes_ano
    UNIQUE (loja_id, categoria, referencia_mes, referencia_ano);
```

- [ ] **Step 3: Infraestrutura de teste (tenant 1 por padrão)**

`<test>/common/TenantTestExecutionListener.java`:

```java
package br.com.seuprojeto.pascoa.common;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class TenantTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
        return 3000;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        TenantContext.set(Loja.PLATAFORMA_ID);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        TenantContext.limpar();
    }
}
```

A ordem 3000 vem antes do `TransactionalTestExecutionListener` (4000): a sessão do teste `@Transactional` captura o tenant ao abrir.

`pascoa-monolith/src/test/resources/META-INF/spring.factories`:

```properties
org.springframework.test.context.TestExecutionListener=br.com.seuprojeto.pascoa.common.TenantTestExecutionListener
```

- [ ] **Step 4: Entidades herdam `TenantEntity`**

Rodar na raiz do repositório (adiciona `extends TenantEntity` e o import nas 25 entidades que não herdam `BaseEntity`):

```bash
cd pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa
for f in auditoria/entity/AuditLog.java crm/entity/NotaCliente.java crm/entity/PontoFidelidade.java \
  estoque/entity/MovimentacaoEstoque.java fichaTecnica/entity/FichaTecnica.java fichaTecnica/entity/FichaTecnicaItem.java \
  financeiro/entity/ConfiguracaoFinanceira.java financeiro/entity/ContaPagar.java financeiro/entity/ContaReceber.java \
  financeiro/entity/DespesaFixa.java financeiro/entity/DespesaVariavel.java gastos/entity/GastoVariavel.java \
  gastos/entity/OrcamentoGasto.java notificacao/entity/AlertaInterno.java notificacao/entity/ConfiguracaoCanal.java \
  notificacao/entity/NotificacaoEnviada.java notificacao/entity/TemplateNotificacao.java orcamento/entity/Orcamento.java \
  orcamento/entity/OrcamentoItem.java pedido/entity/ItemPedido.java pedido/entity/Pagamento.java pedido/entity/Pedido.java \
  producao/entity/OrdemProducao.java qualidade/entity/ChecklistItem.java qualidade/entity/InspecaoQualidade.java; do
  perl -0pi -e 's/^public class (\w+) \{/public class $1 extends TenantEntity {/m; s/^(package [^\n]+;\n)/$1\nimport br.com.seuprojeto.pascoa.common.entity.TenantEntity;/m' "$f"
done
grep -L "extends TenantEntity" auditoria/entity/AuditLog.java crm/entity/*.java estoque/entity/*.java fichaTecnica/entity/*.java financeiro/entity/*.java gastos/entity/*.java notificacao/entity/*.java orcamento/entity/*.java pedido/entity/*.java producao/entity/*.java qualidade/entity/*.java
```

Expected da última linha: lista apenas arquivos que não são entidades (enums, `CampanhaItem`, `SegmentoCliente`, `TipoPonto`, etc.) — nenhuma entidade da lista acima. Cliente, Produto, Fornecedor e MateriaPrima já herdam via `BaseEntity`.

Em `<main>/gastos/entity/OrcamentoGasto.java:21-22`, substituir:

```java
       uniqueConstraints = @UniqueConstraint(columnNames = {"categoria", "referencia_mes", "referencia_ano"}))
```

por:

```java
       uniqueConstraints = @UniqueConstraint(columnNames = {"loja_id", "categoria", "referencia_mes", "referencia_ano"}))
```

Em `<main>/notificacao/entity/ConfiguracaoCanal.java`, substituir `@Column(name = "tipo", nullable = false, unique = true, length = 10)` por `@Column(name = "tipo", nullable = false, length = 10)` e acrescentar acima de `@Data` (ou da primeira anotação de classe) `@Table(name = "configuracao_canal", uniqueConstraints = @UniqueConstraint(columnNames = {"loja_id", "tipo"}))`, importando `jakarta.persistence.UniqueConstraint`. Se a classe já tiver `@Table`, só acrescentar o `uniqueConstraints`.

- [ ] **Step 5: Escrever o teste de isolamento**

`<test>/common/tenant/TenantIsolamentoTest.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Fornecedor;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.FornecedorRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolamentoTest {

    @Autowired private ClienteRepository clientes;
    @Autowired private FornecedorRepository fornecedores;
    @Autowired private ProdutoRepository produtos;

    private Cliente novoCliente(String nome) {
        return Cliente.builder().nome(nome).optIn(false).preferenciaCanal(PreferenciaCanal.NENHUM).build();
    }

    private List<String> nomesDeClientes(long loja) {
        return TenantContext.calcular(loja, () -> clientes.findAll().stream().map(Cliente::getNome).toList());
    }

    @Test
    void cliente_daLojaA_naoApareceNaLojaB() {
        String nome = "Cliente-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> clientes.save(novoCliente(nome)));

        assertThat(nomesDeClientes(1L)).contains(nome);
        assertThat(nomesDeClientes(2L)).doesNotContain(nome);
    }

    @Test
    void findById_deOutraLoja_voltaVazio() {
        Long id = TenantContext.calcular(1L, () -> clientes.save(novoCliente("Cliente-" + UUID.randomUUID())).getId());

        assertThat(TenantContext.calcular(1L, () -> clientes.findById(id))).isPresent();
        assertThat(TenantContext.calcular(2L, () -> clientes.findById(id))).isEmpty();
    }

    @Test
    void insert_gravaLojaDoContexto() {
        Cliente salvo = TenantContext.calcular(2L, () -> clientes.save(novoCliente("Cliente-" + UUID.randomUUID())));

        assertThat(salvo.getLojaId()).isEqualTo(2L);
    }

    @Test
    void fornecedorEProduto_tambemSaoIsolados() {
        String nomeFornecedor = "Fornecedor-" + UUID.randomUUID();
        String nomeProduto = "Produto-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> {
            fornecedores.save(Fornecedor.builder().nome(nomeFornecedor).build());
            produtos.save(Produto.builder().nome(nomeProduto).categoria(Categoria.TRUFADO)
                .precoVenda(BigDecimal.TEN).build());
        });

        assertThat(TenantContext.calcular(2L, () -> fornecedores.findAll()))
            .noneMatch(f -> f.getNome().equals(nomeFornecedor));
        assertThat(TenantContext.calcular(2L, () -> produtos.findAll()))
            .noneMatch(p -> p.getNome().equals(nomeProduto));
        assertThat(TenantContext.calcular(1L, () -> produtos.findAll()))
            .anyMatch(p -> p.getNome().equals(nomeProduto));
    }

    @Test
    void semTenant_leituraVazia_escritaFalha() {
        String nome = "Cliente-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> clientes.save(novoCliente(nome)));

        TenantContext.limpar();
        assertThat(clientes.findAll()).isEmpty();
        assertThatThrownBy(() -> clientes.save(novoCliente("Sem-loja-" + UUID.randomUUID())))
            .hasStackTraceContaining("Escrita sem loja no contexto");
    }
}
```

- [ ] **Step 6: Rodar o teste de isolamento**

Run: `mvn test -pl pascoa-monolith -Dtest=TenantIsolamentoTest`
Expected: 5 testes; `findById_deOutraLoja_voltaVazio` falha até o Step 6b (o Hibernate não aplica o filtro de `@TenantId` em `find`).

- [ ] **Step 6b: Base repository tenant-aware (o Hibernate 6.5 não aplica o filtro de `@TenantId` em `find`/`findById`)**

Confirmado na execução: `clientes.findById(id)` na loja 2 devolvia o cliente da loja 1 (`findAll` e JPQL já isolavam). Decisão do responsável: sobrescrever `findById` e `getReferenceById` numa base global, usando JPQL, que o filtro cobre.

`<main>/common/tenant/TenantAwareRepository.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.common.entity.TenantEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public class TenantAwareRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private final EntityManager em;
    private final JpaEntityInformation<T, ?> info;
    private final boolean porTenant;

    public TenantAwareRepository(JpaEntityInformation<T, ?> info, EntityManager em) {
        super(info, em);
        this.em = em;
        this.info = info;
        this.porTenant = TenantEntity.class.isAssignableFrom(info.getJavaType());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        if (!porTenant) {
            return super.findById(id);
        }
        String atributoId = info.getRequiredIdAttribute().getName();
        return em.createQuery("select e from " + info.getEntityName() + " e where e." + atributoId + " = :id",
                info.getJavaType())
            .setParameter("id", id)
            .getResultStream()
            .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public T getReferenceById(ID id) {
        if (!porTenant) {
            return super.getReferenceById(id);
        }
        return findById(id).orElseThrow(
            () -> new EntityNotFoundException(info.getEntityName() + " não encontrado: " + id));
    }
}
```

Em `<main>/config/AppConfig.java`, acrescentar à classe a anotação (com imports `org.springframework.data.jpa.repository.config.EnableJpaRepositories` e `br.com.seuprojeto.pascoa.common.tenant.TenantAwareRepository`):

```java
@EnableJpaRepositories(basePackages = "br.com.seuprojeto.pascoa", repositoryBaseClass = TenantAwareRepository.class)
```

(`basePackages` explícito porque, sem ele, o Spring escanearia só o pacote `config`.) Acrescentar ao `TenantIsolamentoTest`:

```java
    @Test
    void deleteById_deOutraLoja_naoApaga_eGetReferenceByIdFalha() {
        Long id = TenantContext.calcular(1L, () -> fornecedores.save(Fornecedor.builder()
            .nome("Fornecedor-" + UUID.randomUUID()).build()).getId());

        TenantContext.executar(2L, () -> fornecedores.deleteById(id));

        assertThat(TenantContext.calcular(1L, () -> fornecedores.findById(id))).isPresent();
        assertThatThrownBy(() -> TenantContext.executar(2L, () -> fornecedores.getReferenceById(id)))
            .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
```

Rodar `mvn test -pl pascoa-monolith -Dtest=TenantIsolamentoTest`: os 6 testes devem passar.

- [ ] **Step 7: Rodar a suíte completa**

Run: `mvn test -pl pascoa-monolith -Dsurefire.excludes="**/*IT.java,**/*IntegrationTest.java,**/*IT.class,**/*IntegrationTest.class"`
Expected: todos passando, exceto possíveis falhas de testes que criam `Usuario` (resolvidas na Task 3) e `CrmSegmentoTest` (Task 5). Anotar quais falham; se qualquer outro teste falhar, corrigir a causa antes do commit (provável: teste que roda fora da transação e depende do tenant, ou entidade sem `extends TenantEntity`).

- [ ] **Step 8: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(tenant): migration V16, entidade Loja e isolamento das 29 entidades

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Autenticação, `TenantFilter` e usuários por loja

**Files:**
- Modify: `<main>/seguranca/entity/Usuario.java`
- Create: `<main>/seguranca/service/UsuarioPrincipal.java`
- Modify: `<main>/seguranca/service/UsuarioService.java:33-47` (loadUserByUsername) e `:49-65` (listarTodos, buscarPorId)
- Modify: `<main>/seguranca/repository/UsuarioRepository.java`
- Create: `<main>/config/TenantFilter.java`
- Modify: `<main>/config/SecurityConfig.java` (assinatura de `securityFilterChain` e `.addFilterAfter`)
- Modify: `<main>/config/DataInitializer.java`
- Modify: `<main>/config/SistemaController.java`
- Create: `<test>/config/TenantFilterTest.java`, `<test>/seguranca/UsuarioTenantTest.java`

**Interfaces:**
- Consumes: `TenantContext`, `Loja.PLATAFORMA_ID`.
- Produces: `Usuario.getLojaId()/setLojaId(Long)`; `UsuarioPrincipal(Usuario)` com `Long getLojaId()`; `UsuarioRepository.findAllByLojaIdOrderByNomeAsc(Long)`; `TenantFilter(JdbcTemplate)`.

- [ ] **Step 1: Teste do `TenantFilter`**

`<test>/config/TenantFilterTest.java`:

```java
package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantFilterTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TenantFilter filter = new TenantFilter(jdbc);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        TenantContext.limpar();
    }

    private long tenantDuranteRequisicao(String caminho) throws Exception {
        var request = new MockHttpServletRequest("GET", caminho);
        request.setServletPath(caminho);
        AtomicLong visto = new AtomicLong(-1);
        filter.doFilter(request, new MockHttpServletResponse(), (rq, rs) -> visto.set(TenantContext.atual()));
        return visto.get();
    }

    @Test
    void usuarioLogado_usaLojaDoPrincipal() throws Exception {
        var usuario = Usuario.builder().nome("A").login("a").senha("x").role(Role.ADMIN).lojaId(9L).build();
        var principal = new UsuarioPrincipal(usuario);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(tenantDuranteRequisicao("/clientes")).isEqualTo(9L);
        assertThat(TenantContext.atual()).isEqualTo(TenantContext.SEM_TENANT);
    }

    @Test
    void tokenDeAcompanhamento_resolveLojaDoPedido() throws Exception {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("tok-1"))).thenReturn(List.of(7L));

        assertThat(tenantDuranteRequisicao("/acompanhamento/tok-1")).isEqualTo(7L);
    }

    @Test
    void tokenDeOrcamento_resolveLojaDoOrcamento_tambemNasAcoes() throws Exception {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("tok-2"))).thenReturn(List.of(8L));

        assertThat(tenantDuranteRequisicao("/orcamento-publico/tok-2/aprovar")).isEqualTo(8L);
    }

    @Test
    void tokenDesconhecido_ficaSemTenant() throws Exception {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("nada"))).thenReturn(List.of());

        assertThat(tenantDuranteRequisicao("/acompanhamento/nada")).isEqualTo(TenantContext.SEM_TENANT);
    }

    @Test
    void catalogo_usaLojaDaPlataforma() throws Exception {
        assertThat(tenantDuranteRequisicao("/catalogo")).isEqualTo(1L);
    }

    @Test
    void rotaSemTenant_naoDefineContexto() throws Exception {
        assertThat(tenantDuranteRequisicao("/login")).isEqualTo(TenantContext.SEM_TENANT);
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn test -pl pascoa-monolith -Dtest=TenantFilterTest`
Expected: erro de compilação (`TenantFilter`, `UsuarioPrincipal`, `Usuario.lojaId` ausentes).

- [ ] **Step 3: `Usuario.lojaId`, `UsuarioPrincipal`, repositório**

Em `<main>/seguranca/entity/Usuario.java`, acrescentar os imports `jakarta.persistence.PrePersist` e `br.com.seuprojeto.pascoa.common.tenant.TenantContext`, e dentro da classe, depois do campo `tentativasTotpFalhas`:

```java
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @PrePersist
    void definirLoja() {
        if (lojaId == null && TenantContext.atual() != TenantContext.SEM_TENANT) {
            lojaId = TenantContext.atual();
        }
    }
```

`<main>/seguranca/service/UsuarioPrincipal.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.service;

import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class UsuarioPrincipal extends User {

    private static final long serialVersionUID = 1L;

    private final Long lojaId;

    public UsuarioPrincipal(Usuario usuario) {
        super(usuario.getLogin(), usuario.getSenha(), true, true, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())));
        this.lojaId = usuario.getLojaId();
    }

    public Long getLojaId() {
        return lojaId;
    }
}
```

Em `<main>/seguranca/repository/UsuarioRepository.java`, acrescentar:

```java
    List<Usuario> findAllByLojaIdOrderByNomeAsc(Long lojaId);
```

(e remover `findAllByOrderByNomeAsc` se nenhum outro chamador o usar: `grep -rn findAllByOrderByNomeAsc` dentro de `seguranca/`).

- [ ] **Step 4: `UsuarioService`**

Em `loadUserByUsername`, substituir o bloco `return User.builder() ... .build();` por:

```java
        return new UsuarioPrincipal(usuario);
```

e remover os imports `User` e `SimpleGrantedAuthority` se ficarem sem uso. Substituir `listarTodos` e `buscarPorId`:

```java
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByLojaIdOrderByNomeAsc(TenantContext.exigir());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
            .filter(u -> u.getLojaId() == TenantContext.atual())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }
```

com o import `br.com.seuprojeto.pascoa.common.tenant.TenantContext`. O `u.getLojaId() == TenantContext.atual()` compara `Long` com `long` (unboxing), válido.

- [ ] **Step 5: `TenantFilter`**

`<main>/config/TenantFilter.java`:

```java
package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String SQL_PEDIDO = "SELECT loja_id FROM pedidos WHERE token_acompanhamento = ?";
    private static final String SQL_ORCAMENTO = "SELECT loja_id FROM orcamentos WHERE token_aprovacao = ?";

    private final JdbcTemplate jdbc;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Long lojaId = resolverLoja(request);
        if (lojaId == null) {
            chain.doFilter(request, response);
            return;
        }
        try (var escopo = TenantContext.abrir(lojaId)) {
            chain.doFilter(request, response);
        }
    }

    private Long resolverLoja(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal.getLojaId();
        }
        String[] partes = request.getServletPath().split("/");
        if (partes.length >= 3 && "acompanhamento".equals(partes[1])) {
            return primeiro(SQL_PEDIDO, partes[2]);
        }
        if (partes.length >= 3 && "orcamento-publico".equals(partes[1])) {
            return primeiro(SQL_ORCAMENTO, partes[2]);
        }
        if (partes.length >= 2 && "catalogo".equals(partes[1])) {
            return Loja.PLATAFORMA_ID;
        }
        return null;
    }

    private Long primeiro(String sql, String token) {
        List<Long> ids = jdbc.queryForList(sql, Long.class, token);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
```

- [ ] **Step 6: Ligar o filtro no `SecurityConfig`**

Em `<main>/config/SecurityConfig.java`: acrescentar os imports `org.springframework.jdbc.core.JdbcTemplate` e `org.springframework.security.web.authentication.AnonymousAuthenticationFilter`; na assinatura de `securityFilterChain`, acrescentar o parâmetro `JdbcTemplate jdbcTemplate` depois de `SecurityContextRepository securityContextRepository`; e logo depois da linha `.securityContext(sc -> sc.securityContextRepository(securityContextRepository))` acrescentar:

```java
            .addFilterAfter(new TenantFilter(jdbcTemplate), AnonymousAuthenticationFilter.class)
```

O filtro é criado com `new` (não é `@Component`) para o Spring Boot não registrá-lo também como filtro de servlet fora da cadeia de segurança.

- [ ] **Step 7: `DataInitializer` e `SistemaController`**

Em `<main>/config/DataInitializer.java`, no builder do admin, acrescentar `.lojaId(Loja.PLATAFORMA_ID)` (import `br.com.seuprojeto.pascoa.seguranca.entity.Loja`) antes de `.build()`.

Em `<main>/config/SistemaController.java`, acrescentar o método e chamá-lo na primeira linha de cada um dos três handlers (`pagina`, `salvar`, `toggleManutencao`):

```java
    private void exigirPlataforma() {
        if (TenantContext.atual() != Loja.PLATAFORMA_ID) {
            throw new AccessDeniedException("Apenas a administração da plataforma altera o sistema");
        }
    }
```

com os imports `org.springframework.security.access.AccessDeniedException`, `br.com.seuprojeto.pascoa.common.tenant.TenantContext` e `br.com.seuprojeto.pascoa.seguranca.entity.Loja`.

- [ ] **Step 8: Teste de usuários por loja**

`<test>/seguranca/UsuarioTenantTest.java`:

```java
package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioService;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class UsuarioTenantTest {

    @Autowired private UsuarioService service;
    @Autowired private UsuarioRepository repository;

    private Usuario salvar(String login, long loja) {
        return repository.save(Usuario.builder().nome(login).login(login).senha("x")
            .role(Role.ATENDENTE).lojaId(loja).build());
    }

    @Test
    void listarTodos_devolveSomenteUsuariosDaLojaAtual() {
        String daLoja1 = "u1-" + UUID.randomUUID();
        String daLoja2 = "u2-" + UUID.randomUUID();
        salvar(daLoja1, 1L);
        salvar(daLoja2, 2L);

        assertThat(service.listarTodos()).extracting(Usuario::getLogin)
            .contains(daLoja1).doesNotContain(daLoja2);
    }

    @Test
    void buscarPorId_deOutraLoja_naoEncontra() {
        Usuario outro = salvar("u2-" + UUID.randomUUID(), 2L);

        assertThatThrownBy(() -> service.buscarPorId(outro.getId()))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void usuarioNovoSemLoja_herdaLojaDoContexto() {
        Usuario novo = repository.save(Usuario.builder().nome("n").login("n-" + UUID.randomUUID())
            .senha("x").role(Role.ATENDENTE).build());

        assertThat(novo.getLojaId()).isEqualTo(1L);
    }

    @Test
    void loadUserByUsername_devolvePrincipalComLoja() {
        String login = "p-" + UUID.randomUUID();
        salvar(login, 2L);

        var principal = (UsuarioPrincipal) service.loadUserByUsername(login);

        assertThat(principal.getLojaId()).isEqualTo(2L);
    }
}
```

- [ ] **Step 9: Rodar os testes**

Run: `mvn test -pl pascoa-monolith -Dtest='TenantFilterTest,UsuarioTenantTest,PasswordResetServiceTest,RolePermissionsTest'`
Expected: todos passando. Se `PasswordResetServiceTest` ou `RolePermissionsTest` falharem por `loja_id` nulo ou por falta de tenant, o ajuste vai no teste (tenant 1 já vem do listener; usuário criado sem loja herda do contexto).

- [ ] **Step 10: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(tenant): TenantFilter, UsuarioPrincipal e usuarios por loja

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Queries nativas filtradas por loja

**Files:**
- Modify: `<main>/pedido/repository/PedidoRepository.java:72-93`
- Modify: `<main>/pedido/repository/ItemPedidoRepository.java:25-32`
- Modify: `<main>/crm/repository/PontoFidelidadeRepository.java:20-35`
- Modify: `<main>/notificacao/repository/NotificacaoEnviadaRepository.java:40-46`
- Modify: `<main>/cadastro/repository/ClienteRepository.java:40-46`
- Create: `<test>/common/tenant/QueriesNativasIsolamentoTest.java`

**Interfaces:**
- Consumes: `TenantContext.LOJA_ATUAL_SPEL`.
- Produces: nenhuma assinatura muda.

- [ ] **Step 1: Escrever o teste**

`<test>/common/tenant/QueriesNativasIsolamentoTest.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.crm.entity.PontoFidelidade;
import br.com.seuprojeto.pascoa.crm.entity.TipoPonto;
import br.com.seuprojeto.pascoa.crm.repository.PontoFidelidadeRepository;
import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.NotificacaoEnviada;
import br.com.seuprojeto.pascoa.notificacao.entity.StatusEnvio;
import br.com.seuprojeto.pascoa.notificacao.repository.NotificacaoEnviadaRepository;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
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
class QueriesNativasIsolamentoTest {

    @Autowired private ClienteRepository clientes;
    @Autowired private ProdutoRepository produtos;
    @Autowired private PedidoRepository pedidos;
    @Autowired private ItemPedidoRepository itens;
    @Autowired private PontoFidelidadeRepository pontos;
    @Autowired private NotificacaoEnviadaRepository notificacoes;

    private Cliente novoCliente(LocalDate nascimento) {
        return Cliente.builder().nome("C-" + UUID.randomUUID()).optIn(true)
            .preferenciaCanal(PreferenciaCanal.EMAIL).dataNascimento(nascimento).build();
    }

    @Test
    void saldoPorCliente_somenteDaLojaAtual() {
        Long clienteId = TenantContext.calcular(1L, () -> {
            Cliente c = clientes.save(novoCliente(null));
            pontos.save(PontoFidelidade.builder().cliente(c).pontos(100).tipo(TipoPonto.CREDITO).build());
            return c.getId();
        });

        assertThat(TenantContext.calcular(1L, () -> pontos.saldoPorCliente(clienteId))).isEqualTo(100);
        assertThat(TenantContext.calcular(2L, () -> pontos.saldoPorCliente(clienteId))).isZero();
    }

    @Test
    void findAniversariantesHoje_somenteDaLojaAtual() {
        LocalDate hoje = LocalDate.now();
        Long id = TenantContext.calcular(1L, () -> clientes.save(novoCliente(hoje.minusYears(30))).getId());

        assertThat(TenantContext.calcular(1L, () ->
            clientes.findAniversariantesHoje(hoje.getMonthValue(), hoje.getDayOfMonth())))
            .anyMatch(c -> c.getId().equals(id));
        assertThat(TenantContext.calcular(2L, () ->
            clientes.findAniversariantesHoje(hoje.getMonthValue(), hoje.getDayOfMonth())))
            .noneMatch(c -> c.getId().equals(id));
    }

    @Test
    void jaEnviouAniversarioNoAno_somenteDaLojaAtual() {
        int ano = LocalDate.now().getYear();
        Long clienteId = TenantContext.calcular(1L, () -> {
            Cliente c = clientes.save(novoCliente(null));
            notificacoes.save(NotificacaoEnviada.builder().cliente(c)
                .evento(EventoNotificacao.ANIVERSARIO_CLIENTE).canal(CanalNotificacao.EMAIL)
                .destinatario("a@a.com").status(StatusEnvio.ENVIADA).build());
            return c.getId();
        });

        assertThat(TenantContext.calcular(1L, () ->
            notificacoes.jaEnviouAniversarioNoAno(clienteId, "ANIVERSARIO_CLIENTE", "EMAIL", ano))).isTrue();
        assertThat(TenantContext.calcular(2L, () ->
            notificacoes.jaEnviouAniversarioNoAno(clienteId, "ANIVERSARIO_CLIENTE", "EMAIL", ano))).isFalse();
    }

    @Test
    void consultasDeAnalytics_somenteDaLojaAtual() {
        int ano = LocalDate.now().getYear();
        TenantContext.executar(1L, () -> {
            Cliente c = clientes.save(novoCliente(null));
            Produto p = produtos.save(Produto.builder().nome("P-" + UUID.randomUUID())
                .categoria(Categoria.TRUFADO).precoVenda(BigDecimal.TEN).build());
            Pedido pedido = pedidos.save(Pedido.builder().cliente(c).status(StatusPedido.ENTREGUE)
                .totalPedido(new BigDecimal("100.00")).build());
            itens.save(ItemPedido.builder().pedido(pedido).produto(p).quantidade(2)
                .precoUnitario(new BigDecimal("50.00")).build());
        });

        assertThat(TenantContext.calcular(1L, () -> pedidos.countPorAno(ano))).isPositive();
        assertThat(TenantContext.calcular(1L, () -> pedidos.totalPorAno(ano))).isPositive();
        assertThat(TenantContext.calcular(1L, () -> pedidos.faturamentoPorMes(ano))).isNotEmpty();
        assertThat(TenantContext.calcular(1L, () -> pedidos.anosComPedidos())).contains(ano);
        assertThat(TenantContext.calcular(1L, () -> itens.rankingProdutosPorAno(ano))).isNotEmpty();

        assertThat(TenantContext.calcular(2L, () -> pedidos.countPorAno(ano))).isZero();
        assertThat(TenantContext.calcular(2L, () -> pedidos.totalPorAno(ano))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(TenantContext.calcular(2L, () -> pedidos.faturamentoPorMes(ano))).isEmpty();
        assertThat(TenantContext.calcular(2L, () -> pedidos.anosComPedidos())).isEmpty();
        assertThat(TenantContext.calcular(2L, () -> itens.rankingProdutosPorAno(ano))).isEmpty();
    }
}
```

Se algum `builder()` acima não aceitar um campo (nome diferente na entidade), ajustar ao nome real; as entidades estão em `<main>/*/entity/`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn test -pl pascoa-monolith -Dtest=QueriesNativasIsolamentoTest`
Expected: falha nas asserções de loja 2 (as queries nativas ainda devolvem dados de todas as lojas).

- [ ] **Step 3: Filtrar as queries**

Em cada arquivo, acrescentar a cláusula de tenant. Em todos, `import br.com.seuprojeto.pascoa.common.tenant.TenantContext;`.

`PedidoRepository` (`faturamentoPorMes`, `totalPorAno`, `countPorAno`, `anosComPedidos`): em cada `@Query` nativa acrescentar `AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL`. Resultado final:

```java
    @Query(value = "SELECT EXTRACT(MONTH FROM data_pedido)::int, COALESCE(SUM(total_pedido),0), COUNT(*) " +
                   "FROM pedidos WHERE EXTRACT(YEAR FROM data_pedido) = :ano AND status != 'CANCELADO' " +
                   "AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
                   "GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> faturamentoPorMes(@Param("ano") int ano);

    @Query(value = "SELECT COALESCE(SUM(total_pedido), 0) FROM pedidos " +
                   "WHERE EXTRACT(YEAR FROM data_pedido) = :ano AND status != 'CANCELADO' " +
                   "AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL,
           nativeQuery = true)
    BigDecimal totalPorAno(@Param("ano") int ano);

    @Query(value = "SELECT COUNT(*) FROM pedidos " +
                   "WHERE EXTRACT(YEAR FROM data_pedido) = :ano AND status != 'CANCELADO' " +
                   "AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL,
           nativeQuery = true)
    long countPorAno(@Param("ano") int ano);

    @Query(value = "SELECT DISTINCT EXTRACT(YEAR FROM data_pedido)::int FROM pedidos " +
                   "WHERE status != 'CANCELADO' AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL +
                   " ORDER BY 1 DESC", nativeQuery = true)
    List<Integer> anosComPedidos();
```

`ItemPedidoRepository.rankingProdutosPorAno`: depois de `"WHERE EXTRACT(YEAR FROM ped.data_pedido) = :ano AND ped.status != 'CANCELADO' " +` inserir:

```java
                   "AND i.loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
                   "AND ped.loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
                   "AND pr.loja_id = " + TenantContext.LOJA_ATUAL_SPEL + " " +
```

`PontoFidelidadeRepository.saldoPorCliente`: o text block termina em `WHERE cliente_id = :clienteId` seguido de `""", nativeQuery = true)`. Trocar o final do text block para:

```java
            WHERE cliente_id = :clienteId
              AND loja_id =
            """ + " " + TenantContext.LOJA_ATUAL_SPEL, nativeQuery = true)
```

`NotificacaoEnviadaRepository.jaEnviouAniversarioNoAno`: trocar `"AND EXTRACT(YEAR FROM data_envio) = :ano",` por:

```java
                   "AND EXTRACT(YEAR FROM data_envio) = :ano " +
                   "AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL,
```

`ClienteRepository.findAniversariantesHoje`: trocar `"AND excluido_em IS NULL",` por:

```java
                   "AND excluido_em IS NULL " +
                   "AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL,
```

- [ ] **Step 4: Rodar o teste e a suíte do CRM**

Run: `mvn test -pl pascoa-monolith -Dtest='QueriesNativasIsolamentoTest,CrmSegmentoTest'`
Expected: todos passando (`CrmSegmentoTest` ainda chama `recalcularSegmentos()`; a Task 5 muda isso, mas o método atual segue funcionando até lá).

- [ ] **Step 5: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(tenant): queries nativas filtram por loja

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Jobs, `@Async` e fila de campanha

**Files:**
- Create: `<main>/common/tenant/TenantJobRunner.java`
- Create: `<main>/common/tenant/TenantTaskDecorator.java`
- Modify: `<main>/crm/service/CrmService.java:149-152`
- Modify: `<main>/notificacao/service/NotificacaoAgendadaService.java:51-54` e `:82-85`
- Modify: `<main>/crm/entity/CampanhaItem.java`, `<main>/crm/service/CampanhaService.java`
- Modify: `<test>/crm/service/CrmSegmentoTest.java:154` e `:171`
- Create: `<test>/common/tenant/TenantJobRunnerTest.java`

**Interfaces:**
- Consumes: `LojaRepository`, `TenantContext`.
- Produces: `TenantJobRunner.porLoja(Runnable)`; `CrmService.recalcularSegmentosDaLojaAtual()`; `NotificacaoAgendadaService.notificarAniversariantesDaLojaAtual()` e `notificarOrcamentosExpirandoDaLojaAtual()`; `CampanhaItem(Long lojaId, Long clienteId, ...)`.

- [ ] **Step 1: Teste do runner e do decorator**

`<test>/common/tenant/TenantJobRunnerTest.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TenantJobRunnerTest {

    @Autowired private TenantJobRunner runner;
    @Autowired private LojaRepository lojas;
    @Autowired private AsyncTaskExecutor applicationTaskExecutor;

    @Test
    void porLoja_executaUmaVezPorLojaNoTenantCerto_eUmaFalhaNaoParaAsOutras() {
        Long a = lojas.save(Loja.builder().nome("A").build()).getId();
        Long b = lojas.save(Loja.builder().nome("B").build()).getId();
        List<Long> visitadas = new ArrayList<>();

        runner.porLoja(() -> {
            visitadas.add(TenantContext.atual());
            if (TenantContext.atual() == a) {
                throw new IllegalStateException("falha na primeira");
            }
        });

        assertThat(visitadas).contains(a, b);
        assertThat(TenantContext.atual()).isEqualTo(1L);
    }

    @Test
    void executorAsync_propagaOTenantDaThreadQueSubmete() throws Exception {
        long visto = TenantContext.calcular(5L, () -> {
            try {
                return applicationTaskExecutor.submit(TenantContext::atual).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(visto).isEqualTo(5L);
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn test -pl pascoa-monolith -Dtest=TenantJobRunnerTest`
Expected: erro de compilação (`TenantJobRunner` ausente).

- [ ] **Step 3: Implementar `TenantJobRunner` e `TenantTaskDecorator`**

`<main>/common/tenant/TenantJobRunner.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantJobRunner {

    private final LojaRepository lojaRepository;
    private final TransactionTemplate transactionTemplate;

    public void porLoja(Runnable job) {
        for (Loja loja : lojaRepository.findAll()) {
            try (var escopo = TenantContext.abrir(loja.getId())) {
                transactionTemplate.executeWithoutResult(status -> job.run());
            } catch (RuntimeException e) {
                log.error("[TENANT] Job falhou para a loja {}: {}", loja.getId(), e.getMessage(), e);
            }
        }
    }
}
```

`<main>/common/tenant/TenantTaskDecorator.java`:

```java
package br.com.seuprojeto.pascoa.common.tenant;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

@Component
public class TenantTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable tarefa) {
        long lojaId = TenantContext.atual();
        if (lojaId == TenantContext.SEM_TENANT) {
            return tarefa;
        }
        return () -> TenantContext.executar(lojaId, tarefa);
    }
}
```

O Spring Boot 3.3 aplica um único bean `TaskDecorator` ao `applicationTaskExecutor` (executor padrão do `@Async`).

- [ ] **Step 4: Rodar o teste**

Run: `mvn test -pl pascoa-monolith -Dtest=TenantJobRunnerTest`
Expected: 2 testes passando.

- [ ] **Step 5: `CrmService.recalcularSegmentos`**

Em `<main>/crm/service/CrmService.java`, adicionar o campo `private final TenantJobRunner tenantJobRunner;` junto dos outros `private final` (a classe usa `@RequiredArgsConstructor`) com o import `br.com.seuprojeto.pascoa.common.tenant.TenantJobRunner`, e substituir:

```java
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void recalcularSegmentos() {
        log.info("[CRM] Iniciando recalculo de segmentos de clientes...");
```

por:

```java
    @Scheduled(cron = "0 0 2 * * *")
    public void recalcularSegmentos() {
        tenantJobRunner.porLoja(this::recalcularSegmentosDaLojaAtual);
    }

    @Transactional
    public void recalcularSegmentosDaLojaAtual() {
        log.info("[CRM] Iniciando recalculo de segmentos de clientes...");
```

Em `<test>/crm/service/CrmSegmentoTest.java:154` e `:171`, trocar `crmService.recalcularSegmentos();` por `crmService.recalcularSegmentosDaLojaAtual();`.

- [ ] **Step 6: `NotificacaoAgendadaService`**

Adicionar o campo `private final TenantJobRunner tenantJobRunner;` (com import) junto dos `private final` existentes (linhas 36-38). Substituir os dois métodos agendados:

```java
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void notificarAniversariantes() {
        LocalDate hoje = LocalDate.now();
```

por:

```java
    @Scheduled(cron = "0 0 8 * * *")
    public void notificarAniversariantes() {
        tenantJobRunner.porLoja(this::notificarAniversariantesDaLojaAtual);
    }

    @Transactional
    public void notificarAniversariantesDaLojaAtual() {
        LocalDate hoje = LocalDate.now();
```

e:

```java
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void notificarOrcamentosExpirando() {
        LocalDate dataAlerta = LocalDate.now().plusDays(DIAS_AVISO_ORCAMENTO);
```

por:

```java
    @Scheduled(cron = "0 0 9 * * *")
    public void notificarOrcamentosExpirando() {
        tenantJobRunner.porLoja(this::notificarOrcamentosExpirandoDaLojaAtual);
    }

    @Transactional
    public void notificarOrcamentosExpirandoDaLojaAtual() {
        LocalDate dataAlerta = LocalDate.now().plusDays(DIAS_AVISO_ORCAMENTO);
```

- [ ] **Step 7: Fila de campanha**

`<main>/crm/entity/CampanhaItem.java`: acrescentar `Long lojaId,` como primeiro componente do record:

```java
public record CampanhaItem(
        Long lojaId,
        Long clienteId,
        String nomeCliente,
        String destinatario,
        CanalNotificacao canal,
        String assunto,
        String mensagem
) {}
```

Em `<main>/crm/service/CampanhaService.java`, no `new CampanhaItem(` de `disparar`, acrescentar `TenantContext.exigir(),` como primeiro argumento (import `br.com.seuprojeto.pascoa.common.tenant.TenantContext`). Em `processarProximo`, substituir:

```java
        CampanhaItem item = campanhaQueue.poll();
        if (item == null) { return; }

        Optional<ConfiguracaoCanal> optConfig = canalRepository.findByTipo(item.canal());
```

por:

```java
        CampanhaItem item = campanhaQueue.poll();
        if (item == null) { return; }
        TenantContext.executar(item.lojaId(), () -> enviar(item));
    }

    private void enviar(CampanhaItem item) {
        Optional<ConfiguracaoCanal> optConfig = canalRepository.findByTipo(item.canal());
```

O restante do corpo antigo de `processarProximo` (do `if (optConfig.isEmpty()...` até o fim) passa a ser o corpo de `enviar`, sem outras mudanças; as chaves de fechamento já existentes continuam fechando `enviar`.

- [ ] **Step 8: Compilar e rodar as suítes afetadas**

Run: `mvn test -pl pascoa-monolith -Dtest='TenantJobRunnerTest,CrmSegmentoTest,NotificacaoEventListenerTest,NotificacaoIdempotenciaTest,AlertaInternoIntegrationTest'`
Expected: todos passando. Se algum teste de notificação falhar por falta de tenant numa thread `@Async`, confirmar que `TenantTaskDecorator` é o único `TaskDecorator` no contexto (`grep -rn "TaskDecorator" pascoa-monolith/src`).

- [ ] **Step 9: Commit**

```bash
git add pascoa-monolith/src
git commit -m "feat(tenant): jobs por loja, tenant no @Async e na fila de campanha

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Validação em PostgreSQL, documentação e push

**Files:**
- Modify: `docs/05-estado-implementacao.md`, `docs/06-schema-banco.md`, `CLAUDE.md`

- [ ] **Step 1: Suíte completa**

Run: `mvn test -pl pascoa-monolith -Dsurefire.excludes="**/*IT.java,**/*IntegrationTest.java,**/*IT.class,**/*IntegrationTest.class"`
Expected: `BUILD SUCCESS`, 0 falhas. Corrigir qualquer falha antes de seguir.

- [ ] **Step 2: Migration real em Postgres com a massa de teste**

```bash
docker compose up -d postgres
cd pascoa-monolith && mvn spring-boot:run -Dspring-boot.run.arguments="--security.2fa.enabled=false"
```

Expected: o log mostra `Migrating schema "public" to version "16 - multi tenant"` e a aplicação sobe sem erro de `ddl-auto=validate` (isso confirma que o schema bate com as 29 entidades). Em outro terminal:

```bash
docker compose exec postgres psql -U postgres -d pascoa_monolith -c "SELECT count(*) FROM information_schema.columns WHERE column_name='loja_id' AND table_schema='public';"
docker compose exec postgres psql -U postgres -d pascoa_monolith -c "SELECT count(*) FROM pedidos WHERE loja_id <> 1;"
```

Expected: `30` e `0`.

- [ ] **Step 3: Verificação manual com duas lojas**

```bash
docker compose exec postgres psql -U postgres -d pascoa_monolith -c "INSERT INTO lojas (nome) VALUES ('Loja B');"
docker compose exec postgres psql -U postgres -d pascoa_monolith -c "INSERT INTO usuarios (nome, login, senha, role, ativo, loja_id, totp_ativado, tentativas_totp_falhas) SELECT 'Admin B', 'adminb', senha, 'ADMIN', true, 2, false, 0 FROM usuarios WHERE login = 'admin';"
```

Em `http://localhost:8080` (usar a skill `agent-browser` se preferir automatizar): entrar como `adminb` (mesma senha do `admin`) e abrir `/clientes`, `/pedidos`, `/produtos`, `/dashboard`: todas devem estar vazias e o dashboard zerado. Entrar como `admin`: os dados da massa continuam todos visíveis. Como `adminb`, abrir `/admin/sistema`: deve responder 403. Como `adminb`, criar um cliente e conferir que o `admin` não o vê.

Parar a aplicação ao terminar.

- [ ] **Step 4: Atualizar documentação**

Em `docs/05-estado-implementacao.md`, acrescentar a seção `## 27. F0.1 Multi-tenant (2026-10-05)` com: mecanismo (`@TenantId` + `TenantContext` + `TenantFilter`), migration `V16__multi_tenant.sql` (próxima livre: V17), as 29 tabelas com `loja_id` + `usuarios`, o que ficou global (`lojas`, `shedlock`, `configuracao_sistema`), o catálogo público preso à loja 1 até o F2.3, e a restrição de `/admin/sistema` à loja 1. Trocar a linha de "Estoque ⚠️ Template de saída ausente" do resumo executivo por `✅ Completo` e remover a seção "Gap: Template `estoque/saida.html` ausente" (o arquivo existe em `templates/estoque/saida.html`).

Em `docs/06-schema-banco.md`, acrescentar `lojas` e a coluna `loja_id` (descrição curta e a regra do `DEFAULT 1`).

Em `CLAUDE.md`, acrescentar à seção Convenções:

```markdown
- Multi-tenant: entidade nova herda `TenantEntity` (ou `BaseEntity`) e a tabela ganha `loja_id BIGINT NOT NULL DEFAULT 1 REFERENCES lojas(id)`. `nativeQuery` exige `AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL`. Job `@Scheduled` usa `TenantJobRunner.porLoja`. Thread própria: `TenantContext.executar(lojaId, ...)`.
```

e trocar `(próxima: V15)` por `(próxima: V17)`.

- [ ] **Step 5: Commit e push**

```bash
git add docs CLAUDE.md
git commit -m "docs: F0.1 multi-tenant no estado de implementação e convenções

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
git push
```

---

## Self-Review

**Cobertura da spec:** `@TenantId`/contexto/resolver/sentinela (Task 1); migration, `Loja`, 29 entidades, UNIQUE compostos, isolamento por entidade (Task 2); `UsuarioPrincipal`, `TenantFilter` com usuário/token/catálogo, `DataInitializer`, restrição de `/admin/sistema` (Task 3); 8 queries nativas (Task 4); jobs, `@Async`, fila de campanha (Task 5); validação da migration em Postgres e docs (Task 6). Fora do escopo declarado: F0.2, F0.4, F0.6.

**Tipos consistentes:** `TenantContext.atual()` devolve `long`, `exigir()` devolve `long`; `abrir/executar/calcular` recebem `Long`. `Usuario.getLojaId()` é `Long`, comparado a `long` por unboxing em `UsuarioService.buscarPorId`. `Loja.PLATAFORMA_ID` é `long`, usado em `Usuario.builder().lojaId(...)` e em `DataInitializer` por autoboxing.

**Pontos que dependem de verificação na execução:** (1) `@TenantId` filtrar `findById` (Task 2, Step 6 tem a instrução de parar); (2) `TaskDecorator` aplicado ao `applicationTaskExecutor` (Task 5, Step 4 verifica); (3) nomes de campos nos `builder()` dos testes (Task 4, Step 1 avisa).
