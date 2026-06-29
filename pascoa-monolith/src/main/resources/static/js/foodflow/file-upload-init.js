/* ════════════════════════════════════════════════════════════════════
   F-09 — File upload estilizado.

   Para cada input[type=file] sem opt-out (`data-no-file-style`):
   - Envolve em .ff-file-upload, adiciona label clicável "Escolher arquivo"
     e um span .ff-file-upload__name que mostra o nome selecionado.
   - Mantém o input no DOM com `name` original (preserva submit + multipart).
   - Re-aplicável via window.ffApplyFileUpload(root).
   ════════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  function labelText(input) {
    return input.dataset.fileLabel ||
           (input.multiple ? 'Escolher arquivos' : 'Escolher arquivo');
  }

  function init(input) {
    if (input.dataset.fileStyled === 'true' || input.dataset.noFileStyle === 'true') return;
    input.dataset.fileStyled = 'true';

    // Garante id para o <label for>.
    if (!input.id) input.id = 'ffFile' + Math.random().toString(36).slice(2, 8);

    // Wrapper
    const wrap = document.createElement('span');
    wrap.className = 'ff-file-upload';
    input.parentNode.insertBefore(wrap, input);
    wrap.appendChild(input);

    // Substitui a classe visual do input para invisível.
    input.classList.remove('ff-input');
    input.classList.add('ff-file-upload__input');

    // Label clicável
    const lbl = document.createElement('label');
    lbl.setAttribute('for', input.id);
    lbl.className = 'ff-file-upload__label';
    lbl.innerHTML = '<i class="bi bi-cloud-upload"></i><span>' + labelText(input) + '</span>';
    wrap.appendChild(lbl);

    // Nome do arquivo
    const name = document.createElement('span');
    name.className = 'ff-file-upload__name ff-file-upload__name--empty';
    name.textContent = 'Nenhum arquivo selecionado';
    wrap.appendChild(name);

    input.addEventListener('change', function () {
      const files = input.files;
      if (!files || files.length === 0) {
        name.textContent = 'Nenhum arquivo selecionado';
        name.classList.add('ff-file-upload__name--empty');
        return;
      }
      name.classList.remove('ff-file-upload__name--empty');
      name.textContent = files.length === 1
        ? files[0].name
        : files.length + ' arquivos selecionados';
    });
  }

  function applyAll(root) {
    root.querySelectorAll('input[type="file"]').forEach(init);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { applyAll(document); });
  } else {
    applyAll(document);
  }

  window.ffApplyFileUpload = applyAll;
})();
