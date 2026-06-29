/* ════════════════════════════════════════════════════════════════════
   F-02 — Color picker universal via Pickr (@simonwep/pickr).

   Substitui o input[type=color] nativo (incompatível em iOS Safari e
   inconsistente em Samsung Internet) por uma UI cross-browser com
   suporte completo a touch.

   - Mantém o input original no DOM (escondido) para preservar o `name`
     no submit do form e compatibilidade com Alpine.js (x-model).
   - Dispara `input` e `change` events ao confirmar uma cor, para que
     Alpine.js, listeners @input e libs reativas atualizem o modelo.
   - Escape hatch: input[data-no-pickr="true"] é ignorado.

   Depende de Pickr (carregado via CDN no layout).
   ════════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  if (typeof window.Pickr !== 'function') return;

  function init(input) {
    if (input.dataset.pickrApplied === 'true' || input.dataset.noPickr === 'true') return;
    input.dataset.pickrApplied = 'true';

    // Esconde visualmente o input nativo (mantém no DOM para o form/Alpine).
    input.style.position = 'absolute';
    input.style.left = '-9999px';
    input.style.opacity = '0';
    input.setAttribute('aria-hidden', 'true');
    input.tabIndex = -1;

    // Botão visual que abre o Pickr — herda dimensões inline do input.
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'ff-pickr-btn ' + (input.className || '');
    btn.style.cssText = (input.getAttribute('style') || '') +
      ';display:inline-block;background:' + (input.value || '#ffffff') +
      ';border:1px solid var(--color-card-border, #d1d5db);' +
      'border-radius:var(--radius-md, 6px);cursor:pointer;min-width:44px;min-height:40px;';
    btn.setAttribute('aria-label', 'Escolher cor — ' + (input.value || '#ffffff'));
    input.parentNode.insertBefore(btn, input.nextSibling);

    const pickr = window.Pickr.create({
      el: btn,
      theme: 'nano',
      default: input.value || '#ffffff',
      useAsButton: true,
      components: {
        preview: true,
        opacity: false,
        hue: true,
        interaction: { hex: true, input: true, save: true, clear: false }
      }
    });

    pickr.on('save', function (color) {
      if (!color) return;
      const hex = color.toHEXA().toString().substring(0, 7); // #RRGGBB sem alfa
      input.value = hex;
      btn.style.background = hex;
      btn.setAttribute('aria-label', 'Escolher cor — ' + hex);
      input.dispatchEvent(new Event('input',  { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
      pickr.hide();
    });

    pickr.on('change', function (color) {
      if (!color) return;
      btn.style.background = color.toHEXA().toString();
    });
  }

  function applyAll(root) {
    root.querySelectorAll('input[type="color"]').forEach(init);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { applyAll(document); });
  } else {
    applyAll(document);
  }

  window.ffApplyPickr = applyAll;
})();
