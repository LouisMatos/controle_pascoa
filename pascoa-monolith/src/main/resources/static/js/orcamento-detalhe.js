(function () {
  var btn = document.getElementById('btnCopiarLink');
  var input = document.getElementById('linkAprovacao');
  if (!btn || !input) return;
  var rotulo = btn.innerHTML;
  btn.addEventListener('click', function () {
    input.select();
    navigator.clipboard.writeText(input.value).then(function () {
      btn.innerHTML = '<i class="bi bi-check-lg me-1" aria-hidden="true"></i>Copiado!';
      btn.classList.replace('btn-outline-secondary', 'btn-success');
      setTimeout(function () {
        btn.innerHTML = rotulo;
        btn.classList.replace('btn-success', 'btn-outline-secondary');
      }, 2000);
    });
  });
})();
