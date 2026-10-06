# F0.2 — Cadastro self-service e onboarding de loja

Data: 2026-10-06 · Origem: [backlog](2026-10-05-backlog-encomendas-design.md), item F0.2 · Depende de F0.1 (multi-tenant) e F0.3 (categorias e unidades de doce/salgado), já prontos.

## Objetivo

Uma loja nova se cadastra sozinha, entra logada na hora e é levada por 3 passos curtos até o primeiro pedido, sem ninguém da plataforma intervir.

## Decisões

| Tema | Decisão |
|---|---|
| Entrada | Cadastro público imediato em `/cadastro`; sem confirmação de e-mail; sem aprovação manual |
| Login | O e-mail (em minúsculas) vira o `usuarios.login` do dono. O login `admin` da loja 1 e os logins antigos continuam valendo |
| Papel do dono | `ADMIN` da própria loja (os papéis Dono/Equipe chegam no F0.4) |
| 2FA | Obrigatório só para a loja da plataforma (`Loja.PLATAFORMA_ID`), até o F0.4 mover o 2FA para as configurações avançadas |
| Padrões da loja nova | Canais (e-mail, WhatsApp, SMS) inativos e em modo de teste, mais templates de texto neutro, lidos de um arquivo de recursos versionado |
| Categorias | Nenhuma criada no cadastro; vêm do onboarding, por segmento. Substitui a nota do F0.3 de "criar as 6 categorias padrão", que era vocabulário de ovo |
| Onboarding | 3 passos, todos puláveis: segmento, produtos de exemplo, primeiro pedido |
| Exemplos | Arquivo de recursos versionado, ~8 produtos por segmento (nome, categoria, unidade de venda, preço sugerido); sem ficha técnica |
| Proteção contra abuso | `RateLimitFilter` ganha a categoria de cadastro (5 POSTs por hora por IP) |

## Componentes

### Banco — `V20__lojas_onboarding.sql`

```sql
ALTER TABLE lojas ADD COLUMN segmento VARCHAR(10);
ALTER TABLE lojas ADD COLUMN onboarding_concluido BOOLEAN NOT NULL DEFAULT TRUE;
```

`lojas` é global (sem `loja_id`). O DEFAULT TRUE mantém a loja 1 sem aviso de onboarding; as lojas novas são criadas com `FALSE` pelo serviço. `segmento` aceita `DOCES`, `SALGADOS` ou `AMBOS` (nulo até o passo 1).

### Cadastro

- `seguranca/controller/CadastroController` em `/cadastro` (GET formulário, POST cria); `seguranca/dto/CadastroForm` (`nomeLoja`, `nomeDono`, `email`, `senha`, `confirmacaoSenha`) com Bean Validation (e-mail válido, senha mínima de 8 caracteres, confirmação igual). `/cadastro` e `/cadastro/**` ficam públicos no `SecurityConfig`.
- Template `cadastro/form.html` no layout de autenticação (`fragments/layout-auth`), com o mesmo padrão de acessibilidade das telas de login.
- `seguranca/service/CadastroLojaService.cadastrar(CadastroForm)`, sem `@Transactional` no método, com `TransactionTemplate` explícito (a sessão do Hibernate fixa o tenant ao abrir, então a segunda fase precisa de transação nova):
  1. Transação 1: valida e-mail livre (`existsByLoginIgnoreCase`), cria `Loja(nome, onboardingConcluido=false)` e `Usuario(role=ADMIN, login=email, email=email, senha=BCrypt, lojaId=nova)`.
  2. Transação 2, `REQUIRES_NEW`, dentro de `TenantContext.executar(lojaId, ...)`: `LojaPadroesService.criar()`.
  3. Se a transação 2 falhar: compensação remove o usuário e a loja e relança a exceção. O usuário vê um erro genérico e pode tentar de novo.
- `seguranca/service/LojaPadroesService`: lê `resources/defaults/notificacoes.json` e cria, no tenant atual, uma `ConfiguracaoCanal` por canal (`ativo=false`, `testMode=true`) e os `TemplateNotificacao` padrão (um por evento e canal).
- Depois do sucesso, o controller autentica o dono: monta `UsuarioPrincipal`, cria o `Authentication`, salva com `SecurityContextRepository.saveContext(context, request, response)` (regra do projeto; nunca `session.setAttribute`) e redireciona para `/onboarding`. Esse caminho não passa pelo handler de 2FA.
- E-mail repetido: mensagem de campo ("Já existe uma conta com este e-mail"); não revela a qual loja pertence.

### Login e 2FA

- `UsuarioService.loadUserByUsername`: tenta o login exato; se não achar, tenta em minúsculas. Cadastro grava sempre em minúsculas.
- `TwoFactorAuthenticationSuccessHandler`: só exige 2FA quando o usuário é ADMIN **e** `usuario.getLojaId() == Loja.PLATAFORMA_ID`. Dono de loja nova vai direto ao `/dashboard`.
- Tela de login: o rótulo do campo passa a "E-mail ou usuário", com `autocomplete="username"`.

### Recuperação de senha

`PasswordResetService.solicitar` hoje usa `findByEmail`, que lança `IncorrectResultSizeDataAccessException` se o e-mail se repete entre lojas. Passa a: buscar por login (que, para lojas novas, é o e-mail); se não achar, buscar por e-mail e só agir quando houver exatamente um usuário ativo; com zero ou vários resultados, não faz nada. A resposta ao usuário continua idêntica em todos os casos (anti-enumeração).

### Onboarding

- `onboarding/controller/OnboardingController` em `/onboarding` (autenticado). O estado fica em `lojas`: `segmento` e `onboarding_concluido`.
  - Passo 1 (`GET/POST /onboarding/segmento`): escolhe `DOCES`, `SALGADOS` ou `AMBOS`.
  - Passo 2 (`GET/POST /onboarding/produtos`): lista os exemplos do segmento (do arquivo `resources/defaults/produtos-exemplo.json`); cada item vem marcado, com preço editável. Ao salvar, cria as categorias (por nome, sem duplicar) e os produtos marcados, com a unidade de venda do exemplo. Tudo no tenant da loja.
  - Passo 3 (`GET /onboarding/pedido`): tela curta com o link para `/pedidos/novo` e um aviso de que o custo e a margem aparecem quando a ficha técnica for preenchida.
  - Pular qualquer passo é um link "Pular" que avança. `POST /onboarding/concluir` marca `onboarding_concluido=true` e leva ao dashboard.
- O dashboard mostra um aviso "Continue a configuração da sua loja" enquanto `onboarding_concluido` for falso. O dado vem do `Loja` do usuário logado (por `LojaRepository`, que é global).
- `onboarding/service/OnboardingService` concentra a lógica de criar categorias e produtos; os controllers só chamam o service.

### Arquivos de recursos

`resources/defaults/produtos-exemplo.json`: lista de `{segmento, categoria, nome, unidadeVenda, precoSugerido}`, ~8 itens para `DOCES` (ex.: brigadeiro por cento, bolo por kg, docinhos por dúzia) e ~8 para `SALGADOS` (ex.: coxinha por cento, empada por cento, esfiha por dúzia). `AMBOS` reúne os dois.

`resources/defaults/notificacoes.json`: os 10 eventos de `EventoNotificacao` para os canais EMAIL e WHATSAPP, com assunto, corpo e variáveis (`{nome}`, `{numeroPedido}`, `{dataEntrega}`, `{link}`, `{valor}`), em português neutro, sem "Páscoa". Canal SMS usa só a configuração (sem template próprio; o SMS é fallback do WhatsApp).

## Fluxo

1. Visitante abre `/cadastro`, preenche e envia.
2. O serviço cria loja e dono, cria os padrões no tenant novo, autentica e redireciona.
3. `/onboarding`: segmento, exemplos, primeiro pedido, cada um com "Pular".
4. Dashboard sem aviso depois de concluir.

## Erros

- Validação de campo: reexibe o formulário com as mensagens.
- E-mail já usado: erro de campo, sem identificar a loja.
- Falha ao criar os padrões: a compensação remove loja e usuário; erro genérico ("Não foi possível criar a conta, tente novamente"). A falha é registrada em log com a causa.
- Limite de taxa excedido: resposta 429 padrão do `RateLimitFilter`.

## Testes

- `CadastroLojaServiceTest`: cria loja, admin (login em minúsculas, senha com hash), canais inativos em modo de teste e os templates padrão, todos com o `loja_id` da loja nova e nenhum na loja 1.
- E-mail repetido (mesmo com maiúsculas diferentes) é rejeitado e nada é criado.
- Falha forçada na fase 2 (por exemplo, arquivo de padrões inválido injetado) desfaz loja e usuário.
- `CadastroControllerTest` (MockMvc): GET `/cadastro` anônimo retorna 200; POST válido redireciona para `/onboarding` e a sessão já enxerga o dashboard da loja nova; POST inválido volta ao formulário.
- Login com o e-mail em maiúsculas funciona; o dono de loja nova não passa pelo 2FA e o admin da loja 1 continua passando.
- `PasswordResetServiceTest`: e-mail repetido em duas lojas não lança e não envia; e-mail único envia; login igual ao e-mail funciona.
- `OnboardingServiceTest`: cria categorias sem duplicar e só marcadas; os produtos têm a unidade de venda do exemplo; tudo isolado na loja nova; `segmento` e `onboarding_concluido` atualizam.
- `OnboardingControllerTest`: os 3 passos respondem 200; "Pular" avança; o dashboard mostra o aviso até concluir.
- `RateLimitFilterTest`: o sexto POST em `/cadastro` do mesmo IP na mesma hora retorna 429.
- Validação em PostgreSQL: V20 em cópia do banco do dev; cadastro de uma loja real de ponta a ponta no navegador (`agent-browser`), com a loja 1 intacta e a loja nova sem dados da outra.

## Fora do escopo

Confirmação de e-mail, papéis Dono/Equipe e 2FA opcional (F0.4), termos de uso e consentimento LGPD da loja, upload de imagens por loja (F0.6), planos e cobrança (F2.5), vitrine pública por loja (F2.3; o `/catalogo` segue preso à loja 1).

## Riscos

- **Cadastro aberto na internet:** sem confirmação de e-mail, qualquer um cria lojas. O rate limit por IP é a única barreira; é uma proteção em memória por instância, que basta para o piloto. Revisitar com confirmação de e-mail se aparecer abuso.
- **Compensação da fase 2:** se o processo cair entre as duas transações, sobra uma loja sem padrões. O dono ainda entra (o usuário existe), mas sem canais nem templates; a criação dos padrões é idempotente (`LojaPadroesService` só cria o que falta) e pode ser reexecutada no login do onboarding. Tratar isso no plano.
- **Conteúdo a escrever:** os 20 textos padrão de notificação e as ~16 linhas de produtos de exemplo precisam de revisão de quem conhece o negócio antes de ir ao ar.
- **Dois mundos de login:** a loja 1 usa logins livres (`admin`, `financeiro`...) e as novas usam e-mail. Convivem sem conflito porque `login` é único globalmente, mas um e-mail novo pode colidir com um login antigo igual (ex.: `admin` nunca é e-mail válido, então não colide na prática).
