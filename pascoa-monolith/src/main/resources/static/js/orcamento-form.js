(function () {
  var corpo = document.getElementById('linhasItens');
  var modelo = document.getElementById('modeloLinha');
  if (!corpo || !modelo) return;
  var indice = corpo.querySelectorAll('tr').length;
  var moeda = function (v) { return 'R$ ' + v.toLocaleString('pt-BR', { minimumFractionDigits: 2 }); };

  function recalcular() {
    var total = 0;
    corpo.querySelectorAll('tr').forEach(function (tr) {
      var preco = tr.querySelector('.preco-input');
      var qtd = tr.querySelector('.qtd-input');
      var sub = parseFloat(preco && preco.value || 0) * parseFloat(qtd && qtd.value || 0);
      var cel = tr.querySelector('.subtotal');
      if (cel) cel.textContent = sub > 0 ? moeda(sub) : '—';
      total += sub || 0;
    });
    document.getElementById('totalGeral').textContent = moeda(total);
  }

  function ligar(tr) {
    var sel = tr.querySelector('.produto-select');
    var preco = tr.querySelector('.preco-input');
    sel.addEventListener('change', function () {
      var opt = sel.selectedOptions[0];
      if (opt && opt.dataset.preco) preco.value = opt.dataset.preco;
      recalcular();
    });
    preco.addEventListener('input', recalcular);
    tr.querySelector('.qtd-input').addEventListener('input', recalcular);
  }

  document.getElementById('btnAdicionarLinha').addEventListener('click', function () {
    var idx = indice++;
    var tr = modelo.content.firstElementChild.cloneNode(true);
    tr.id = 'linha-' + idx;
    tr.querySelectorAll('[name]').forEach(function (el) { el.name = el.name.replace('__IDX__', idx); });
    corpo.appendChild(tr);
    ligar(tr);
    recalcular();
  });

  corpo.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-remover]');
    if (!btn) return;
    btn.closest('tr').remove();
    recalcular();
  });

  corpo.querySelectorAll('tr').forEach(ligar);
  recalcular();
})();
