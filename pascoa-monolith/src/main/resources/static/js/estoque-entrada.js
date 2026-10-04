(function () {
  const mpSelect = document.getElementById('mpSelect');
  const qtdInput = document.getElementById('qtdInput');

  function atualizarInfoMP() {
      const opt = mpSelect.options[mpSelect.selectedIndex];
      if (!opt.value) {
          document.getElementById('infoMP').classList.add('d-none');
          document.getElementById('previewTotal').classList.add('d-none');
          document.getElementById('unidadeLabel').textContent = '—';
          return;
      }
      const saldo  = parseFloat(opt.dataset.saldo  || 0);
      const unidade = opt.dataset.unidade || '';
      const custo  = parseFloat(opt.dataset.custo  || 0);

      document.getElementById('unidadeLabel').textContent = unidade;
      document.getElementById('saldoAtual').textContent   = saldo.toFixed(3) + ' ' + unidade;
      document.getElementById('infoMP').classList.remove('d-none');

      // Pre-fill custo
      if (!document.getElementById('custoInput').value) {
          document.getElementById('custoInput').value = custo.toFixed(4);
      }

      atualizarPreview();
  }

  function atualizarPreview() {
      const opt = mpSelect.options[mpSelect.selectedIndex];
      if (!opt.value) return;

      const saldo   = parseFloat(opt.dataset.saldo || 0);
      const unidade = opt.dataset.unidade || '';
      const entrada = parseFloat(qtdInput.value || 0);

      if (entrada > 0) {
          document.getElementById('prevSaldoAtual').textContent = saldo.toFixed(3) + ' ' + unidade;
          document.getElementById('prevEntrada').textContent    = entrada.toFixed(3) + ' ' + unidade;
          document.getElementById('prevNovoSaldo').textContent  = (saldo + entrada).toFixed(3) + ' ' + unidade;
          document.getElementById('previewTotal').classList.remove('d-none');
      } else {
          document.getElementById('previewTotal').classList.add('d-none');
      }
  }

  qtdInput.addEventListener('input', atualizarPreview);

  // Aplica ao carregar caso já haja valor (ex: retorno de validação)
  if (mpSelect.value) atualizarInfoMP();
  mpSelect.addEventListener('change', atualizarInfoMP);
})();
