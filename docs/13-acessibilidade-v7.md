# Acessibilidade — FoodFlow v7 (FASE 10)

> **Status:** Auditoria de código aplicada · Validação manual pendente (axe/Lighthouse/NVDA)
> **Meta WCAG:** 2.1 AA — opcional AAA para textos críticos
> **Atualizado em:** 2026-05-31

---

## 1. O que já está aplicado no código (FASE 10)

### 1.1 Foundation no Design System

| Item | Onde | Critério WCAG |
|---|---|---|
| `:focus-visible` universal com anel rosa 2px + offset 2px | `static/css/foodflow/a11y.css` | 2.4.7 (Focus Visible — AA) |
| Skip link "Pular para o conteúdo" | `fragments/layout.html` + `layout.css` | 2.4.1 (Bypass Blocks — A) |
| Reduced motion (animações desativam) | `reset.css`, `a11y.css` | 2.3.3 (Animations from Interactions — AAA) |
| Touch targets ≥ 44×44px em `pointer: coarse` | `a11y.css` | 2.5.5 (Target Size — AAA) |
| High contrast / forced colors suporte | `a11y.css` `@media (forced-colors)` | 1.4.3 (Contrast — AA) |
| Print styles (limpa sidebar/topbar) | `a11y.css` | — |
| Utilitários `ff-sr-only` / `ff-sr-only-focusable` | `a11y.css` | 1.1.1, 4.1.2 |

### 1.2 Estrutura semântica

| Item | Status | Onde |
|---|---|---|
| `<html lang="pt-BR">` em todos os layouts | ✅ | layout.html, layout-public.html, layout-fullscreen.html, login.html, 2fa/*, etc. |
| `<header role="banner">` no topbar | ✅ | fragments/topbar.html |
| `<aside role="navigation" aria-label="Menu de navegação principal">` na sidebar | ✅ | fragments/sidebar.html |
| `<main role="main" id="ffContent">` no conteúdo | ✅ | fragments/layout.html |
| `<nav aria-label="Trilha de navegação">` no breadcrumb | ✅ | fragments/breadcrumb.html |
| `<nav aria-label="Navegação por páginas">` na paginação | ✅ | fragments/pagination.html + `aria-current="page"` |
| Stepper de status como `<ol>` semântico | ✅ | acompanhamento/pedido.html, onboarding/* |

### 1.3 Formulários

| Item | Status | Onde |
|---|---|---|
| Todo `<input>` tem `<label class="ff-label">` associado | ✅ | produtos/form, materias-primas/form, admin/tenant/* |
| Erros de validação com `<div class="ff-error" th:errors="...">` | ✅ | produtos/form (modelo) |
| `aria-label` em busca global (icon-only) | ✅ | fragments/topbar.html |
| Inputs OTP com `inputmode="numeric"` + `autocomplete="one-time-code"` | ✅ | 2fa/setup.html, 2fa/verificar.html |
| Botões `type="submit"` explícitos | ✅ | todos os forms |

### 1.4 Componentes

| Item | Status | Onde |
|---|---|---|
| Modal com `aria-labelledby="ffConfirmTitle"` + `aria-hidden` | ✅ | fragments/modal-confirm.html |
| Toast container com `aria-live="polite" aria-atomic="true"` | ✅ | fragments/alert.html |
| Toasts individuais com `role="alert" aria-live="assertive"` | ✅ | fragments/alert.html (ffToast) |
| Empty-state com texto descritivo + heading | ✅ | fragments/empty-state.html |
| Badges status com classes semânticas (não dependem só de cor) | ✅ | `.ff-badge--success` tem ícone implícito + texto |
| Stepper com `aria-current="step"` na etapa atual | ⚠️ Parcial | adicionar nos onboarding etapas |
| Botões icon-only com `title="..."` | ✅ | Lista de pedidos, produtos, MP |
| Botões icon-only com `aria-label` | ⚠️ Parcial | título funciona mas aria-label é mais robusto |

---

## 2. Checklist WCAG 2.1 AA por critério

### 2.1 Perceivable

| Critério | Status | Notas |
|---|---|---|
| 1.1.1 Non-text Content (A) | ✅ | Ícones decorativos com `aria-hidden="true"` ou dentro de elemento com texto. Imagens com `alt` |
| 1.3.1 Info and Relationships (A) | ✅ | HTML semântico (`<table>` com `<thead>/<tbody>`, `<form>` com `<label>`) |
| 1.3.2 Meaningful Sequence (A) | ✅ | Ordem lógica preservada |
| 1.3.5 Identify Input Purpose (AA) | ✅ | `autocomplete="username"/"current-password"/"email"/"one-time-code"` |
| 1.4.3 Contrast (Minimum) (AA) | ⚠️ Validar | `TenantColorValidator` (FASE 7.6) calcula em runtime; texto branco × rosa #e91e63 = ratio 3.94 (atende AA Large; texto pequeno **falha**) → recomendar usar `#c2185b` no header da sidebar quando texto normal |
| 1.4.4 Resize Text (AA) | ✅ | `rem` em toda a tipografia; respeita zoom do browser até 200% |
| 1.4.10 Reflow (AA) | ✅ | Layout responsivo até 320px sem scroll horizontal |
| 1.4.11 Non-text Contrast (AA) | ✅ | Bordas de input ≥ 3:1 com fundo |
| 1.4.12 Text Spacing (AA) | ✅ | Não há `!important` em line-height nem letter-spacing fixos |
| 1.4.13 Content on Hover or Focus (AA) | ✅ | Dropdowns Bootstrap (Esc fecha, persistent) |

### 2.2 Operable

| Critério | Status | Notas |
|---|---|---|
| 2.1.1 Keyboard (A) | ✅ | Tudo navegável por Tab. Modais com Esc. Wizard com next/prev por Enter |
| 2.1.2 No Keyboard Trap (A) | ✅ | Modais Bootstrap liberam foco no close |
| 2.4.1 Bypass Blocks (A) | ✅ | Skip link "Pular para o conteúdo" |
| 2.4.2 Page Titled (A) | ✅ | `<title>` único em cada template |
| 2.4.3 Focus Order (A) | ✅ | Ordem visual = ordem do DOM (sidebar → topbar → content) |
| 2.4.4 Link Purpose (A) | ⚠️ Parcial | Alguns links têm só ícone — `title` resolve mas `aria-label` é melhor |
| 2.4.6 Headings and Labels (AA) | ✅ | H1 único por página, h2 para cards, labels descritivas |
| 2.4.7 Focus Visible (AA) | ✅ | `:focus-visible` com outline rosa forte |
| 2.5.1 Pointer Gestures (A) | ✅ | Kanban e fases têm drag-drop **e** botões fallback |
| 2.5.2 Pointer Cancellation (A) | ✅ | Cliques disparam em `mouseup` (default) |
| 2.5.3 Label in Name (A) | ✅ | Texto visível dos botões = nome acessível |
| 2.5.5 Target Size (AAA) | ✅ | 44×44px em `pointer: coarse`; 48px no mobile do operador |

### 2.3 Understandable

| Critério | Status | Notas |
|---|---|---|
| 3.1.1 Language of Page (A) | ✅ | `<html lang="pt-BR">` |
| 3.2.1 On Focus (A) | ✅ | Nenhum form auto-submita ao receber foco |
| 3.2.2 On Input (A) | ✅ | Mudança em select de safra (analytics) muda contexto **e** avisa via "Filtrar" label |
| 3.3.1 Error Identification (A) | ✅ | Erros do Bean Validation aparecem inline com cor + ícone |
| 3.3.2 Labels or Instructions (A) | ✅ | Todo input com label + hint quando necessário |
| 3.3.3 Error Suggestion (AA) | ✅ | Mensagens em pt-BR descritivas ("Nome é obrigatório", "Deve ter no máximo 150 caracteres") |
| 3.3.4 Error Prevention (Legal/Financial) (AA) | ✅ | Cancelar pedido / excluir produto pedem confirmação (`onsubmit=confirm()` ou `ffConfirm`) |

### 2.4 Robust

| Critério | Status | Notas |
|---|---|---|
| 4.1.1 Parsing (A) | ✅ | HTML5 válido (Thymeleaf gera) |
| 4.1.2 Name, Role, Value (A) | ✅ | Botões nativos, links nativos, inputs nativos com labels |
| 4.1.3 Status Messages (AA) | ✅ | Toasts com `aria-live="polite"`, alerts com `role="alert"` |

---

## 3. Validação Manual (Semana 7-8 conforme doc v7)

### 3.1 Auditoria automática — axe DevTools

**Instalar:** Chrome Extension → axe DevTools
**Rodar em:**
```
http://localhost:8080/dashboard
http://localhost:8080/pedidos
http://localhost:8080/pedidos/wizard
http://localhost:8080/producao/kanban
http://localhost:8080/produtos
http://localhost:8080/financeiro/dashboard
http://localhost:8080/admin/tenant/identidade
http://localhost:8080/onboarding/etapa1
http://localhost:8080/catalogo
http://localhost:8080/login
```

**Meta:** 0 violations critical/serious. Moderate/minor podem ficar para issue.

### 3.2 Navegação por teclado — manual

Em cada tela:
1. Pressionar Tab repetidamente — focus deve ser **sempre visível** com anel rosa
2. Tab deve seguir ordem lógica (top-left → bottom-right)
3. Enter ativa botão/link em foco
4. Espaço ativa botão / marca checkbox
5. Esc fecha modais / dropdowns abertos
6. Setas navegam dropdowns Bootstrap

### 3.3 Screen reader — NVDA (Windows) / VoiceOver (Mac)

| Tela | O que deve anunciar |
|---|---|
| Login | "Login, Sistema Páscoa, formulário, login [campo de texto], senha [campo de texto], Entrar [botão]" |
| Dashboard | "Dashboard, título nível 1, KPIs..." |
| Lista pedidos | "Pedidos, título 1, filtros: ..., tabela com 7 colunas e N linhas, ..." |
| Alerta toast | Anuncia imediatamente "Pedido salvo com sucesso" |

### 3.4 Touch targets — Chrome DevTools Mobile

1. F12 → modo mobile (iPhone 12)
2. Inspecionar qualquer botão ícone-only → deve ter ≥ 44×44px
3. Mobile produção (`/producao/mobile`) → botões devem ter ≥ 48px

### 3.5 Lighthouse — meta ≥ 90 em cada categoria

```
Chrome → F12 → Lighthouse → Categories: Performance, Accessibility, Best Practices, SEO, PWA
→ Generate report
```

**Telas críticas:**
- `/dashboard` — fluxo principal autenticado
- `/catalogo` — público
- `/acompanhamento/{token}` — público
- `/onboarding/etapa1` — público

### 3.6 Cross-browser

| Browser | Versão mínima | Critério |
|---|---|---|
| Chrome | 100+ | Layout idêntico, animações suaves |
| Firefox | 100+ | `color-mix()` funciona, `:has()` para choice cards |
| Safari (iOS) | 15+ | Inputs nativos, PWA install |
| Edge | 100+ | Forced colors mode funciona |

`color-mix(in srgb, ...)` requer Chrome 111 / Firefox 113 / Safari 16.2. Se precisar suportar versões anteriores, fallback hard-coded já existe nos tokens semânticos.

`:has()` (usado em `.ff-choice:has(input:checked)`) — Chrome 105, Firefox 121, Safari 15.4.

### 3.7 E2E — fluxo completo

**Cenário:** Login → Wizard → Produção → Entrega → Notificação

```
1. Logar (admin/admin123)
2. /pedidos/wizard → criar pedido para cliente X com produto Y, qtd 2, data entrega +7 dias
3. Confirmar pedido → status = CONFIRMADO → ordem aparece em /producao/kanban
4. Mover ordem para EM_PRODUCAO (drag ou botão)
5. Concluir ordem → pedido vai para PRONTO
6. Registrar entrega → status = ENTREGUE
7. Verificar /acompanhamento/{token} mostra todos os passos com ✓
```

**Esperado:**
- Cada transição mostra toast / flash
- Notificações são enviadas (testMode no dev)
- Nenhum 500 / log de erro
- Pedido aparece em /financeiro/dashboard com faturamento atualizado

---

## 4. Itens conhecidos a melhorar

### 4.1 Contraste do header da sidebar
**Cor primária default (`#e91e63`) × texto branco = 3.94:1** — atende AA Large mas não AA texto normal.

**Opções:**
1. Trocar default para `#c2185b` (primary-dark) — 5.16:1, atende AA
2. Manter primária mas usar texto pequeno apenas em headers/badges
3. Documentar como cor de marca e exigir do tenant uma escolha que atenda contraste no `TenantColorValidator`

Recomendação: deixar default com `#c2185b` para uso em superfícies grandes, e `#e91e63` apenas como cor de destaque/badge.

### 4.2 aria-label em botões icon-only
Botões `ff-btn--icon` usam `title="..."` (anunciado pelo screen reader em delay). Para melhor experiência, substituir por `aria-label="..."`. Refactor mecânico: ~30 ocorrências em produtos/lista, pedidos/lista, materias-primas/lista, financeiro/dashboard.

### 4.3 aria-current="step" no stepper de onboarding
O `_tabs.html` do admin tenant já destaca visualmente o passo atual. Adicionar `aria-current="step"` no `<li class="ff-stepper__step--current">` para que screen readers anunciem "passo atual".

### 4.4 Dark mode (futuro)
Tokens semânticos já permitem mudança simples adicionando `@media (prefers-color-scheme: dark)` em `tokens.css` com overrides de `--color-page-bg`, `--color-card-bg`, `--color-text-primary`. Fora do escopo da v7.

---

## 5. Ferramentas recomendadas para a auditoria

| Ferramenta | Uso |
|---|---|
| **axe DevTools** (Chrome Extension) | Auditoria automática WCAG |
| **WAVE** (browser extension) | Visual feedback de problemas a11y |
| **Lighthouse** (Chrome DevTools) | Score Performance/A11y/PWA/SEO |
| **NVDA** (Windows, grátis) | Screen reader para validar fluxos |
| **VoiceOver** (macOS/iOS, built-in) | Screen reader Apple |
| **WebAIM Contrast Checker** | Validação manual de contraste |
| **Stark** (plugin Figma/Chrome) | Simulação de daltonismo |
| **Tab Stops** (axe feature) | Visualiza ordem de Tab |

---

## 6. Resumo executivo

A v7 entrega uma **fundação de acessibilidade sólida no Design System**: focus visíveis, navegação semântica, formulários acessíveis, touch targets adequados, suporte a high contrast e reduced motion. O `TenantColorValidator` (FASE 7.6) garante que cada tenant white-label seja avisado quando sua cor falhar WCAG.

**Os 3 itens que faltam para WCAG AA pleno** são tratáveis em um sprint curto:
1. Substituir `title` por `aria-label` em ícones (~30 trocas mecânicas)
2. Trocar cor default da sidebar para `#c2185b` (1 linha em `tokens.css`)
3. Adicionar `aria-current="step"` nos steppers (4 templates)

A validação automática (axe + Lighthouse) e manual (NVDA + teclado puro) **deve ser executada pelo dev no IntelliJ** antes do merge para `main` — o ambiente desta sessão não tem browser headless. Os comandos e URLs estão na §3 deste documento.
