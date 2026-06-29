package br.com.seuprojeto.pascoa.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Tela de login do ambiente FoodFlow servida pelo próprio gateway.
 *
 * <p>É o destino do redirect quando o monólito (UI legada) está fora — o
 * {@link FallbackController} devolve 302 para {@code /login.html} sem expor
 * que a UI legada existe.</p>
 *
 * <p>A página é HTML estático inline (Mono&lt;String&gt;) — segue o mesmo
 * padrão do FallbackController para evitar depender de ResourceHandler do
 * WebFlux convivendo com o catch-all do Gateway.</p>
 *
 * <p>O formulário POSTa em {@code /api/auth/login} (roteado para o
 * {@code auth-service} na porta 8081). O auth-service emite os cookies
 * {@code access_token}/{@code refresh_token} httpOnly. Após login a página
 * redireciona o usuário para {@code /onboarding.html} — única UI completa
 * do ambiente moderno hoje.</p>
 */
@RestController
@RequestMapping("/login.html")
public class LoginPageController {

    @GetMapping
    public Mono<ResponseEntity<String>> loginPage() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.valueOf("text/html;charset=UTF-8"))
                .body(PAGE_HTML));
    }

    private static final String PAGE_HTML = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Entrar — FoodFlow</title>
              <style>
                :root {
                  --primary: #c2185b;
                  --primary-dark: #8e0038;
                  --bg: linear-gradient(135deg, #fce4ec 0%, #fff 100%);
                  --text: #3a2b31;
                  --muted: #8a7680;
                  --border: #e1d4d8;
                  --error-bg: #ffebee;
                  --error-fg: #b71c1c;
                  --success-bg: #e8f5e9;
                  --success-fg: #1b5e20;
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                  background: var(--bg); color: var(--text); padding: 1.5rem;
                }
                .card {
                  background: #fff; max-width: 420px; width: 100%; border-radius: 16px; padding: 2.5rem 2rem;
                  box-shadow: 0 12px 40px rgba(194, 24, 91, .12); border-top: 5px solid var(--primary);
                }
                .brand { text-align: center; margin-bottom: 1.5rem; }
                .brand .logo { font-size: 2.25rem; line-height: 1; margin-bottom: .25rem; }
                .brand h1 { font-size: 1.5rem; margin: 0; color: var(--primary); font-weight: 700; }
                .brand p { margin: .25rem 0 0; color: var(--muted); font-size: .9rem; }
                label { display: block; font-weight: 600; margin: 1rem 0 .3rem; font-size: .9rem; }
                input {
                  width: 100%; padding: .65rem .8rem; border: 1px solid var(--border);
                  border-radius: 8px; font-size: 1rem; transition: border-color .15s, box-shadow .15s;
                }
                input:focus {
                  outline: none; border-color: var(--primary);
                  box-shadow: 0 0 0 3px rgba(194, 24, 91, .15);
                }
                button {
                  width: 100%; margin-top: 1.5rem; background: var(--primary); color: #fff;
                  border: 0; padding: .8rem; border-radius: 8px; font-size: 1rem; font-weight: 600;
                  cursor: pointer; transition: background .15s;
                }
                button:hover:not(:disabled) { background: var(--primary-dark); }
                button:disabled { background: #aaa; cursor: not-allowed; }
                .msg {
                  margin-top: 1rem; padding: .75rem 1rem; border-radius: 8px; font-size: .9rem;
                  display: none;
                }
                .msg.error   { background: var(--error-bg);   color: var(--error-fg);   display: block; }
                .msg.success { background: var(--success-bg); color: var(--success-fg); display: block; }
                .footer { margin-top: 1.5rem; text-align: center; font-size: .85rem; color: var(--muted); }
                .footer a { color: var(--primary); text-decoration: none; font-weight: 600; }
                .footer a:hover { text-decoration: underline; }
              </style>
            </head>
            <body>
              <main class="card">
                <div class="brand">
                  <div class="logo" aria-hidden="true">🍰</div>
                  <h1>FoodFlow</h1>
                  <p>Entre na sua conta</p>
                </div>

                <form id="loginForm" autocomplete="on">
                  <label for="login">Usuário ou e-mail</label>
                  <input id="login" name="login" type="text" required autocomplete="username" autofocus>

                  <label for="senha">Senha</label>
                  <input id="senha" name="senha" type="password" required autocomplete="current-password">

                  <label for="totp">Código 2FA <span style="font-weight:400;color:var(--muted)">(se ativado)</span></label>
                  <input id="totp" name="totpCodigo" type="text" inputmode="numeric" pattern="[0-9]*" maxlength="6" autocomplete="one-time-code">

                  <button id="btn" type="submit">Entrar</button>
                  <div id="msg" class="msg" role="status" aria-live="polite"></div>
                </form>

                <div class="footer">
                  Ainda não tem conta? <a href="/onboarding.html">Criar minha empresa</a>
                </div>
              </main>

              <script>
                const form = document.getElementById('loginForm');
                const btn  = document.getElementById('btn');
                const msg  = document.getElementById('msg');

                form.addEventListener('submit', async (e) => {
                  e.preventDefault();
                  msg.className = 'msg'; msg.textContent = '';
                  btn.disabled = true; btn.textContent = 'Entrando…';

                  const payload = {
                    login: form.login.value.trim(),
                    senha: form.senha.value,
                    totpCodigo: form.totp.value ? Number(form.totp.value) : null
                  };

                  try {
                    const res = await fetch('/api/auth/login', {
                      method: 'POST',
                      credentials: 'include',
                      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                      body: JSON.stringify(payload)
                    });

                    if (res.ok) {
                      msg.className = 'msg success';
                      msg.textContent = 'Login realizado. Redirecionando…';
                      window.location.href = '/onboarding.html';
                      return;
                    }

                    let detail = 'Falha no login.';
                    try {
                      const body = await res.json();
                      detail = body.error || body.message || detail;
                    } catch (_) { /* corpo não-JSON */ }
                    msg.className = 'msg error';
                    msg.textContent = detail;
                  } catch (err) {
                    msg.className = 'msg error';
                    msg.textContent = 'Não foi possível conectar ao serviço de autenticação.';
                  } finally {
                    btn.disabled = false; btn.textContent = 'Entrar';
                  }
                });
              </script>
            </body>
            </html>
            """;
}
