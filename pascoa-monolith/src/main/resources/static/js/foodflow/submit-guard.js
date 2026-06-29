/* ════════════════════════════════════════════════════════════════════
   F-06 — Submit guard global contra duplo submit.

   Aplica-se a TODOS os forms POST (e qualquer form com `data-submit-once`):
   - No primeiro submit, desabilita o botão e marca o form como `submitting`.
   - Submits subsequentes (Enter rápido / double-click) são ignorados.
   - Safety net: re-habilita o botão após 10s caso a navegação não ocorra
     (rede offline, validação server-side com erro 4xx, etc.).
   - Opt-out por form ou botão: atributo `data-allow-resubmit`.

   Importante: NÃO interfere em validação client-side — só age depois que
   o browser decidiu submeter (post-checkValidity).
   ════════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  const TIMEOUT_MS = 10000;

  function isEligible(form) {
    if (form.dataset.allowResubmit === 'true') return false;
    if (form.dataset.submitOnce === 'true') return true;
    const method = (form.getAttribute('method') || 'get').toLowerCase();
    return method === 'post';
  }

  function guard(form) {
    if (form.dataset.guardApplied === 'true') return;
    form.dataset.guardApplied = 'true';

    form.addEventListener('submit', function (e) {
      if (!isEligible(form)) return;
      if (form.dataset.submitting === 'true') {
        e.preventDefault();
        e.stopImmediatePropagation();
        return;
      }
      form.dataset.submitting = 'true';

      const submitters = form.querySelectorAll('button[type="submit"], input[type="submit"]');
      submitters.forEach(function (btn) {
        if (btn.dataset.allowResubmit === 'true') return;
        btn.dataset.originalHTML = btn.innerHTML || '';
        btn.disabled = true;
        btn.setAttribute('aria-busy', 'true');
        if (btn.tagName === 'BUTTON' && btn.innerHTML !== '') {
          btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>' +
                          (btn.dataset.busyLabel || 'Aguarde...');
        }
      });

      // Safety net — re-habilita se o usuário ficar preso (ex.: erro 4xx).
      setTimeout(function () { reset(form); }, TIMEOUT_MS);
    });
  }

  function reset(form) {
    if (form.dataset.submitting !== 'true') return;
    delete form.dataset.submitting;
    form.querySelectorAll('button[type="submit"], input[type="submit"]').forEach(function (btn) {
      btn.disabled = false;
      btn.removeAttribute('aria-busy');
      if (btn.dataset.originalHTML !== undefined) {
        btn.innerHTML = btn.dataset.originalHTML;
        delete btn.dataset.originalHTML;
      }
    });
  }

  function applyAll(root) {
    root.querySelectorAll('form').forEach(guard);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { applyAll(document); });
  } else {
    applyAll(document);
  }

  // Reset ao voltar pelo bfcache (Safari/Firefox): senão o botão fica disabled.
  window.addEventListener('pageshow', function (e) {
    if (e.persisted) document.querySelectorAll('form').forEach(reset);
  });

  window.ffApplySubmitGuard = applyAll;
  window.ffResetSubmitGuard = reset;
})();
