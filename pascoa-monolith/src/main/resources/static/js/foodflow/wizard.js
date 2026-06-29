/* ═══════════════════════════════════════════════════════════════════
   FoodFlow Wizard Controller v7
   Navegação entre etapas + validação por etapa antes de avançar.
   Compatível com a estrutura DOM do pedidos/wizard.html (4 etapas).

   API:
     FFWizard.init({
       totalSteps: 4,
       onValidate: function(step) { return true|false|errorMsg },
       onEnter:    function(step) { ... },
       onSubmit:   function(form, e) { ... }
     });
     FFWizard.goTo(n)
     FFWizard.next() / FFWizard.prev()
   ═══════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  var state = {
    current: 1,
    total:   1,
    config:  {},
  };

  function setStep(n) {
    if (n < 1 || n > state.total) return;
    document.querySelectorAll('[data-wizard-step]').forEach(function (el) {
      var step = parseInt(el.getAttribute('data-wizard-step'), 10);
      el.style.display = (step === n) ? '' : 'none';
    });
    // Atualiza stepper visual
    document.querySelectorAll('.ff-stepper__step').forEach(function (el) {
      var step = parseInt(el.getAttribute('data-step'), 10);
      el.classList.remove('ff-stepper__step--active', 'ff-stepper__step--done');
      if (step === n) el.classList.add('ff-stepper__step--active');
      else if (step < n) el.classList.add('ff-stepper__step--done');
    });
    document.querySelectorAll('.ff-stepper__line').forEach(function (el, i) {
      el.classList.toggle('ff-stepper__line--done', i < n - 1);
    });
    state.current = n;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    if (typeof state.config.onEnter === 'function') state.config.onEnter(n);
  }

  function goTo(n) {
    if (n > state.current && typeof state.config.onValidate === 'function') {
      var result = state.config.onValidate(state.current);
      if (result === false) return;
      if (typeof result === 'string' && result.length > 0) {
        if (window.ffToast) ffToast('danger', result);
        else alert(result);
        return;
      }
    }
    setStep(n);
  }

  function next() { goTo(state.current + 1); }
  function prev() { goTo(state.current - 1); }

  function init(config) {
    state.config = config || {};
    state.total  = config.totalSteps || 1;
    setStep(1);

    // Botões com data-wizard-action (auto-bind)
    document.querySelectorAll('[data-wizard-action]').forEach(function (btn) {
      btn.addEventListener('click', function (e) {
        e.preventDefault();
        var act = btn.getAttribute('data-wizard-action');
        if (act === 'next') next();
        else if (act === 'prev') prev();
        else if (act.indexOf('go:') === 0) goTo(parseInt(act.slice(3), 10));
      });
    });

    // Hook de submit
    if (config.form && typeof config.onSubmit === 'function') {
      var form = (typeof config.form === 'string')
        ? document.querySelector(config.form) : config.form;
      if (form) {
        form.addEventListener('submit', function (e) {
          // Última validação no passo final
          if (typeof config.onValidate === 'function') {
            var r = config.onValidate(state.current);
            if (r === false || typeof r === 'string') {
              e.preventDefault();
              if (typeof r === 'string' && window.ffToast) ffToast('danger', r);
              return;
            }
          }
          // Loader no botão de submit
          var btn = form.querySelector('[type="submit"]');
          if (btn) btn.classList.add('is-loading');
          config.onSubmit(form, e);
        });
      }
    }
  }

  window.FFWizard = { init: init, goTo: goTo, next: next, prev: prev,
                      get current() { return state.current; } };
})();
