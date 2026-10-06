# F0.2 Cadastro self-service e onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Uma loja nova se cadastra sozinha em `/cadastro`, entra logada e é levada por 3 passos puláveis (segmento, produtos de exemplo, primeiro pedido).

**Architecture:** O cadastro cria `Loja` e `Usuario` ADMIN numa transação e, numa segunda transação já no tenant da loja nova (a sessão do Hibernate fixa o tenant ao abrir), cria canais desligados e templates padrão lidos de JSON versionado, com compensação se a segunda falhar. O e-mail vira o login. O 2FA passa a valer só para a loja da plataforma. O onboarding guarda o estado em `lojas` (`segmento`, `onboarding_concluido`) e cria categorias e produtos no tenant da loja.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Spring Security 6, Hibernate 6.5 (`@TenantId`), Thymeleaf, Flyway, PostgreSQL 16 (H2 nos testes), Jackson, JUnit 5 + AssertJ + MockMvc + Mockito.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-10-06-f02-cadastro-onboarding-design.md`. Pré-requisitos já prontos: F0.1 (multi-tenant) e F0.3 (categorias, unidades de venda, quantidade decimal).
- Pacote base `br.com.seuprojeto.pascoa`; módulo `pascoa-monolith`. Caminhos: `<main>` = `pascoa-monolith/src/main/java/br/com/seuprojeto/pascoa`, `<res>` = `pascoa-monolith/src/main/resources`, `<test>` = `pascoa-monolith/src/test/java/br/com/seuprojeto/pascoa`.
- `@RequiredArgsConstructor`, nunca `@Autowired` em código de produção. Services `@Transactional` onde a regra não exigir controle explícito de transação. Nunca mexer em `ddl-auto`. Migration `V20__lojas_onboarding.sql` (próxima livre).
- Multi-tenant: a sessão do Hibernate fixa o tenant ao abrir. Para gravar em tenant diferente do da requisição, definir `TenantContext.executar(lojaId, ...)` **antes** de abrir a transação (`TenantContext.executar(id, () -> template.executeWithoutResult(...))`), nunca dentro dela. `lojas` e `usuarios` não são tenant; `categorias_produto`, `produtos`, `configuracao_canal`, `templates_notificacao` são.
- Spring Security 6: autenticar o dono recém-cadastrado com `SecurityContextRepository.saveContext(context, request, response)` (nunca `session.setAttribute`). Rota nova pública entra em `SecurityConfig.java`.
- Código sem comentários, exceto regra de negócio não óbvia. Thymeleaf: nunca JS inline (CSP); ícones decorativos com `aria-hidden="true"`; `<label for>` literal; textos de erro com `role="alert"`.
- Testes: `mvn test -pl pascoa-monolith -Dtest=<Classe>`; suíte completa: `mvn test -pl pascoa-monolith -Dsurefire.excludes="**/*IT.java,**/*IntegrationTest.java,**/*IT.class,**/*IntegrationTest.class"`; integração: `mvn test -pl pascoa-monolith -Dtest='*IntegrationTest'`. O `TenantTestExecutionListener` define o tenant 1 antes de cada método; testes que usam outras lojas não são `@Transactional` e usam `TenantContext.executar/calcular` com ids de loja próprios do teste (ex.: criar `Loja` e usar o id gerado, ou ids altos como `701L`).
- Validação em PostgreSQL usa banco descartável (`pascoa_f02*`), nunca `pascoa_monolith`; o dev pode ter uma instância na porta 8080, então a aplicação de teste usa 8086/8087.
- Commits em português, tipo convencional, terminando com `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Branch: `feat/backlog-fase1-encomendas`. `git add` sempre com caminhos explícitos.

## File Structure

| Arquivo | Responsabilidade |
|---|---|
| `<res>/db/migration/V20__lojas_onboarding.sql` | `lojas.segmento`, `lojas.onboarding_concluido` |
| `<main>/seguranca/entity/Segmento.java`, `Loja.java` | Segmento da loja e estado do onboarding |
| `<main>/seguranca/service/LojaPadroesService.java` | Canais desligados e templates padrão (idempotente) |
| `<main>/seguranca/service/CadastroLojaService.java` | Cadastro em duas transações, com compensação |
| `<main>/seguranca/controller/CadastroController.java`, `seguranca/dto/CadastroForm.java` | Tela e POST de `/cadastro`, auto-login |
| `<main>/onboarding/service/OnboardingService.java`, `onboarding/controller/OnboardingController.java`, `onboarding/dto/ProdutosExemploForm.java` | Os 3 passos |
| `<res>/defaults/notificacoes.json`, `<res>/defaults/produtos-exemplo.json` | Conteúdo padrão versionado |
| `<res>/templates/cadastro/form.html`, `onboarding/{segmento,produtos,pedido}.html` | Telas |

---

### Task 1: Acesso por e-mail, 2FA só na plataforma, recuperação de senha e colunas da loja

**Files:**
- Create: `<res>/db/migration/V20__lojas_onboarding.sql`, `<main>/seguranca/entity/Segmento.java`
- Modify: `<main>/seguranca/entity/Loja.java`, `<main>/seguranca/repository/UsuarioRepository.java`, `<main>/seguranca/service/UsuarioService.java:33-47`, `<main>/seguranca/service/TwoFactorAuthenticationSuccessHandler.java`, `<main>/seguranca/service/PasswordResetService.java:55-65`, `<res>/templates/login.html:26`
- Create tests: `<test>/seguranca/AcessoPorEmailTest.java`, `<test>/seguranca/PasswordResetEmailDuplicadoTest.java`

**Interfaces:**
- Produces: `Segmento` (`DOCES`, `SALGADOS`, `AMBOS`); `Loja.getSegmento()/setSegmento(Segmento)` e `Loja.isOnboardingConcluido()/setOnboardingConcluido(boolean)` (builder: `.segmento(...)`, `.onboardingConcluido(...)`; padrão `true`); `UsuarioRepository.existsByLoginIgnoreCase(String)` e `findAllByEmailIgnoreCase(String)` (substitui `findByEmail`).

- [ ] **Step 1: Escrever os testes**

`<test>/seguranca/AcessoPorEmailTest.java`:

```java
package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.TwoFactorAuthenticationSuccessHandler;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AcessoPorEmailTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TwoFactorAuthenticationSuccessHandler handler;

    private Usuario salvar(String login, long loja) {
        return usuarios.save(Usuario.builder().nome("Dono").login(login).email(login).senha("x")
            .role(Role.ADMIN).ativo(true).lojaId(loja).build());
    }

    private String redirecionamento(Usuario usuario) throws Exception {
        ReflectionTestUtils.setField(handler, "twoFactorEnabled", true);
        var principal = new UsuarioPrincipal(usuario);
        var autenticacao = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        var resposta = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), resposta, autenticacao);
        return resposta.getRedirectedUrl();
    }

    @Test
    void loginComEmailEmMaiusculas_encontraOUsuarioGravadoEmMinusculas() {
        String email = "dona-" + UUID.randomUUID() + "@teste.com";
        salvar(email, 701L);

        var principal = (UsuarioPrincipal) usuarioService.loadUserByUsername(email.toUpperCase());

        assertThat(principal.getUsername()).isEqualTo(email);
        assertThat(principal.getLojaId()).isEqualTo(701L);
    }

    @Test
    void loginInexistente_continuaFalhando() {
        assertThatThrownBy(() -> usuarioService.loadUserByUsername("nao-existe-" + UUID.randomUUID()))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void adminDeLojaNova_naoPassaPeloDoisFatores() throws Exception {
        Usuario dono = salvar("dono-" + UUID.randomUUID() + "@teste.com", 702L);

        assertThat(redirecionamento(dono)).isEqualTo("/dashboard");
    }

    @Test
    void adminDaPlataforma_continuaPassandoPeloDoisFatores() throws Exception {
        Usuario admin = salvar("admin-" + UUID.randomUUID(), 1L);

        assertThat(redirecionamento(admin)).isEqualTo("/2fa/setup");
    }

    @Test
    void existsByLoginIgnoreCase_ignoraCaixa() {
        String email = "cx-" + UUID.randomUUID() + "@teste.com";
        salvar(email, 703L);

        assertThat(usuarios.existsByLoginIgnoreCase(email.toUpperCase())).isTrue();
        assertThat(usuarios.existsByLoginIgnoreCase("outro-" + UUID.randomUUID())).isFalse();
    }
}
```

`<test>/seguranca/PasswordResetEmailDuplicadoTest.java` (siga o mesmo `@MockBean JavaMailSender`/setup que `PasswordResetServiceTest` já usa — leia esse arquivo antes e copie a configuração):

```java
package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
class PasswordResetEmailDuplicadoTest {

    @MockBean private JavaMailSender mailSender;
    @Autowired private PasswordResetService service;
    @Autowired private UsuarioRepository usuarios;

    private void salvar(String login, String email, long loja) {
        usuarios.save(Usuario.builder().nome("U").login(login).email(email).senha("x")
            .role(Role.ATENDENTE).ativo(true).lojaId(loja).build());
    }

    @Test
    void emailRepetidoEmDuasLojas_naoLancaENaoEnviaParaNinguem() {
        String email = "rep-" + UUID.randomUUID() + "@teste.com";
        salvar("a-" + UUID.randomUUID(), email, 711L);
        salvar("b-" + UUID.randomUUID(), email, 712L);

        assertThatCode(() -> assertThat(service.solicitarReset(email)).isFalse()).doesNotThrowAnyException();
    }

    @Test
    void emailUnico_encontraOUsuario() {
        String email = "uni-" + UUID.randomUUID() + "@teste.com";
        salvar("c-" + UUID.randomUUID(), email, 713L);

        assertThat(service.solicitarReset(email)).isTrue();
    }

    @Test
    void loginIgualAoEmail_funcionaEmQualquerCaixa() {
        String email = "log-" + UUID.randomUUID() + "@teste.com";
        salvar(email, email, 714L);

        assertThat(service.solicitarReset(email.toUpperCase())).isTrue();
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `mvn test -pl pascoa-monolith -Dtest='AcessoPorEmailTest,PasswordResetEmailDuplicadoTest'`
Expected: erro de compilação (`existsByLoginIgnoreCase` ausente) ou falhas de asserção.

- [ ] **Step 3: Migration, enum e `Loja`**

`<res>/db/migration/V20__lojas_onboarding.sql`:

```sql
ALTER TABLE lojas ADD COLUMN segmento VARCHAR(10);
ALTER TABLE lojas ADD COLUMN onboarding_concluido BOOLEAN NOT NULL DEFAULT TRUE;
```

`<main>/seguranca/entity/Segmento.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.entity;

public enum Segmento {
    DOCES("Doces"),
    SALGADOS("Salgados"),
    AMBOS("Doces e salgados");

    private final String descricao;

    Segmento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
```

Em `<main>/seguranca/entity/Loja.java`, acrescentar os imports `jakarta.persistence.EnumType` e `jakarta.persistence.Enumerated` e, depois do campo `nome`:

```java
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Segmento segmento;

    @Builder.Default
    @Column(name = "onboarding_concluido", nullable = false)
    private boolean onboardingConcluido = true;
```

- [ ] **Step 4: Repositório e login**

Em `<main>/seguranca/repository/UsuarioRepository.java`, trocar `Optional<Usuario> findByEmail(String email);` por:

```java
    List<Usuario> findAllByEmailIgnoreCase(String email);

    boolean existsByLoginIgnoreCase(String login);
```

Em `UsuarioService.loadUserByUsername`, trocar a busca do usuário por:

```java
        Usuario usuario = usuarioRepository.findByLogin(login)
            .or(() -> usuarioRepository.findByLogin(login.toLowerCase()))
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + login));
```

Em `<main>/seguranca/service/TwoFactorAuthenticationSuccessHandler.java`, no bloco que carrega o `usuario` e trata `usuario == null`, trocar:

```java
        if (usuario == null) {
            response.sendRedirect("/dashboard");
            return;
        }
```

por:

```java
        if (usuario == null || usuario.getLojaId() != Loja.PLATAFORMA_ID) {
            response.sendRedirect("/dashboard");
            return;
        }
```

(import `br.com.seuprojeto.pascoa.seguranca.entity.Loja`).

Em `PasswordResetService.solicitarReset`, trocar o trecho de busca (`findByLogin` seguido de `findByEmail`) por:

```java
        Optional<Usuario> optUsuario = usuarioRepository.findByLogin(loginOuEmail)
            .or(() -> usuarioRepository.findByLogin(loginOuEmail.toLowerCase()));
        if (optUsuario.isEmpty()) {
            List<Usuario> porEmail = usuarioRepository.findAllByEmailIgnoreCase(loginOuEmail);
            if (porEmail.size() == 1) {
                optUsuario = Optional.of(porEmail.get(0));
            }
        }
```

(import `java.util.List` se faltar; o restante do método — o `if (optUsuario.isEmpty())` com o log e `return false` — continua como está.)

Em `<res>/templates/login.html:26`, trocar `>Login</label>` por `>E-mail ou usuário</label>`.

- [ ] **Step 5: Rodar os testes**

Run: `mvn test -pl pascoa-monolith -Dtest='AcessoPorEmailTest,PasswordResetEmailDuplicadoTest,PasswordResetServiceTest,RolePermissionsTest'`
Expected: todos passando. Se `PasswordResetServiceTest` referenciar `findByEmail`, ajustar para o método novo.

- [ ] **Step 6: Suíte completa e commit**

Run: a suíte completa. Expected: 0 falhas.

```bash
git add pascoa-monolith/src
git commit -m "feat(acesso): login por e-mail, 2FA só na plataforma e recuperação com e-mail repetido (V20)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Padrões da loja nova (canais e templates)

**Files:**
- Create: `<res>/defaults/notificacoes.json`, `<main>/seguranca/service/LojaPadroesService.java`
- Modify: `<main>/notificacao/repository/TemplateNotificacaoRepository.java`
- Create test: `<test>/seguranca/LojaPadroesServiceTest.java`

**Interfaces:**
- Consumes: `ConfiguracaoCanalRepository.findByTipo`, `TemplateNotificacao` e `ConfiguracaoCanal` (builders), tenant atual via `TenantContext`.
- Produces: `LojaPadroesService.criar()` (idempotente; cria no tenant atual canais EMAIL, WHATSAPP e SMS inativos e em modo de teste, e os templates do JSON); `TemplateNotificacaoRepository.existsByEventoGatilhoAndCanal(EventoNotificacao, CanalNotificacao)`.

- [ ] **Step 1: Teste**

`<test>/seguranca/LojaPadroesServiceTest.java`:

```java
package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.repository.ConfiguracaoCanalRepository;
import br.com.seuprojeto.pascoa.notificacao.repository.TemplateNotificacaoRepository;
import br.com.seuprojeto.pascoa.seguranca.service.LojaPadroesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LojaPadroesServiceTest {

    @Autowired private LojaPadroesService service;
    @Autowired private ConfiguracaoCanalRepository canais;
    @Autowired private TemplateNotificacaoRepository templates;

    @Test
    void criar_geraTresCanaisInativosEmModoDeTeste_eUmTemplatePorEventoECanalDeMensagem() {
        TenantContext.executar(721L, service::criar);

        var canaisDaLoja = TenantContext.calcular(721L, () -> canais.findAll());
        assertThat(canaisDaLoja).extracting(c -> c.getTipo()).containsExactlyInAnyOrder(CanalNotificacao.values());
        assertThat(canaisDaLoja).allMatch(c -> !c.getAtivo() && c.getTestMode());

        var templatesDaLoja = TenantContext.calcular(721L, () -> templates.findAll());
        assertThat(templatesDaLoja).hasSize(EventoNotificacao.values().length * 2);
        assertThat(templatesDaLoja).allMatch(t -> t.getAtivo() && !t.getCorpo().isBlank());
        assertThat(templatesDaLoja).noneMatch(t -> t.getCorpo().toLowerCase().contains("páscoa")
            || t.getCorpo().toLowerCase().contains("pascoa"));
    }

    @Test
    void criar_eIdempotente() {
        TenantContext.executar(722L, service::criar);
        long canaisAntes = TenantContext.calcular(722L, () -> canais.count());
        long templatesAntes = TenantContext.calcular(722L, () -> templates.count());

        TenantContext.executar(722L, service::criar);

        assertThat(TenantContext.calcular(722L, () -> canais.count())).isEqualTo(canaisAntes);
        assertThat(TenantContext.calcular(722L, () -> templates.count())).isEqualTo(templatesAntes);
    }

    @Test
    void criar_naoAfetaOutraLoja() {
        long antes = TenantContext.calcular(1L, () -> templates.count());

        TenantContext.executar(723L, service::criar);

        assertThat(TenantContext.calcular(1L, () -> templates.count())).isEqualTo(antes);
        assertThat(TenantContext.calcular(724L, () -> templates.count())).isZero();
    }
}
```

Run: `mvn test -pl pascoa-monolith -Dtest=LojaPadroesServiceTest` — Expected: erro de compilação (`LojaPadroesService` ausente).

- [ ] **Step 2: Conteúdo padrão**

`<res>/defaults/notificacoes.json` — 20 modelos (10 eventos × EMAIL e WHATSAPP). Antes de escrever, abra `<res>/db/migration/V14__novas_notificacoes_item25.sql` e copie o **formato** do campo `variaveis` dos inserts de lá (ex.: lista separada por vírgula); use o mesmo formato em todos. Os marcadores suportados pelo `NotificacaoService` são `{nome}`, `{numeroPedido}`, `{dataEntrega}`, `{link}`, `{valor}`. Texto neutro, sem a palavra "Páscoa".

```json
[
  {"evento": "PEDIDO_CONFIRMADO", "canal": "EMAIL", "assunto": "Pedido {numeroPedido} confirmado",
   "corpo": "<p>Olá, {nome}!</p><p>Seu pedido {numeroPedido} foi confirmado. Entrega prevista para {dataEntrega}.</p><p>Acompanhe o andamento: <a href=\"{link}\">{link}</a></p>",
   "variaveis": "nome, numeroPedido, dataEntrega, link"},
  {"evento": "PEDIDO_CONFIRMADO", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Seu pedido {numeroPedido} foi confirmado. Entrega prevista para {dataEntrega}. Acompanhe: {link}",
   "variaveis": "nome, numeroPedido, dataEntrega, link"},
  {"evento": "PRODUCAO_INICIADA", "canal": "EMAIL", "assunto": "Seu pedido {numeroPedido} entrou em produção",
   "corpo": "<p>Olá, {nome}!</p><p>Começamos a produzir o seu pedido {numeroPedido}. Avisaremos quando estiver pronto.</p>",
   "variaveis": "nome, numeroPedido"},
  {"evento": "PRODUCAO_INICIADA", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Começamos a produzir o seu pedido {numeroPedido}. Avisaremos quando estiver pronto.",
   "variaveis": "nome, numeroPedido"},
  {"evento": "PEDIDO_PRONTO", "canal": "EMAIL", "assunto": "Seu pedido {numeroPedido} está pronto",
   "corpo": "<p>Olá, {nome}!</p><p>Seu pedido {numeroPedido} está pronto. Combine conosco a retirada ou a entrega.</p>",
   "variaveis": "nome, numeroPedido"},
  {"evento": "PEDIDO_PRONTO", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Seu pedido {numeroPedido} está pronto. Combine conosco a retirada ou a entrega.",
   "variaveis": "nome, numeroPedido"},
  {"evento": "PEDIDO_ENTREGUE", "canal": "EMAIL", "assunto": "Pedido {numeroPedido} entregue",
   "corpo": "<p>Olá, {nome}!</p><p>Seu pedido {numeroPedido} foi entregue. Obrigado pela preferência!</p>",
   "variaveis": "nome, numeroPedido"},
  {"evento": "PEDIDO_ENTREGUE", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Seu pedido {numeroPedido} foi entregue. Obrigado pela preferência!",
   "variaveis": "nome, numeroPedido"},
  {"evento": "PAGAMENTO_RECEBIDO", "canal": "EMAIL", "assunto": "Pagamento recebido",
   "corpo": "<p>Olá, {nome}!</p><p>Recebemos o seu pagamento de {valor} referente ao pedido {numeroPedido}. Obrigado!</p>",
   "variaveis": "nome, numeroPedido, valor"},
  {"evento": "PAGAMENTO_RECEBIDO", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Recebemos o seu pagamento de {valor} referente ao pedido {numeroPedido}. Obrigado!",
   "variaveis": "nome, numeroPedido, valor"},
  {"evento": "PEDIDO_CANCELADO", "canal": "EMAIL", "assunto": "Pedido {numeroPedido} cancelado",
   "corpo": "<p>Olá, {nome}!</p><p>Seu pedido {numeroPedido} foi cancelado. Se tiver dúvidas, fale com a gente.</p>",
   "variaveis": "nome, numeroPedido"},
  {"evento": "PEDIDO_CANCELADO", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Seu pedido {numeroPedido} foi cancelado. Se tiver dúvidas, fale com a gente.",
   "variaveis": "nome, numeroPedido"},
  {"evento": "ORCAMENTO_APROVADO", "canal": "EMAIL", "assunto": "Orçamento aprovado",
   "corpo": "<p>Olá, {nome}!</p><p>Seu orçamento foi aprovado. Obrigado! Veja os detalhes: <a href=\"{link}\">{link}</a></p>",
   "variaveis": "nome, link"},
  {"evento": "ORCAMENTO_APROVADO", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Seu orçamento foi aprovado. Obrigado! Detalhes: {link}",
   "variaveis": "nome, link"},
  {"evento": "ORCAMENTO_RECUSADO", "canal": "EMAIL", "assunto": "Orçamento recusado",
   "corpo": "<p>Olá, {nome}!</p><p>Registramos a recusa do seu orçamento. Se quiser ajustar algo, é só responder.</p>",
   "variaveis": "nome"},
  {"evento": "ORCAMENTO_RECUSADO", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! Registramos a recusa do seu orçamento. Se quiser ajustar algo, é só responder.",
   "variaveis": "nome"},
  {"evento": "ANIVERSARIO_CLIENTE", "canal": "EMAIL", "assunto": "Feliz aniversário, {nome}!",
   "corpo": "<p>Olá, {nome}!</p><p>Passando para desejar um feliz aniversário! 🎉</p><p>Com carinho, nossa equipe.</p>",
   "variaveis": "nome"},
  {"evento": "ANIVERSARIO_CLIENTE", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Feliz aniversário, {nome}! 🎉 Com carinho, nossa equipe.",
   "variaveis": "nome"},
  {"evento": "ORCAMENTO_EXPIRANDO", "canal": "EMAIL", "assunto": "Seu orçamento vence em breve",
   "corpo": "<p>Olá, {nome}!</p><p>O seu orçamento vence em breve. Para confirmar, acesse: <a href=\"{link}\">{link}</a></p>",
   "variaveis": "nome, link"},
  {"evento": "ORCAMENTO_EXPIRANDO", "canal": "WHATSAPP", "assunto": null,
   "corpo": "Olá, {nome}! O seu orçamento vence em breve. Para confirmar, acesse: {link}",
   "variaveis": "nome, link"}
]
```

- [ ] **Step 3: Repositório e service**

Em `TemplateNotificacaoRepository`, acrescentar:

```java
    boolean existsByEventoGatilhoAndCanal(EventoNotificacao evento, CanalNotificacao canal);
```

`<main>/seguranca/service/LojaPadroesService.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.service;

import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.ConfiguracaoCanal;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.TemplateNotificacao;
import br.com.seuprojeto.pascoa.notificacao.repository.ConfiguracaoCanalRepository;
import br.com.seuprojeto.pascoa.notificacao.repository.TemplateNotificacaoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LojaPadroesService {

    private static final String ARQUIVO = "defaults/notificacoes.json";

    private final ConfiguracaoCanalRepository canais;
    private final TemplateNotificacaoRepository templates;
    private final ObjectMapper objectMapper;

    public record ModeloTemplate(EventoNotificacao evento, CanalNotificacao canal, String assunto,
                                 String corpo, String variaveis) {
    }

    @Transactional
    public void criar() {
        for (CanalNotificacao tipo : CanalNotificacao.values()) {
            if (canais.findByTipo(tipo).isEmpty()) {
                canais.save(ConfiguracaoCanal.builder().tipo(tipo).ativo(false).testMode(true).build());
            }
        }
        for (ModeloTemplate modelo : lerModelos()) {
            if (!templates.existsByEventoGatilhoAndCanal(modelo.evento(), modelo.canal())) {
                templates.save(TemplateNotificacao.builder()
                    .eventoGatilho(modelo.evento())
                    .canal(modelo.canal())
                    .assunto(modelo.assunto())
                    .corpo(modelo.corpo())
                    .variaveis(modelo.variaveis())
                    .ativo(true)
                    .build());
            }
        }
    }

    private List<ModeloTemplate> lerModelos() {
        try (InputStream entrada = new ClassPathResource(ARQUIVO).getInputStream()) {
            return objectMapper.readValue(entrada, new TypeReference<List<ModeloTemplate>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + ARQUIVO, e);
        }
    }
}
```

Se `ConfiguracaoCanal.builder()` exigir campos obrigatórios (`apiUrl`, `remetente`), usar `""` como a migration V14 usa para o SMS. Confirmar lendo a entidade.

- [ ] **Step 4: Rodar e commitar**

Run: `mvn test -pl pascoa-monolith -Dtest=LojaPadroesServiceTest` — Expected: 3 testes passando; depois a suíte completa — Expected: 0 falhas.

```bash
git add pascoa-monolith/src
git commit -m "feat(loja): padrões de canais e templates de notificação para loja nova

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Cadastro de loja (`/cadastro`)

**Files:**
- Create: `<main>/seguranca/dto/CadastroForm.java`, `<main>/seguranca/service/CadastroLojaService.java`, `<main>/seguranca/controller/CadastroController.java`, `<res>/templates/cadastro/form.html`
- Modify: `<main>/config/SecurityConfig.java` (rota pública), `<main>/config/RateLimitFilter.java`, `<res>/templates/login.html` (link "Criar conta")
- Create tests: `<test>/seguranca/CadastroLojaServiceTest.java`, `<test>/seguranca/CadastroLojaFalhaTest.java`, `<test>/seguranca/CadastroControllerTest.java`, `<test>/config/RateLimitCadastroTest.java`

**Interfaces:**
- Consumes: `LojaPadroesService.criar()`, `UsuarioRepository.existsByLoginIgnoreCase`, `Loja` builder, `UsuarioPrincipal`, `SecurityContextRepository` (bean existente).
- Produces: `CadastroLojaService.cadastrar(CadastroForm)` devolve o `Usuario` dono; lança `CadastroLojaService.EmailJaCadastradoException` ou `CadastroLojaService.FalhaCadastroException`.

- [ ] **Step 1: Testes**

`<test>/seguranca/CadastroLojaServiceTest.java`:

```java
package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.notificacao.repository.ConfiguracaoCanalRepository;
import br.com.seuprojeto.pascoa.notificacao.repository.TemplateNotificacaoRepository;
import br.com.seuprojeto.pascoa.seguranca.dto.CadastroForm;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.CadastroLojaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CadastroLojaServiceTest {

    @Autowired private CadastroLojaService service;
    @Autowired private LojaRepository lojas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ConfiguracaoCanalRepository canais;
    @Autowired private TemplateNotificacaoRepository templates;
    @Autowired private PasswordEncoder encoder;

    private CadastroForm form(String email) {
        CadastroForm f = new CadastroForm();
        f.setNomeLoja("Doces da Ana");
        f.setNomeDono("Ana");
        f.setEmail(email);
        f.setSenha("senha-forte-1");
        f.setConfirmacaoSenha("senha-forte-1");
        return f;
    }

    @Test
    void cadastrar_criaLojaDonoCanaisETemplates_soNaLojaNova() {
        String email = "Ana-" + UUID.randomUUID() + "@Teste.com";
        long templatesDaLoja1 = TenantContext.calcular(1L, () -> templates.count());

        var dono = service.cadastrar(form(email));

        assertThat(dono.getLogin()).isEqualTo(email.toLowerCase());
        assertThat(dono.getEmail()).isEqualTo(email.toLowerCase());
        assertThat(dono.getRole()).isEqualTo(Role.ADMIN);
        assertThat(encoder.matches("senha-forte-1", dono.getSenha())).isTrue();
        var loja = lojas.findById(dono.getLojaId()).orElseThrow();
        assertThat(loja.getNome()).isEqualTo("Doces da Ana");
        assertThat(loja.isOnboardingConcluido()).isFalse();
        assertThat(loja.getSegmento()).isNull();
        assertThat(TenantContext.calcular(dono.getLojaId(), () -> canais.count())).isEqualTo(3);
        assertThat(TenantContext.calcular(dono.getLojaId(), () -> templates.count())).isEqualTo(20);
        assertThat(TenantContext.calcular(1L, () -> templates.count())).isEqualTo(templatesDaLoja1);
    }

    @Test
    void cadastrar_comEmailJaUsado_aindaQueEmOutraCaixa_rejeitaENaoCriaNada() {
        String email = "dup-" + UUID.randomUUID() + "@teste.com";
        service.cadastrar(form(email));
        long lojasAntes = lojas.count();

        assertThatThrownBy(() -> service.cadastrar(form(email.toUpperCase())))
            .isInstanceOf(CadastroLojaService.EmailJaCadastradoException.class);

        assertThat(lojas.count()).isEqualTo(lojasAntes);
    }
}
```

`<test>/seguranca/CadastroLojaFalhaTest.java`:

```java
package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.seguranca.dto.CadastroForm;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.CadastroLojaService;
import br.com.seuprojeto.pascoa.seguranca.service.LojaPadroesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class CadastroLojaFalhaTest {

    @MockBean private LojaPadroesService padroes;
    @Autowired private CadastroLojaService service;
    @Autowired private LojaRepository lojas;
    @Autowired private UsuarioRepository usuarios;

    @Test
    void falhaNaCriacaoDosPadroes_desfazLojaEUsuario() {
        doThrow(new IllegalStateException("falha simulada")).when(padroes).criar();
        String email = "falha-" + UUID.randomUUID() + "@teste.com";
        CadastroForm form = new CadastroForm();
        form.setNomeLoja("Loja que falha");
        form.setNomeDono("Dono");
        form.setEmail(email);
        form.setSenha("senha-forte-1");
        form.setConfirmacaoSenha("senha-forte-1");
        long lojasAntes = lojas.count();

        assertThatThrownBy(() -> service.cadastrar(form))
            .isInstanceOf(CadastroLojaService.FalhaCadastroException.class);

        assertThat(lojas.count()).isEqualTo(lojasAntes);
        assertThat(usuarios.existsByLoginIgnoreCase(email)).isFalse();
    }
}
```

`<test>/seguranca/CadastroControllerTest.java`:

```java
package br.com.seuprojeto.pascoa.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CadastroControllerTest {

    @Autowired private MockMvc mvc;

    @Test
    void formularioEPublico() throws Exception {
        mvc.perform(get("/cadastro")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Criar conta")));
    }

    @Test
    void cadastroValido_redirecionaParaOnboarding_eJaEstaLogado() throws Exception {
        String email = "novo-" + UUID.randomUUID() + "@teste.com";

        var resultado = mvc.perform(post("/cadastro").with(csrf())
                .param("nomeLoja", "Salgados do João").param("nomeDono", "João")
                .param("email", email).param("senha", "senha-forte-1")
                .param("confirmacaoSenha", "senha-forte-1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/onboarding"))
            .andReturn();

        var sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/dashboard").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Dashboard")));
    }

    @Test
    void senhasDiferentes_voltaAoFormularioComMensagem() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                .param("nomeLoja", "X").param("nomeDono", "Y")
                .param("email", "x-" + UUID.randomUUID() + "@teste.com")
                .param("senha", "senha-forte-1").param("confirmacaoSenha", "outra-senha-9"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("As senhas não conferem")));
    }

    @Test
    void emailInvalido_voltaAoFormulario() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                .param("nomeLoja", "X").param("nomeDono", "Y").param("email", "nao-e-email")
                .param("senha", "senha-forte-1").param("confirmacaoSenha", "senha-forte-1"))
            .andExpect(status().isOk());
    }

    @Test
    void emailJaUsado_mostraMensagemSemIdentificarLoja() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@teste.com";
        for (int i = 0; i < 2; i++) {
            var req = post("/cadastro").with(csrf())
                .param("nomeLoja", "L").param("nomeDono", "D").param("email", email)
                .param("senha", "senha-forte-1").param("confirmacaoSenha", "senha-forte-1");
            if (i == 1) {
                mvc.perform(req).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Já existe uma conta com este e-mail")));
            } else {
                mvc.perform(req).andExpect(status().is3xxRedirection());
            }
        }
    }
}
```

`<test>/config/RateLimitCadastroTest.java`:

```java
package br.com.seuprojeto.pascoa.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitCadastroTest {

    private int postar(RateLimitFilter filtro, String ip) throws Exception {
        var req = new MockHttpServletRequest("POST", "/cadastro");
        req.setRemoteAddr(ip);
        var res = new MockHttpServletResponse();
        filtro.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void sextoPostNaMesmaHoraDoMesmoIp_retorna429() throws Exception {
        var filtro = new RateLimitFilter();
        for (int i = 0; i < 5; i++) {
            assertThat(postar(filtro, "10.9.9.9")).isEqualTo(200);
        }
        assertThat(postar(filtro, "10.9.9.9")).isEqualTo(429);
        assertThat(postar(filtro, "10.9.9.8")).isEqualTo(200);
    }

    @Test
    void getDoFormularioNaoConsomeLimite() throws Exception {
        var filtro = new RateLimitFilter();
        for (int i = 0; i < 10; i++) {
            var req = new MockHttpServletRequest("GET", "/cadastro");
            req.setRemoteAddr("10.9.9.7");
            var res = new MockHttpServletResponse();
            filtro.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }
}
```

Run: `mvn test -pl pascoa-monolith -Dtest='CadastroLojaServiceTest,CadastroLojaFalhaTest,CadastroControllerTest,RateLimitCadastroTest'` — Expected: erro de compilação (classes ausentes).

- [ ] **Step 2: Form, service e controller**

`<main>/seguranca/dto/CadastroForm.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CadastroForm {

    @NotBlank(message = "Informe o nome da loja")
    @Size(max = 150, message = "O nome da loja pode ter no máximo 150 caracteres")
    private String nomeLoja;

    @NotBlank(message = "Informe o seu nome")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres")
    private String nomeDono;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 60, message = "O e-mail pode ter no máximo 60 caracteres")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres")
    private String senha;

    @NotBlank(message = "Confirme a senha")
    private String confirmacaoSenha;
}
```

`<main>/seguranca/service/CadastroLojaService.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.service;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.seguranca.dto.CadastroForm;
import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CadastroLojaService {

    private final LojaRepository lojaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LojaPadroesService lojaPadroesService;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    public static class EmailJaCadastradoException extends RuntimeException {
        public EmailJaCadastradoException() {
            super("E-mail já cadastrado");
        }
    }

    public static class FalhaCadastroException extends RuntimeException {
        public FalhaCadastroException(Throwable causa) {
            super("Falha ao criar a loja", causa);
        }
    }

    public Usuario cadastrar(CadastroForm form) {
        String email = form.getEmail().trim().toLowerCase();
        TransactionTemplate criacao = new TransactionTemplate(transactionManager);
        TransactionTemplate padroes = new TransactionTemplate(transactionManager);
        padroes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Usuario dono;
        try {
            dono = criacao.execute(status -> {
                if (usuarioRepository.existsByLoginIgnoreCase(email)) {
                    throw new EmailJaCadastradoException();
                }
                Loja loja = lojaRepository.save(Loja.builder()
                    .nome(form.getNomeLoja().trim())
                    .onboardingConcluido(false)
                    .build());
                return usuarioRepository.save(Usuario.builder()
                    .nome(form.getNomeDono().trim())
                    .login(email)
                    .email(email)
                    .senha(passwordEncoder.encode(form.getSenha()))
                    .role(Role.ADMIN)
                    .ativo(true)
                    .lojaId(loja.getId())
                    .build());
            });
        } catch (DataIntegrityViolationException e) {
            throw new EmailJaCadastradoException();
        }

        try {
            TenantContext.executar(dono.getLojaId(),
                () -> padroes.executeWithoutResult(status -> lojaPadroesService.criar()));
        } catch (RuntimeException e) {
            log.error("[CADASTRO] Falha ao criar os padrões da loja {}: {}", dono.getLojaId(), e.getMessage(), e);
            desfazer(dono);
            throw new FalhaCadastroException(e);
        }
        return dono;
    }

    private void desfazer(Usuario dono) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                usuarioRepository.deleteById(dono.getId());
                lojaRepository.deleteById(dono.getLojaId());
            });
        } catch (RuntimeException e) {
            log.error("[CADASTRO] Não foi possível desfazer a loja {}: {}", dono.getLojaId(), e.getMessage(), e);
        }
    }
}
```

`<main>/seguranca/controller/CadastroController.java`:

```java
package br.com.seuprojeto.pascoa.seguranca.controller;

import br.com.seuprojeto.pascoa.seguranca.dto.CadastroForm;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.service.CadastroLojaService;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cadastro")
@RequiredArgsConstructor
public class CadastroController {

    private final CadastroLojaService service;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("cadastroForm", new CadastroForm());
        return "cadastro/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("cadastroForm") CadastroForm form, BindingResult result,
                        HttpServletRequest request, HttpServletResponse response) {
        if (form.getSenha() != null && !form.getSenha().equals(form.getConfirmacaoSenha())) {
            result.rejectValue("confirmacaoSenha", "diferente", "As senhas não conferem");
        }
        if (result.hasErrors()) {
            return "cadastro/form";
        }
        try {
            Usuario dono = service.cadastrar(form);
            autenticar(dono, request, response);
            return "redirect:/onboarding";
        } catch (CadastroLojaService.EmailJaCadastradoException e) {
            result.rejectValue("email", "duplicado", "Já existe uma conta com este e-mail");
        } catch (CadastroLojaService.FalhaCadastroException e) {
            result.reject("falha", "Não foi possível criar a conta, tente novamente.");
        }
        return "cadastro/form";
    }

    private void autenticar(Usuario dono, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        UsuarioPrincipal principal = new UsuarioPrincipal(dono);
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);
    }
}
```

- [ ] **Step 3: Template, rota pública, rate limit e link**

`<res>/templates/cadastro/form.html`:

```html
<!DOCTYPE html>
<html lang="pt-BR" th:replace="~{fragments/layout-auth :: layout(~{::title}, ~{::main})}"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Criar conta — Páscoa Gestão</title>
</head>
<body>
<main class="auth-shell">
    <div th:replace="~{fragments/auth-brand :: brand('Crie a conta da sua loja')}"></div>

    <div class="card auth-card">
        <div class="card-body p-4">
            <h1 class="h4 fw-bold mb-1">Criar conta</h1>
            <p class="text-body-secondary small mb-3">Leva menos de um minuto. Você entra na hora.</p>

            <div th:if="${#fields.hasGlobalErrors()}" class="alert alert-danger py-2 small mb-3" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-1" aria-hidden="true"></i>
                <span th:each="e : ${#fields.globalErrors()}" th:text="${e}"></span>
            </div>

            <form th:action="@{/cadastro}" th:object="${cadastroForm}" method="post" data-loading="Criando…">
                <div class="mb-3">
                    <label for="nomeLoja" class="form-label fw-semibold">Nome da loja</label>
                    <input type="text" id="nomeLoja" th:field="*{nomeLoja}" class="form-control"
                           th:classappend="${#fields.hasErrors('nomeLoja')} ? 'is-invalid'"
                           autofocus autocomplete="organization" maxlength="150" required>
                    <div class="invalid-feedback" th:errors="*{nomeLoja}"></div>
                </div>
                <div class="mb-3">
                    <label for="nomeDono" class="form-label fw-semibold">Seu nome</label>
                    <input type="text" id="nomeDono" th:field="*{nomeDono}" class="form-control"
                           th:classappend="${#fields.hasErrors('nomeDono')} ? 'is-invalid'"
                           autocomplete="name" maxlength="100" required>
                    <div class="invalid-feedback" th:errors="*{nomeDono}"></div>
                </div>
                <div class="mb-3">
                    <label for="email" class="form-label fw-semibold">E-mail (será o seu login)</label>
                    <input type="email" id="email" th:field="*{email}" class="form-control"
                           th:classappend="${#fields.hasErrors('email')} ? 'is-invalid'"
                           autocomplete="email" autocapitalize="none" spellcheck="false" maxlength="60" required>
                    <div class="invalid-feedback" th:errors="*{email}"></div>
                </div>
                <div class="mb-3">
                    <label for="senha" class="form-label fw-semibold">Senha</label>
                    <input type="password" id="senha" th:field="*{senha}" class="form-control"
                           th:classappend="${#fields.hasErrors('senha')} ? 'is-invalid'"
                           autocomplete="new-password" minlength="8" maxlength="72" required>
                    <div class="form-text">Mínimo de 8 caracteres.</div>
                    <div class="invalid-feedback" th:errors="*{senha}"></div>
                </div>
                <div class="mb-4">
                    <label for="confirmacaoSenha" class="form-label fw-semibold">Confirme a senha</label>
                    <input type="password" id="confirmacaoSenha" th:field="*{confirmacaoSenha}" class="form-control"
                           th:classappend="${#fields.hasErrors('confirmacaoSenha')} ? 'is-invalid'"
                           autocomplete="new-password" required>
                    <div class="invalid-feedback" th:errors="*{confirmacaoSenha}"></div>
                </div>
                <button type="submit" class="btn btn-success w-100 py-2">
                    <i class="bi bi-person-plus me-2" aria-hidden="true"></i><span class="rotulo">Criar conta</span>
                </button>
            </form>

            <div class="text-center mt-3">
                <a th:href="@{/login}" class="auth-link small">Já tenho conta</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
```

Em `<main>/config/SecurityConfig.java`, depois de `.requestMatchers("/auth/**").permitAll()`, acrescentar:

```java
                .requestMatchers("/cadastro", "/cadastro/**").permitAll()
```

Em `<main>/config/RateLimitFilter.java`: acrescentar `CADASTRO` ao enum `Category` (`CATALOGO, TRACKING, LOGIN, CADASTRO, NONE`); em `categorize`, depois da linha do `LOGIN`:

```java
        if ("POST".equalsIgnoreCase(method) && "/cadastro".equals(path))  return Category.CADASTRO;
```

em `buildBucket`, depois do caso `LOGIN`:

```java
            case CADASTRO -> Bandwidth.classic(5,   Refill.intervally(5,   Duration.ofHours(1)));
```

e em `refillSeconds`, depois do caso `LOGIN`:

```java
            case CADASTRO -> 60 * 60L;
```

Em `<res>/templates/login.html`, depois do `div` do link "Esqueceu sua senha?", acrescentar:

```html
            <div class="text-center mt-2">
                <a th:href="@{/cadastro}" class="auth-link small">Criar conta</a>
            </div>
```

- [ ] **Step 4: Rodar, suíte e commit**

Run: `mvn test -pl pascoa-monolith -Dtest='CadastroLojaServiceTest,CadastroLojaFalhaTest,CadastroControllerTest,RateLimitCadastroTest,RolePermissionsTest'` — Expected: todos passando. Depois a suíte completa e a de integração — Expected: 0 falhas.

```bash
git add pascoa-monolith/src
git commit -m "feat(cadastro): loja nova se cadastra em /cadastro e entra logada

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Onboarding de 3 passos e aviso no dashboard

**Files:**
- Create: `<res>/defaults/produtos-exemplo.json`, `<main>/onboarding/service/OnboardingService.java`, `<main>/onboarding/controller/OnboardingController.java`, `<main>/onboarding/dto/ProdutosExemploForm.java`, `<res>/templates/onboarding/segmento.html`, `produtos.html`, `pedido.html`
- Modify: `<main>/cadastro/repository/CategoriaProdutoRepository.java`, `<main>/cadastro/controller/DashboardController.java`, `<res>/templates/dashboard.html`
- Create tests: `<test>/onboarding/OnboardingServiceTest.java`, `<test>/onboarding/OnboardingControllerTest.java`

**Interfaces:**
- Consumes: `LojaRepository`, `CategoriaProdutoRepository`, `ProdutoRepository`, `UnidadeVenda`, `TenantContext`.
- Produces: `OnboardingService`: `Loja loja()`, `definirSegmento(Segmento)`, `List<ProdutoExemplo> exemplos()`, `int criarProdutos(ProdutosExemploForm)`, `concluir()`, `boolean pendente()`; `record ProdutoExemplo(Segmento segmento, String categoria, String nome, UnidadeVenda unidadeVenda, BigDecimal precoSugerido)`; `CategoriaProdutoRepository.findByNomeIgnoreCase(String)`.

- [ ] **Step 1: Testes**

`<test>/onboarding/OnboardingServiceTest.java` (não `@Transactional`; cada teste cria a própria loja e usa `TenantContext.executar/calcular` com o id dela):

```java
package br.com.seuprojeto.pascoa.onboarding;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.onboarding.dto.ProdutosExemploForm;
import br.com.seuprojeto.pascoa.onboarding.service.OnboardingService;
import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.entity.Segmento;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OnboardingServiceTest {

    @Autowired private OnboardingService service;
    @Autowired private LojaRepository lojas;
    @Autowired private CategoriaProdutoRepository categorias;
    @Autowired private ProdutoRepository produtos;

    private long novaLoja() {
        return lojas.save(Loja.builder().nome("Teste").onboardingConcluido(false).build()).getId();
    }

    @Test
    void segmentoDefineOsExemplosOferecidos() {
        long loja = novaLoja();

        TenantContext.executar(loja, () -> service.definirSegmento(Segmento.DOCES));
        var doces = TenantContext.calcular(loja, () -> service.exemplos());
        TenantContext.executar(loja, () -> service.definirSegmento(Segmento.SALGADOS));
        var salgados = TenantContext.calcular(loja, () -> service.exemplos());
        TenantContext.executar(loja, () -> service.definirSegmento(Segmento.AMBOS));
        var ambos = TenantContext.calcular(loja, () -> service.exemplos());

        assertThat(doces).isNotEmpty().allMatch(e -> e.segmento() == Segmento.DOCES);
        assertThat(salgados).isNotEmpty().allMatch(e -> e.segmento() == Segmento.SALGADOS);
        assertThat(ambos).hasSize(doces.size() + salgados.size());
    }

    @Test
    void criarProdutos_criaSoOsMarcadosComCategoriaEUnidade_semDuplicarNaSegundaVez() {
        long loja = novaLoja();
        TenantContext.executar(loja, () -> service.definirSegmento(Segmento.SALGADOS));
        var exemplos = TenantContext.calcular(loja, () -> service.exemplos());

        ProdutosExemploForm form = new ProdutosExemploForm();
        form.setIndices(List.of(0, 1));
        form.setPrecos(Map.of(0, new BigDecimal("99.90")));

        int primeira = TenantContext.calcular(loja, () -> service.criarProdutos(form));
        int segunda = TenantContext.calcular(loja, () -> service.criarProdutos(form));

        assertThat(primeira).isEqualTo(2);
        assertThat(segunda).isZero();
        List<Produto> criados = TenantContext.calcular(loja, () -> produtos.findAllByOrderByNomeAsc());
        assertThat(criados).hasSize(2);
        Produto primeiro = criados.stream().filter(p -> p.getNome().equals(exemplos.get(0).nome())).findFirst().orElseThrow();
        assertThat(primeiro.getPrecoVenda()).isEqualByComparingTo("99.90");
        assertThat(primeiro.getUnidadeVenda()).isEqualTo(exemplos.get(0).unidadeVenda());
        assertThat(primeiro.getCategoria().getNome()).isEqualTo(exemplos.get(0).categoria());
        List<CategoriaProduto> cats = TenantContext.calcular(loja, () -> categorias.findAll());
        assertThat(cats).extracting(CategoriaProduto::getNome).doesNotHaveDuplicates();
    }

    @Test
    void produtosDaLojaNovaNaoApareceNaLoja1() {
        long loja = novaLoja();
        TenantContext.executar(loja, () -> service.definirSegmento(Segmento.DOCES));
        long antes = TenantContext.calcular(1L, () -> produtos.count());
        ProdutosExemploForm form = new ProdutosExemploForm();
        form.setIndices(List.of(0));

        TenantContext.executar(loja, () -> service.criarProdutos(form));

        assertThat(TenantContext.calcular(1L, () -> produtos.count())).isEqualTo(antes);
    }

    @Test
    void concluirMarcaOOnboardingComoConcluido() {
        long loja = novaLoja();
        assertThat(TenantContext.calcular(loja, () -> service.pendente())).isTrue();

        TenantContext.executar(loja, () -> service.concluir());

        assertThat(TenantContext.calcular(loja, () -> service.pendente())).isFalse();
    }

    @Test
    void lojaInexistenteNaoEstaPendente() {
        assertThat(TenantContext.calcular(987654L, () -> service.pendente())).isFalse();
    }
}
```

`<test>/onboarding/OnboardingControllerTest.java`:

```java
package br.com.seuprojeto.pascoa.onboarding;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private LojaRepository lojas;

    private RequestPostProcessor dono() {
        long id = lojas.save(Loja.builder().nome("Loja do teste").onboardingConcluido(false).build()).getId();
        Usuario u = Usuario.builder().nome("Dono").login("dono" + id + "@teste.com").senha("x")
            .role(Role.ADMIN).ativo(true).lojaId(id).build();
        return user(new UsuarioPrincipal(u));
    }

    @Test
    void fluxoCompleto_dosTresPassosAoDashboardSemAviso() throws Exception {
        RequestPostProcessor dono = dono();

        mvc.perform(get("/onboarding").with(dono)).andExpect(redirectedUrl("/onboarding/segmento"));
        mvc.perform(get("/onboarding/segmento").with(dono)).andExpect(status().isOk());
        mvc.perform(post("/onboarding/segmento").with(dono).with(csrf()).param("segmento", "DOCES"))
            .andExpect(redirectedUrl("/onboarding/produtos"));
        mvc.perform(get("/onboarding/produtos").with(dono)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Brigadeiro")));
        mvc.perform(post("/onboarding/produtos").with(dono).with(csrf()).param("indices", "0", "1"))
            .andExpect(redirectedUrl("/onboarding/pedido"));
        mvc.perform(get("/onboarding/pedido").with(dono)).andExpect(status().isOk())
            .andExpect(content().string(containsString("/pedidos/wizard")));
        mvc.perform(get("/dashboard").with(dono)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Continue a configuração")));
        mvc.perform(post("/onboarding/concluir").with(dono).with(csrf())).andExpect(redirectedUrl("/dashboard"));
        mvc.perform(get("/dashboard").with(dono)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Continue a configuração"))));
    }

    @Test
    void produtosSemSegmento_voltamParaOPrimeiroPasso() throws Exception {
        mvc.perform(get("/onboarding/produtos").with(dono())).andExpect(redirectedUrl("/onboarding/segmento"));
    }

    @Test
    void onboardingExigeLogin() throws Exception {
        mvc.perform(get("/onboarding")).andExpect(status().is3xxRedirection());
    }
}
```

Run: `mvn test -pl pascoa-monolith -Dtest='OnboardingServiceTest,OnboardingControllerTest'` — Expected: erro de compilação (classes ausentes). Nota: a rota do pedido guiado é `/pedidos/wizard` (não `/pedidos/novo`).

- [ ] **Step 2: Conteúdo dos exemplos**

`<res>/defaults/produtos-exemplo.json` — preços sugeridos são ponto de partida e **precisam de revisão de quem conhece o negócio**:

```json
[
  {"segmento": "DOCES", "categoria": "Docinhos", "nome": "Brigadeiro", "unidadeVenda": "CENTO", "precoSugerido": 90.00},
  {"segmento": "DOCES", "categoria": "Docinhos", "nome": "Beijinho", "unidadeVenda": "CENTO", "precoSugerido": 90.00},
  {"segmento": "DOCES", "categoria": "Docinhos", "nome": "Docinhos sortidos", "unidadeVenda": "DUZIA", "precoSugerido": 36.00},
  {"segmento": "DOCES", "categoria": "Docinhos", "nome": "Trufa", "unidadeVenda": "DUZIA", "precoSugerido": 42.00},
  {"segmento": "DOCES", "categoria": "Bolos e tortas", "nome": "Bolo caseiro", "unidadeVenda": "KG", "precoSugerido": 55.00},
  {"segmento": "DOCES", "categoria": "Bolos e tortas", "nome": "Bolo de pote", "unidadeVenda": "UNIDADE", "precoSugerido": 12.00},
  {"segmento": "DOCES", "categoria": "Bolos e tortas", "nome": "Brownie", "unidadeVenda": "UNIDADE", "precoSugerido": 8.00},
  {"segmento": "DOCES", "categoria": "Bolos e tortas", "nome": "Cupcake", "unidadeVenda": "UNIDADE", "precoSugerido": 7.00},
  {"segmento": "SALGADOS", "categoria": "Salgados fritos", "nome": "Coxinha", "unidadeVenda": "CENTO", "precoSugerido": 85.00},
  {"segmento": "SALGADOS", "categoria": "Salgados fritos", "nome": "Kibe", "unidadeVenda": "CENTO", "precoSugerido": 90.00},
  {"segmento": "SALGADOS", "categoria": "Salgados fritos", "nome": "Pastel", "unidadeVenda": "DUZIA", "precoSugerido": 40.00},
  {"segmento": "SALGADOS", "categoria": "Salgados fritos", "nome": "Risole", "unidadeVenda": "CENTO", "precoSugerido": 85.00},
  {"segmento": "SALGADOS", "categoria": "Salgados assados", "nome": "Empada", "unidadeVenda": "CENTO", "precoSugerido": 95.00},
  {"segmento": "SALGADOS", "categoria": "Salgados assados", "nome": "Esfiha", "unidadeVenda": "DUZIA", "precoSugerido": 36.00},
  {"segmento": "SALGADOS", "categoria": "Salgados assados", "nome": "Enroladinho de salsicha", "unidadeVenda": "CENTO", "precoSugerido": 80.00},
  {"segmento": "SALGADOS", "categoria": "Salgados assados", "nome": "Pão de queijo", "unidadeVenda": "CENTO", "precoSugerido": 60.00}
]
```

- [ ] **Step 3: Repositório, form e service**

Em `CategoriaProdutoRepository`, acrescentar:

```java
    java.util.Optional<CategoriaProduto> findByNomeIgnoreCase(String nome);
```

`<main>/onboarding/dto/ProdutosExemploForm.java`:

```java
package br.com.seuprojeto.pascoa.onboarding.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProdutosExemploForm {

    private List<Integer> indices = new ArrayList<>();

    private Map<Integer, BigDecimal> precos = new HashMap<>();
}
```

`<main>/onboarding/service/OnboardingService.java`:

```java
package br.com.seuprojeto.pascoa.onboarding.service;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.onboarding.dto.ProdutosExemploForm;
import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.entity.Segmento;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String ARQUIVO = "defaults/produtos-exemplo.json";
    private static final BigDecimal PRECO_MINIMO = new BigDecimal("0.01");

    private final LojaRepository lojaRepository;
    private final CategoriaProdutoRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final ObjectMapper objectMapper;

    public record ProdutoExemplo(Segmento segmento, String categoria, String nome,
                                 UnidadeVenda unidadeVenda, BigDecimal precoSugerido) {
    }

    @Transactional(readOnly = true)
    public Loja loja() {
        return lojaRepository.findById(TenantContext.exigir()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public boolean pendente() {
        return lojaRepository.findById(TenantContext.atual()).map(l -> !l.isOnboardingConcluido()).orElse(false);
    }

    @Transactional
    public void definirSegmento(Segmento segmento) {
        Loja loja = loja();
        loja.setSegmento(segmento);
        lojaRepository.save(loja);
    }

    @Transactional(readOnly = true)
    public List<ProdutoExemplo> exemplos() {
        Segmento segmento = loja().getSegmento();
        if (segmento == null) {
            return List.of();
        }
        return lerExemplos().stream()
            .filter(e -> segmento == Segmento.AMBOS || e.segmento() == segmento)
            .toList();
    }

    @Transactional
    public int criarProdutos(ProdutosExemploForm form) {
        List<ProdutoExemplo> exemplos = exemplos();
        Set<String> existentes = new HashSet<>();
        produtoRepository.findAllByOrderByNomeAsc().forEach(p -> existentes.add(p.getNome().toLowerCase(Locale.ROOT)));
        int criados = 0;
        for (Integer indice : form.getIndices()) {
            if (indice == null || indice < 0 || indice >= exemplos.size()) {
                continue;
            }
            ProdutoExemplo exemplo = exemplos.get(indice);
            if (!existentes.add(exemplo.nome().toLowerCase(Locale.ROOT))) {
                continue;
            }
            BigDecimal preco = form.getPrecos().getOrDefault(indice, exemplo.precoSugerido());
            if (preco == null || preco.compareTo(PRECO_MINIMO) < 0) {
                preco = exemplo.precoSugerido();
            }
            produtoRepository.save(Produto.builder()
                .nome(exemplo.nome())
                .categoria(categoria(exemplo.categoria()))
                .unidadeVenda(exemplo.unidadeVenda())
                .precoVenda(preco)
                .ativo(true)
                .build());
            criados++;
        }
        return criados;
    }

    @Transactional
    public void concluir() {
        Loja loja = loja();
        loja.setOnboardingConcluido(true);
        lojaRepository.save(loja);
    }

    private CategoriaProduto categoria(String nome) {
        return categoriaRepository.findByNomeIgnoreCase(nome)
            .orElseGet(() -> categoriaRepository.save(CategoriaProduto.builder().nome(nome).build()));
    }

    private List<ProdutoExemplo> lerExemplos() {
        try (InputStream entrada = new ClassPathResource(ARQUIVO).getInputStream()) {
            return objectMapper.readValue(entrada, new TypeReference<List<ProdutoExemplo>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + ARQUIVO, e);
        }
    }
}
```

- [ ] **Step 4: Controller e templates**

`<main>/onboarding/controller/OnboardingController.java`:

```java
package br.com.seuprojeto.pascoa.onboarding.controller;

import br.com.seuprojeto.pascoa.onboarding.dto.ProdutosExemploForm;
import br.com.seuprojeto.pascoa.onboarding.service.OnboardingService;
import br.com.seuprojeto.pascoa.seguranca.entity.Segmento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService service;

    @GetMapping
    public String inicio() {
        return service.loja().getSegmento() == null ? "redirect:/onboarding/segmento" : "redirect:/onboarding/produtos";
    }

    @GetMapping("/segmento")
    public String segmento(Model model) {
        model.addAttribute("segmentos", Segmento.values());
        model.addAttribute("segmentoAtual", service.loja().getSegmento());
        return "onboarding/segmento";
    }

    @PostMapping("/segmento")
    public String salvarSegmento(@RequestParam Segmento segmento) {
        service.definirSegmento(segmento);
        return "redirect:/onboarding/produtos";
    }

    @GetMapping("/produtos")
    public String produtos(Model model) {
        if (service.loja().getSegmento() == null) {
            return "redirect:/onboarding/segmento";
        }
        model.addAttribute("exemplos", service.exemplos());
        return "onboarding/produtos";
    }

    @PostMapping("/produtos")
    public String salvarProdutos(@ModelAttribute ProdutosExemploForm form, RedirectAttributes ra) {
        int criados = service.criarProdutos(form);
        ra.addFlashAttribute("sucesso", criados + " produto(s) criado(s).");
        return "redirect:/onboarding/pedido";
    }

    @GetMapping("/pedido")
    public String pedido() {
        return "onboarding/pedido";
    }

    @PostMapping("/concluir")
    public String concluir() {
        service.concluir();
        return "redirect:/dashboard";
    }
}
```

`<res>/templates/onboarding/segmento.html`:

```html
<!DOCTYPE html>
<html th:replace="~{fragments/layout :: layout(~{::title}, ~{::#pageContent})}"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Configurar a loja — Sistema Páscoa</title>
</head>
<body>
<div id="pageContent">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0"><i class="bi bi-shop me-2 text-success" aria-hidden="true"></i>O que a sua loja faz?</h2>
        <span class="text-muted small">Passo 1 de 3</span>
    </div>

    <div class="card">
        <div class="card-body">
            <form th:action="@{/onboarding/segmento}" method="post">
                <div class="row g-3 mb-4">
                    <div class="col-md-4" th:each="s : ${segmentos}">
                        <div class="form-check border rounded p-3 h-100">
                            <input class="form-check-input ms-0 me-2" type="radio" name="segmento"
                                   th:id="${'seg-' + s.name()}" th:value="${s.name()}"
                                   th:checked="${segmentoAtual == s}" required>
                            <label class="form-check-label fw-semibold" th:for="${'seg-' + s.name()}"
                                   th:text="${s.descricao}"></label>
                        </div>
                    </div>
                </div>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-success px-4">
                        <i class="bi bi-arrow-right me-1" aria-hidden="true"></i>Continuar
                    </button>
                    <a th:href="@{/onboarding/pedido}" class="btn btn-outline-secondary">Pular</a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>
```

`<res>/templates/onboarding/produtos.html`:

```html
<!DOCTYPE html>
<html th:replace="~{fragments/layout :: layout(~{::title}, ~{::#pageContent})}"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Produtos de exemplo — Sistema Páscoa</title>
</head>
<body>
<div id="pageContent">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0"><i class="bi bi-box-seam me-2 text-success" aria-hidden="true"></i>Comece com alguns produtos</h2>
        <span class="text-muted small">Passo 2 de 3</span>
    </div>
    <p class="text-muted">Marque os que vendem na sua loja e ajuste o preço. Você pode mudar tudo depois.</p>

    <form th:action="@{/onboarding/produtos}" method="post">
        <div class="card mb-3">
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3">Usar</th>
                            <th scope="col">Produto</th>
                            <th scope="col">Categoria</th>
                            <th scope="col">Vendido por</th>
                            <th scope="col" class="pe-3">Preço (R$)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr th:each="e, i : ${exemplos}">
                            <td class="ps-3">
                                <input class="form-check-input" type="checkbox" name="indices"
                                       th:id="${'ex-' + i.index}" th:value="${i.index}" checked
                                       th:aria-label="${'Usar ' + e.nome}">
                            </td>
                            <td><label th:for="${'ex-' + i.index}" th:text="${e.nome}"></label></td>
                            <td th:text="${e.categoria}"></td>
                            <td th:text="${e.unidadeVenda.descricao}"></td>
                            <td class="pe-3">
                                <input type="number" class="form-control form-control-sm" step="0.01" min="0.01"
                                       th:name="${'precos[' + i.index + ']'}" th:value="${e.precoSugerido}"
                                       th:aria-label="${'Preço de ' + e.nome}" inputmode="decimal">
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="d-flex gap-2">
            <button type="submit" class="btn btn-success px-4">
                <i class="bi bi-check-lg me-1" aria-hidden="true"></i>Criar produtos
            </button>
            <a th:href="@{/onboarding/pedido}" class="btn btn-outline-secondary">Pular</a>
        </div>
    </form>
</div>
</body>
</html>
```

`<res>/templates/onboarding/pedido.html`:

```html
<!DOCTYPE html>
<html th:replace="~{fragments/layout :: layout(~{::title}, ~{::#pageContent})}"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Primeiro pedido — Sistema Páscoa</title>
</head>
<body>
<div id="pageContent">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0"><i class="bi bi-receipt me-2 text-success" aria-hidden="true"></i>Registre o seu primeiro pedido</h2>
        <span class="text-muted small">Passo 3 de 3</span>
    </div>

    <div th:if="${sucesso}" class="alert alert-success py-2" role="status" th:text="${sucesso}"></div>

    <div class="card mb-3">
        <div class="card-body">
            <p class="mb-2">Cadastre um cliente, escolha os produtos e a data de entrega. É rápido.</p>
            <p class="text-muted small mb-3">
                O custo e a margem de cada produto aparecem quando você preencher a ficha técnica dele.
            </p>
            <a th:href="@{/pedidos/wizard}" class="btn btn-success">
                <i class="bi bi-plus-lg me-1" aria-hidden="true"></i>Criar um pedido
            </a>
        </div>
    </div>

    <form th:action="@{/onboarding/concluir}" method="post">
        <button type="submit" class="btn btn-outline-secondary">Concluir configuração</button>
    </form>
</div>
</body>
</html>
```

- [ ] **Step 5: Aviso no dashboard**

Em `DashboardController`, acrescentar o campo `private final OnboardingService onboardingService;` (import `br.com.seuprojeto.pascoa.onboarding.service.OnboardingService`) e, no método `dashboard(Model model)`, antes do `return`:

```java
        model.addAttribute("onboardingPendente", onboardingService.pendente());
```

Em `<res>/templates/dashboard.html`, logo depois de `<div id="pageContent">`:

```html
    <div th:if="${onboardingPendente}" class="alert alert-success d-flex justify-content-between align-items-center" role="status">
        <span>Continue a configuração da sua loja.</span>
        <a th:href="@{/onboarding}" class="btn btn-success btn-sm">Continuar</a>
    </div>
```

(o teste procura o texto "Continue a configuração", presente no `<span>`.)

- [ ] **Step 6: Rodar, suíte e commit**

Run: `mvn test -pl pascoa-monolith -Dtest='OnboardingServiceTest,OnboardingControllerTest,RolePermissionsTest'` — Expected: todos passando; depois a suíte completa e a de integração — Expected: 0 falhas.

```bash
git add pascoa-monolith/src
git commit -m "feat(onboarding): segmento, produtos de exemplo e primeiro pedido

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Validação em PostgreSQL, ponta a ponta no navegador e docs

**Files:** `docs/05-estado-implementacao.md`, `docs/06-schema-banco.md`, `docs/04-rotas-endpoints.md`, `CLAUDE.md`, `docs/superpowers/specs/2026-10-05-backlog-encomendas-design.md`

- [ ] **Step 1: V20 sobre cópia do dev**

Docker rodando (`docker compose up -d postgres`; se o daemon estiver parado, `open -a Docker` e aguardar `docker info`). Cópia do banco do dev (somente leitura nele) e aplicação na porta 8086:

```bash
docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f02"
docker compose exec -T postgres psql -U postgres -c "CREATE DATABASE pascoa_f02"
docker compose exec -T postgres pg_dump -U postgres pascoa_monolith | docker compose exec -T postgres psql -U postgres -d pascoa_f02 -q
cd pascoa-monolith && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8086 --spring.datasource.url=jdbc:postgresql://localhost:5432/pascoa_f02"
```

Expected no log: `Migrating schema "public" to version "20 - lojas onboarding"` (se o dev já estiver em V20, a cópia só valida) e sem erro de `ddl-auto=validate`. Conferir:

```bash
docker compose exec -T postgres psql -U postgres -d pascoa_f02 -c "SELECT id, nome, segmento, onboarding_concluido FROM lojas ORDER BY id;"
```

Expected: a loja 1 com `onboarding_concluido = t` e `segmento` vazio.

- [ ] **Step 2: Ponta a ponta no navegador (skill `agent-browser`)**

Com a aplicação em `http://localhost:8086` (o 2FA para a loja 1 segue ligado; a loja nova não precisa dele): abrir `/cadastro` anônimo, criar a conta `dona-teste@teste.com` com a loja "Doces da Ana", verificar que cai em `/onboarding/segmento` já logada; escolher "Doces e salgados"; no passo 2 desmarcar um item, mudar um preço e criar; ver o passo 3 e abrir "Criar um pedido" (`/pedidos/wizard`), registrar um pedido com 1 produto criado; voltar ao dashboard e ver o aviso "Continue a configuração"; concluir e ver o aviso sumir; sair e entrar de novo com `DONA-TESTE@TESTE.COM` (maiúsculas) sem passar pelo 2FA. Depois entrar como `admin`/`admin123` e conferir que a loja 1 **não** vê nenhum produto, cliente, categoria nem pedido da loja nova, e que continua passando pelo 2FA. Registrar o que foi observado de verdade; se algum passo não puder ser feito no navegador, dizer qual e por quê.

- [ ] **Step 3: Banco após o cadastro**

```bash
docker compose exec -T postgres psql -U postgres -d pascoa_f02 -c "SELECT l.id, l.nome, l.segmento, l.onboarding_concluido, (SELECT count(*) FROM configuracao_canal c WHERE c.loja_id = l.id) canais, (SELECT count(*) FROM templates_notificacao t WHERE t.loja_id = l.id) templates, (SELECT count(*) FROM produtos p WHERE p.loja_id = l.id) produtos, (SELECT count(*) FROM categorias_produto c WHERE c.loja_id = l.id) categorias FROM lojas l ORDER BY l.id;"
```

Expected: a loja nova com `canais = 3`, `templates = 20`, `onboarding_concluido = t`, produtos e categorias só dela; a loja 1 com os mesmos números de antes.

- [ ] **Step 4: Cadastro duplicado e limite de taxa (manual)**

Repetir o cadastro com o mesmo e-mail (em outra caixa) e confirmar a mensagem "Já existe uma conta com este e-mail"; enviar 6 cadastros seguidos com e-mails diferentes pelo mesmo IP e confirmar o 429 no sexto. Parar a aplicação ao terminar.

- [ ] **Step 5: Suíte completa e docs**

Run a suíte completa e `mvn test -pl pascoa-monolith -Dtest='*IntegrationTest'`; usar os números reais. Atualizar: `docs/05-estado-implementacao.md` (nova seção `## 29. F0.2 — Cadastro e onboarding`: decisões, rotas, V20, conteúdo padrão a revisar, o que foi validado e como, limitações: sem confirmação de e-mail, rate limit em memória, loja órfã sem padrões se o processo cair entre as transações — `LojaPadroesService.criar` é idempotente e pode ser reexecutado; contadores de testes; "próxima V21"), `docs/06-schema-banco.md` (`lojas.segmento`, `lojas.onboarding_concluido`, V20), `docs/04-rotas-endpoints.md` (`/cadastro` público, `/onboarding/**` autenticado), `CLAUDE.md` (rota pública `/cadastro` na lista de públicas, "próxima: V21", "migrations V1–V20"), e marcar o F0.2 como concluído no backlog (`docs/superpowers/specs/2026-10-05-backlog-encomendas-design.md`).

- [ ] **Step 6: Limpeza, commit e push**

```bash
docker compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS pascoa_f02"
git add docs CLAUDE.md
git commit -m "docs: F0.2 concluído (cadastro self-service e onboarding)

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
git push
```

---

## Self-Review

**Cobertura da spec:** cadastro público com e-mail como login (Task 3), loja e dono em transação própria e padrões em transação nova no tenant da loja com compensação (Task 3, apoiada no Task 2), canais desligados e templates neutros do JSON (Task 2), 2FA só na loja da plataforma e login case-insensitive (Task 1), recuperação de senha com e-mail repetido (Task 1), onboarding de 3 passos com `segmento`/`onboarding_concluido` e aviso no dashboard (Tasks 1 e 4), rate limit de 5 por hora por IP (Task 3), exemplos em JSON (Task 4), validação em PostgreSQL e no navegador (Task 5).

**Divergências deliberadas da spec:** (1) o pedido guiado aponta para `/pedidos/wizard`, a rota real (a spec dizia `/pedidos/novo`); (2) o texto padrão de `variaveis` segue o formato do V14, a confirmar na leitura; (3) a "reexecução da criação dos padrões no login do onboarding" que a spec cita como mitigação fica só documentada (o serviço já é idempotente), sem gatilho automático, para não aumentar o escopo.

**Consistência de tipos:** `Loja.isOnboardingConcluido()`/`setOnboardingConcluido(boolean)`, `Loja.setSegmento(Segmento)` usados em Tasks 1, 3 e 4; `UsuarioRepository.existsByLoginIgnoreCase` (Task 1) usado em Task 3; `LojaPadroesService.criar()` (Task 2) usado em Task 3; `OnboardingService.pendente()` (Task 4) usado no `DashboardController`.

**Pontos que dependem de verificação na execução:** (1) o setup de mock de e-mail do `PasswordResetServiceTest` existente (Task 1, Step 1); (2) campos obrigatórios do builder de `ConfiguracaoCanal` (Task 2, Step 3); (3) formato de `variaveis` do V14 (Task 2, Step 2); (4) o corpo de `DashboardController.dashboard` ter um ponto único de `return` onde acrescentar o atributo (Task 4, Step 5).
