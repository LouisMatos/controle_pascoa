(function () {
  const mpSelect      = document.getElementById('mpSelect');
  const novaQtdInput  = document.getElementById('novaQtdInput');
  const previewEl     = document.getElementById('previewAjuste');

  function atualizarInfoMP() {
      const opt = mpSelect.options[mpSelect.selectedIndex];
      if (!opt.value) {
          document.getElementById('infoMP').classList.add('d-none');
          previewEl.classList.add('d-none');
          document.getElementById('unidadeLabel').textContent = '—';
          return;
      }
      const saldo   = parseFloat(opt.dataset.saldo  || 0);
      const unidade = opt.dataset.unidade || '';

      document.getElementById('unidadeLabel').textContent = unidade;
      document.getElementById('saldoAtual').textContent   = saldo.toFixed(3) + ' ' + unidade;
      document.getElementById('infoMP').classList.remove('d-none');

      atualizarPreview();
  }

  function atualizarPreview() {
      const opt = mpSelect.options[mpSelect.selectedIndex];
      if (!opt.value) return;

      const saldo   = parseFloat(opt.dataset.saldo || 0);
      const unidade = opt.dataset.unidade || '';
      const nova    = parseFloat(novaQtdInput.value);

      if (novaQtdInput.value === '' || isNaN(nova)) {
          previewEl.classList.add('d-none');
          return;
      }

      const delta = nova - saldo;
      const isAumento = delta >= 0;

      document.getElementById('prevSaldoAtual').textContent = saldo.toFixed(3) + ' ' + unidade;
      document.getElementById('prevNovoSaldo').textContent  = nova.toFixed(3) + ' ' + unidade;

      const deltaEl = document.getElementById('prevDelta');
      deltaEl.textContent = (isAumento ? '+' : '') + delta.toFixed(3) + ' ' + unidade;
      deltaEl.className   = isAumento ? 'text-success' : 'text-danger';

      previewEl.classList.remove('d-none');
      previewEl.className = previewEl.className.replace(/alert-\w+/, '');
      previewEl.classList.add(isAumento ? 'alert-success' : (delta < 0 ? 'alert-danger' : 'alert-secondary'));
  }

  // Aplica ao carregar caso já haja valor (retorno de validação)
  if (mpSelect.value) atualizarInfoMP();
  mpSelect.addEventListener('change', atualizarInfoMP);
  novaQtdInput.addEventListener('input', atualizarPreview);
})();
