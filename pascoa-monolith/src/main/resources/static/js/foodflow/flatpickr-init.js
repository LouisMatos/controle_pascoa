/* ════════════════════════════════════════════════════════════════════
   F-01 — Date/Time picker universal via Flatpickr.

   Substitui o picker nativo de input[type=date|time|datetime-local]
   por uma UI consistente em todos os browsers (especialmente Safari
   desktop, que não tem date picker nativo).

   - Mantém o `name` e value no formato ISO esperado pelo Spring
     (@DateTimeFormat ISO.DATE / ISO.TIME / ISO.DATE_TIME).
   - Locale pt-BR para o picker; valor serializado segue ISO.
   - Inputs com data-flatpickr são também processados (uso explícito).
   - Inputs com data-no-flatpickr são pulados (escape hatch).

   Depende de flatpickr + locale pt (carregados via CDN no layout).
   ════════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  if (typeof window.flatpickr !== 'function') return;

  if (window.flatpickr.l10ns && window.flatpickr.l10ns.pt) {
    window.flatpickr.localize(window.flatpickr.l10ns.pt);
  }

  const COMMON = {
    allowInput: true,
    disableMobile: false, // Sempre Flatpickr, mesmo em mobile (consistência).
    monthSelectorType: 'static'
  };

  function init(el, opts) {
    if (el._flatpickr || el.dataset.noFlatpickr === 'true') return;
    el.classList.add('ff-flatpickr');
    window.flatpickr(el, Object.assign({}, COMMON, opts));
  }

  function applyAll(root) {
    root.querySelectorAll('input[type="date"]').forEach(function (el) {
      init(el, { dateFormat: 'Y-m-d', altInput: true, altFormat: 'd/m/Y' });
    });
    root.querySelectorAll('input[type="time"]').forEach(function (el) {
      init(el, { enableTime: true, noCalendar: true, dateFormat: 'H:i', time_24hr: true });
    });
    root.querySelectorAll('input[type="datetime-local"]').forEach(function (el) {
      init(el, { enableTime: true, dateFormat: 'Y-m-d\\TH:i',
                 altInput: true, altFormat: 'd/m/Y H:i', time_24hr: true });
    });
    root.querySelectorAll('input[data-flatpickr]:not([type="date"]):not([type="time"]):not([type="datetime-local"])').forEach(function (el) {
      const withTime = el.dataset.enableTime === 'true';
      init(el, {
        enableTime: withTime,
        dateFormat: withTime ? 'Y-m-d H:i' : 'Y-m-d',
        altInput: true,
        altFormat: withTime ? 'd/m/Y H:i' : 'd/m/Y',
        time_24hr: true
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { applyAll(document); });
  } else {
    applyAll(document);
  }

  // Exposto para conteúdo carregado dinamicamente (ex.: modais, AJAX).
  window.ffApplyFlatpickr = applyAll;
})();
