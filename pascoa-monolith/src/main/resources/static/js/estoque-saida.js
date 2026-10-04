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
      const saida   = parseFloat(qtdInput.value || 0);
      const alertaExcede = document.getElementById('alertaExcede');

      if (saida > 0) {
          document.getElementById('prevSaldoAtual').textContent = saldo.toFixed(3) + ' ' + unidade;
          document.getElementById('prevSaida').textContent      = saida.toFixed(3) + ' ' + unidade;
          document.getElementById('prevNovoSaldo').textContent  = (saldo - saida).toFixed(3) + ' ' + unidade;
          document.getElementById('previewTotal').classList.remove('d-none');
          alertaExcede.classList.toggle('d-none', saida <= saldo);
      } else {
          document.getElementById('previewTotal').classList.add('d-none');
          alertaExcede.classList.add('d-none');
      }
  }

  qtdInput.addEventListener('input', atualizarPreview);

  // Aplica ao carregar caso já haja valor (ex: retorno de validação)
  if (mpSelect.value) atualizarInfoMP();
  mpSelect.addEventListener('change', atualizarInfoMP);
})();
