(function () {
  var canal = document.getElementById('canalSelect');
  var assunto = document.getElementById('assuntoRow');
  if (!canal || !assunto) return;
  function toggle() { assunto.classList.toggle('d-none', canal.value !== 'EMAIL'); }
  canal.addEventListener('change', toggle);
  toggle();
})();
