/* ════════════════════════════════════════════════════════════════════
   F-08 — Desliga a validação nativa do browser (tooltips amarelos que
   não seguem o Design System) e aplica validação custom integrada com
   as classes `.is-invalid` / `.ff-input--error` já existentes.

   - Adiciona `novalidate` em TODOS os forms (opt-out: `data-validate-native`).
   - No blur de cada campo, valida via `checkValidity()` e marca/desmarca
     `.is-invalid`. Mensagem de erro injetada como `<div class="invalid-feedback">`
     irmão do campo (compatível com Bootstrap e com `.ff-field`).
   - No submit, se `checkValidity()` falhar: bloqueia o envio, marca todos
     os campos inválidos e foca no primeiro.
   - Compatível com submit-guard.js (este roda ANTES e bloqueia o submit
     antes que o guard marque o form como `submitting`).
   ════════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  function shouldGuard(form) {
    return form.dataset.validateNative !== 'true';
  }

  function showError(field) {
    field.classList.add('is-invalid', 'ff-input--error');
    field.setAttribute('aria-invalid', 'true');
    let fb = field.parentElement && field.parentElement.querySelector('.invalid-feedback.ff-auto');
    if (!fb) {
      fb = document.createElement('div');
      fb.className = 'invalid-feedback ff-auto';
      fb.style.display = 'block';
      field.parentElement && field.parentElement.appendChild(fb);
    }
    fb.textContent = field.validationMessage || 'Campo inválido.';
  }

  function clearError(field) {
    field.classList.remove('is-invalid', 'ff-input--error');
    field.removeAttribute('aria-invalid');
    const fb = field.parentElement && field.parentElement.querySelector('.invalid-feedback.ff-auto');
    if (fb) fb.remove();
  }

  function validateField(field) {
    if (!field.willValidate) return true;
    if (field.checkValidity()) {
      clearError(field);
      return true;
    }
    showError(field);
    return false;
  }

  function attachField(field) {
    if (field.dataset.validateAttached === 'true') return;
    field.dataset.validateAttached = 'true';
    field.addEventListener('blur', function () {
      // Só valida no blur depois que o usuário interagiu — evita marcar
      // ":invalid" assim que a página abre.
      if (field.dataset.touched === 'true') validateField(field);
    });
    field.addEventListener('input', function () {
      field.dataset.touched = 'true';
      // Se já estava marcado como erro, re-valida ao corrigir.
      if (field.classList.contains('is-invalid')) validateField(field);
    });
  }

  function guard(form) {
    if (!shouldGuard(form)) return;
    if (form.dataset.validateApplied === 'true') return;
    form.dataset.validateApplied = 'true';
    form.setAttribute('novalidate', 'novalidate');

    form.querySelectorAll('input, select, textarea').forEach(attachField);

    form.addEventListener('submit', function (e) {
      const invalid = [];
      form.querySelectorAll('input, select, textarea').forEach(function (f) {
        f.dataset.touched = 'true';
        if (!validateField(f)) invalid.push(f);
      });
      if (invalid.length > 0) {
        e.preventDefault();
        e.stopImmediatePropagation();
        invalid[0].focus();
      }
    }, true); // useCapture — roda antes do submit-guard
  }

  function applyAll(root) {
    root.querySelectorAll('form').forEach(guard);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { applyAll(document); });
  } else {
    applyAll(document);
  }

  window.ffApplyValidation = applyAll;
})();
