(function () {
  var sazonal = document.getElementById('sazonal');
  var campos = document.getElementById('camposTemporada');
  if (!sazonal || !campos) { return; }
  function atualizar() {
    campos.classList.toggle('d-none', !sazonal.checked);
  }
  sazonal.addEventListener('change', atualizar);
  atualizar();
})();
