(function () {
  // Preenche modal de edição
  document.getElementById('modalEditarTemplate').addEventListener('show.bs.modal', function(event) {
    const btn = event.relatedTarget;
    document.getElementById('editId').value = btn.dataset.id;
    document.getElementById('editAssunto').value = btn.dataset.assunto || '';
    document.getElementById('editCorpo').value = btn.dataset.corpo || '';

    const selEvento = document.getElementById('editEvento');
    for (let opt of selEvento.options) opt.selected = (opt.value === btn.dataset.evento);

    const selCanal = document.getElementById('editCanal');
    for (let opt of selCanal.options) opt.selected = (opt.value === btn.dataset.canal);
  });
})();
