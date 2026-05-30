# Bugfix — Loop de Login após Migração para Microsserviços (v5)

> **Sintoma reportado:** Após a migração para a arquitetura v5 (Strangler Fig com API Gateway), o login não funciona — o usuário insere `admin / admin123`, clica em "Entrar" e é devolvido para a tela de login, sem qualquer mensagem de erro. As páginas internas (`/dashboard`, `/2fa/setup`) também não abrem.
>
> **Detectado em:** 2026-05-30
> **Resolvido em:** 2026-05-30
> **Severidade:** 🔴 Crítica (sistema inacessível via gateway)

---

## 1. Sintomas observados

- POST `/login` retorna 302, mas o redirect leva para `/login` (ou para a tela de login do `/2fa/setup`).
- A URL no navegador às vezes "pula" silenciosamente de `http://localhost:8090/...` para `http://localhost:8080/...`.
- Acesso direto ao monólito em `localhost:8080` funciona normalmente — o problema só aparece via gateway (`localhost:8090`).
- O `JSESSIONID` aparece nos cookies do navegador, mas não é enviado na requisição seguinte do redirect.

---

## 2. Causa raiz

O monólito (`pascoa-monolith`, Tomcat embedded, porta 8080) está atrás do **`pascoa-api-gateway`** (Spring Cloud Gateway, porta 8090, rota catch-all `Path=/**` → `http://localhost:8080`).

Quando o `TwoFactorAuthenticationSuccessHandler` (ou qualquer outro `sendRedirect`) emite:

```java
response.sendRedirect("/2fa/setup");
```

o Tomcat **transforma o caminho relativo em URL absoluta usando o `Host` que ele próprio enxerga**, que é `localhost:8080` (o backend), não `localhost:8090` (o gateway). O navegador então recebe:

```
HTTP/1.1 302 Found
Location: http://localhost:8080/2fa/setup
Set-Cookie: JSESSIONID=ABC...; Path=/; HttpOnly
```

Resultado em cadeia:

1. O navegador deixa o gateway e vai direto para `localhost:8080`.
2. O cookie `JSESSIONID` foi **fixado pelo navegador no host onde a resposta chegou** (origem do gateway). Como `localhost:8080` é tratado como host diferente, o cookie pode não ser enviado em ambientes proxy/HTTPS reais (e mesmo em dev local, o salto fora do gateway é confuso e quebra o fluxo esperado).
3. Pior: como `TwoFactorAuthenticationSuccessHandler` **remove o `SecurityContext` da sessão** propositalmente (para forçar o 2FA), a nova requisição sem `JSESSIONID` válido cai em `TwoFactorController.setupForm`, que faz:

   ```java
   if (usuarioPendente(session) == null) return "redirect:/login";
   ```

4. Volta para `/login`. Loop fechado — o usuário "nunca entra".

O `DashboardController` para usuários **não-ADMIN** sofre o mesmo problema: o handler faz `sendRedirect("/dashboard")`, escapa para `:8080`, perde a sessão e cai em `.anyRequest().authenticated()` → redirect para `/login`.

---

## 3. Por que só apareceu agora

No **v3/v4 (monólito puro)** o usuário acessava diretamente `localhost:8080`. O `Host` do Tomcat coincidia com o `Host` do navegador, então `sendRedirect` produzia URLs corretas e o cookie ficava no mesmo host.

A introdução do **`pascoa-api-gateway` (v5)** quebrou essa equivalência. Spring Cloud Gateway envia os headers `X-Forwarded-Host`, `X-Forwarded-Port` e `X-Forwarded-Proto`, **mas o Tomcat só os respeita quando `server.forward-headers-strategy` está configurado**. Por padrão essa propriedade é `none`, então o Tomcat ignora os headers e usa seu próprio host/porta.

---

## 4. Correção aplicada

### 4.1 `pascoa-monolith/src/main/resources/application.properties`

Adicionado:

```properties
# --- Reverse proxy / API Gateway (v5 microsserviços) ---
server.forward-headers-strategy=framework
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.http-only=true
```

| Propriedade | Efeito |
|-------------|--------|
| `server.forward-headers-strategy=framework` | Faz o Spring/Tomcat usar `X-Forwarded-*` para resolver `request.getServerName()`/`getServerPort()`/`getScheme()`. Como consequência, `sendRedirect("/x")` gera URL com host:porta do **gateway**, não do backend. |
| `same-site=lax` | Mantém o cookie de sessão no 302 do POST → GET (necessário em browsers modernos que tratam reverse proxy como cross-site em alguns cenários). |
| `http-only=true` | Hardening — cookie de sessão inacessível ao JavaScript (sem efeito no bug, mas é boa prática reforçada agora). |

### 4.2 `pascoa-config-server/src/main/resources/configs/pascoa-monolith.yml`

Mesmas propriedades replicadas no config-server, para o caso de o monólito ser inicializado com `spring.profiles.active` apontando para o config remoto:

```yaml
server:
  port: 8080
  forward-headers-strategy: framework
  servlet:
    session:
      cookie:
        same-site: lax
        http-only: true
```

### 4.3 `pascoa-api-gateway/src/main/resources/application.yml`

`X-Forwarded-*` habilitado explicitamente (defaults já são `true` no Spring Cloud Gateway, mas tornamos explícito para evitar surpresa em futuras versões):

```yaml
spring:
  cloud:
    gateway:
      x-forwarded:
        enabled: true
        for-enabled: true
        host-enabled: true
        port-enabled: true
        proto-enabled: true
        prefix-enabled: true
```

---

## 5. Como testar a correção

1. Subir tudo: `./start-all.sh`
2. Acessar **pelo gateway**: http://localhost:8090
3. Login: `admin` / `admin123`
4. **Esperado:** redirecionar para `http://localhost:8090/2fa/setup` (não para `:8080`).
5. Concluir o 2FA → cair em `http://localhost:8090/dashboard`.
6. Verificar que o cookie `JSESSIONID` está fixado em `localhost:8090` (DevTools → Application → Cookies).

Verificação por logs (gateway):

```
DEBUG br.com.seuprojeto.pascoa.gateway : GET /dashboard 200 45ms
```

Sem mais loop para `/login`.

---

## 6. Outras páginas com o mesmo padrão

Toda chamada a `response.sendRedirect("/algo")` ou `return "redirect:/algo"` em um `@Controller` agora produz URL absoluta correta. Isto cobre, entre outras:

- `TwoFactorController.completarAutenticacao()` → `/dashboard`
- `TwoFactorController.setupConfirmar()` → `/dashboard`
- `TwoFactorAuthenticationSuccessHandler.onAuthenticationSuccess()` → `/2fa/setup`, `/2fa/verificar`, `/dashboard`
- `AuthController.forgot/reset-password` → `/login`
- Spring Security `formLogin().failureUrl("/login?erro")`
- Spring Security `logout().logoutSuccessUrl("/login?logout")`
- `MaintenanceFilter.doFilterInternal()` → `/manutencao`

Nenhuma dessas precisou ser alterada — todas passam a funcionar corretamente apenas com a configuração de forward-headers.

---

## 7. Lições aprendidas

1. **Reverse proxy + Spring Boot ⇒ sempre setar `server.forward-headers-strategy`.** É um dos itens obrigatórios do checklist quando se coloca um Spring Boot atrás de qualquer gateway/load balancer (Nginx, Cloud Gateway, AWS ALB, etc.).
2. **`sendRedirect` com path relativo é traiçoeiro atrás de proxy.** A alternativa "explícita" seria construir URLs absolutas via `ServletUriComponentsBuilder.fromCurrentContextPath()` — mas isso só funciona corretamente *também* depois de habilitar forward-headers. Então a raiz é a mesma.
3. **Cookies de sessão por porta:** browsers tratam `localhost:8080` e `localhost:8090` como o mesmo host para cookies (cookies não diferenciam por porta), mas o "salto" de URL na barra de endereço já é suficiente para confundir o usuário e quebrar a UX.
4. **Validar manualmente cada redirect crítico após introduzir proxy** — não basta só `curl` no backend; o teste tem que passar **pelo gateway**.

---

## 8. Arquivos alterados

| Arquivo | Alteração |
|---------|-----------|
| `pascoa-monolith/src/main/resources/application.properties` | + 4 linhas (forward-headers + cookie SameSite/HttpOnly) |
| `pascoa-config-server/src/main/resources/configs/pascoa-monolith.yml` | + 6 linhas (mesmas configs em YAML) |
| `pascoa-api-gateway/src/main/resources/application.yml` | + 8 linhas (X-Forwarded explícito) |
| `pascoa-config-server/pom.xml` | + dependência `spring-security-test` (test scope) — bug B12 |
| `start-all.sh` | `spring-boot:run` passa a usar `-Dmaven.test.skip=true`; logs do serviço travado são despejados no console; pidfile órfão é removido em caso de falha — bug B12 |
| `docs/10-bugfix-login-loop-gateway.md` | Novo — esta documentação |
| `docs/05-estado-implementacao.md` | Bugs B11 e B12 registrados na seção 16 |
| `docs/00-indice.md` | Referência ao novo documento |

---

## 9. Bug B12 — `./start-all.sh` travava em "Iniciando pascoa-config-server (porta 8888)..." — RESOLVIDO 2026-05-30

**Sintoma:** Após a migração v5, executar `./start-all.sh` deixava o script preso em `[INFO] Iniciando pascoa-config-server (porta 8888)...` por 180s, depois `[ERR] pascoa-config-server não respondeu em 180s (porta 8888)`. Nenhuma indicação clara da causa no console.

**Causa raiz:** Dois problemas combinados.

1. `pascoa-config-server/src/test/java/.../ConfigServerSmokeTest.java` importa `SecurityMockMvcRequestPostProcessors.httpBasic` (do `spring-security-test`), mas o `pom.xml` do config-server não declarava essa dependência (apenas `spring-boot-starter-test`). Compilação dos testes falhava:
   ```
   package org.springframework.security.test.web.servlet.request does not exist
   ```
2. O `start_service()` do `start-all.sh` chamava `mvn spring-boot:run` **sem** `-Dmaven.test.skip=true`. O ciclo do plugin Spring Boot dispara `test-compile` antes de iniciar a JVM. O erro acima impedia o JAR de subir; a porta 8888 nunca abria; `wait_port` estourava o timeout.

Como apenas a saída ia para `logs/pascoa-config-server.log` e o script não exibia o conteúdo, o usuário só via o "travou esperando porta" — daí a impressão de boot lento ou deadlock.

**Correção:**
- `pascoa-config-server/pom.xml`: adicionada dependência `spring-security-test` (test scope).
- `start-all.sh / start_service()`:
  - `nohup mvn -q -Dmaven.test.skip=true spring-boot:run ...` — pular compilação/execução de testes (o `build_all` já fez a compilação principal).
  - Em caso de timeout do `wait_port`, despejar `tail -20` do log no console e remover o pidfile órfão.

**Por que só apareceu agora:** o teste foi adicionado durante a migração v5; antes só existia o starter de teste padrão. O bug ficou latente porque `mvn install` no `build_all()` usa `-Dmaven.test.skip=true` e passava sem erro — só o `spring-boot:run` (sem o flag) explodia.

**Teste pós-fix:** `./start-all.sh stop && ./start-all.sh` — config-server sobe em <30s, demais serviços seguem na sequência.

---

## 10. Bug B13 — Após ativar 2FA, volta para `/login` em vez de ir para `/dashboard` — RESOLVIDO 2026-05-30

**Sintoma:** Mesmo após as correções de forward-headers (B11), o usuário entrava em `/login` com `admin/admin123`, era levado para `/2fa/setup`, escaneava o QR, digitava o código, clicava em "Ativar 2FA" e — em vez de ir para `/dashboard` — voltava para a tela de login. O Tomcat enviava 302 para `/dashboard`, o browser seguia, mas o monólito tratava o request como não-autenticado e redirecionava para `/login`.

**Causa raiz:** `TwoFactorController.completarAutenticacao()` populava o `SecurityContext` usando apenas o padrão antigo do Spring Security 5:

```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(auth);
SecurityContextHolder.setContext(context);
session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
```

No **Spring Security 6** o `SecurityContextPersistenceFilter` foi **removido**. A persistência do contexto agora passa **obrigatoriamente** pelo `SecurityContextRepository` (`DelegatingSecurityContextRepository` por padrão, que combina `RequestAttributeSecurityContextRepository` + `HttpSessionSecurityContextRepository`). Setar o atributo de sessão "à mão":

- não publica o contexto no `RequestAttributeSecurityContextRepository`, que tem precedência no `Delegating`;
- não dispara os listeners internos que invalidam o `DeferredSecurityContext` em cache;
- e em alguns fluxos o `loadContext()` do próximo request acaba retornando o contexto "antigo" (vazio) que estava em cache de request attribute pendente.

Resultado: o GET `/dashboard` que vem logo depois roda como **anônimo**, cai em `.anyRequest().authenticated()` do `SecurityFilterChain` e é redirecionado para `/login`.

**Correção:**

1. **`SecurityConfig.java`** — expor o `SecurityContextRepository` como **bean** (Spring Security só o registra como *shared object* internamente) e amarrar o `SecurityFilterChain` ao mesmo bean, garantindo que `saveContext()` (chamado pelo controller) e `loadContext()` (chamado pelo filtro no próximo request) usem **a mesma instância**:

   ```java
   @Bean
   public SecurityContextRepository securityContextRepository() {
       return new DelegatingSecurityContextRepository(
           new RequestAttributeSecurityContextRepository(),
           new HttpSessionSecurityContextRepository()
       );
   }

   @Bean
   public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                  TwoFactorAuthenticationSuccessHandler twoFactorHandler,
                                                  SecurityContextRepository securityContextRepository) throws Exception {
       http
           .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
           // ...
   }
   ```

2. **`TwoFactorController.java`** — injetar o repositório e chamar `saveContext()` explicitamente em `completarAutenticacao()`:

   ```java
   private final SecurityContextRepository securityContextRepository;

   private String completarAutenticacao(HttpSession session,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
       Authentication auth = (Authentication) session.getAttribute(PENDING_2FA_AUTH);
       if (auth == null) return "redirect:/login";

       SecurityContext context = SecurityContextHolder.createEmptyContext();
       context.setAuthentication(auth);
       SecurityContextHolder.setContext(context);
       securityContextRepository.saveContext(context, request, response);   // ← chave do fix
       session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context); // defensivo

       session.removeAttribute(PENDING_2FA_AUTH);
       session.removeAttribute(PENDING_2FA_USER_ID);
       return "redirect:/dashboard";
   }
   ```

   Também adicionei `log.info` na conclusão e `log.warn` quando `PENDING_2FA_AUTH` está ausente, para diagnóstico futuro.

**Por que só apareceu agora:**
O fluxo de 2FA foi escrito no v3/v4 com Spring Security 5, onde `session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, ctx)` era o caminho recomendado e o `SecurityContextPersistenceFilter` cuidava do resto. A subida para Spring Boot 3.3.4 / Spring Security 6 quebrou esse contrato silenciosamente — funcionava em alguns cenários (cookies/sessão "limpos" no monólito direto), mas falhava de forma reproduzível atrás do gateway por causa do cache de request attribute. Sem a documentação acima, esse comportamento parece "tela de login aleatória após 2FA".

**Como testar:**

1. `./start-all.sh stop && ./start-all.sh` (precisa recompilar — `SecurityConfig.java` e `TwoFactorController.java` foram alterados).
2. Acessar **http://localhost:8090** (via gateway), login `admin / admin123`.
3. Cair em `/2fa/setup`, escanear QR com Google Authenticator/Authy, digitar o código de 6 dígitos.
4. **Esperado:** vai para `http://localhost:8090/dashboard` com o usuário logado.
5. Conferir no log do monólito: `[2FA] Autenticação concluída para admin — redirecionando para /dashboard`.

**Arquivos alterados nesta rodada:**

| Arquivo | Alteração |
|---------|-----------|
| `pascoa-monolith/.../config/SecurityConfig.java` | + bean `SecurityContextRepository`; amarra ao `SecurityFilterChain` via `.securityContext(...)` |
| `pascoa-monolith/.../seguranca/controller/TwoFactorController.java` | injeta `SecurityContextRepository`; usa `saveContext()` em `completarAutenticacao()`; logs de diagnóstico |
