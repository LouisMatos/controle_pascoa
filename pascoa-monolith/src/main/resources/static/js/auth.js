document.querySelectorAll('[data-toggle-senha]').forEach(function (btn) {
  btn.addEventListener('click', function () {
    var input = document.getElementById(btn.dataset.toggleSenha);
    var mostrar = input.type === 'password';
    input.type = mostrar ? 'text' : 'password';
    btn.setAttribute('aria-pressed', String(mostrar));
    btn.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
    btn.querySelector('i').className = 'bi ' + (mostrar ? 'bi-eye-slash' : 'bi-eye');
  });
});

document.querySelectorAll('form[data-loading]').forEach(function (form) {
  var btn = form.querySelector('button[type=submit]');
  var icone = btn.querySelector('i');
  var rotulo = btn.querySelector('.rotulo');
  var original = { icone: icone.className, rotulo: rotulo.textContent };

  function restaurar() {
    btn.disabled = false;
    icone.className = original.icone;
    rotulo.textContent = original.rotulo;
  }

  form.addEventListener('submit', function () {
    btn.disabled = true;
    icone.className = 'spinner-border spinner-border-sm me-2';
    rotulo.textContent = form.dataset.loading;
  });
  window.addEventListener('pageshow', function (e) { if (e.persisted) restaurar(); });
});

document.querySelectorAll('[data-auto-submit]').forEach(function (input) {
  input.addEventListener('input', function () {
    input.value = input.value.replace(/\D/g, '');
    if (input.value.length === input.maxLength) input.form.requestSubmit();
  });
});

document.querySelectorAll('[data-copiar]').forEach(function (btn) {
  btn.addEventListener('click', function () {
    var texto = document.getElementById(btn.dataset.copiar).textContent.trim();
    navigator.clipboard.writeText(texto).then(function () {
      var rotulo = btn.querySelector('.rotulo');
      var anterior = rotulo.textContent;
      rotulo.textContent = 'Copiado';
      setTimeout(function () { rotulo.textContent = anterior; }, 2000);
    });
  });
});

var formReset = document.getElementById('formReset');
if (formReset) {
  var nova = document.getElementById('novaSenha');
  var confirma = document.getElementById('confirmarSenha');
  var barra = document.getElementById('forcaBar');
  var barraWrap = document.getElementById('forcaWrap');
  var rotuloForca = document.getElementById('forcaLabel');
  var msg = document.getElementById('matchMsg');
  var niveis = [
    { pct: 0, cor: 'bg-secondary', txt: '' },
    { pct: 25, cor: 'bg-danger', txt: 'Muito fraca' },
    { pct: 50, cor: 'bg-warning', txt: 'Fraca' },
    { pct: 70, cor: 'bg-info', txt: 'Razoável' },
    { pct: 90, cor: 'bg-primary', txt: 'Boa' },
    { pct: 100, cor: 'bg-success', txt: 'Forte' }
  ];

  function atualizarForca() {
    var s = nova.value;
    var pontos = [s.length >= 8, s.length >= 12, /[A-Z]/.test(s), /[0-9]/.test(s), /[^A-Za-z0-9]/.test(s)]
      .filter(Boolean).length;
    var n = niveis[pontos];
    barra.style.width = n.pct + '%';
    barra.className = 'progress-bar ' + n.cor;
    barraWrap.setAttribute('aria-valuenow', String(n.pct));
    rotuloForca.textContent = n.txt ? 'Força da senha: ' + n.txt : '';
  }

  function validarConfirmacao() {
    if (confirma.value === '') {
      msg.textContent = '';
      msg.className = 'form-text';
      confirma.setCustomValidity('');
    } else if (nova.value === confirma.value) {
      msg.textContent = 'Senhas coincidem.';
      msg.className = 'form-text text-success';
      confirma.setCustomValidity('');
    } else {
      msg.textContent = 'As senhas não coincidem. Digite a mesma senha nos dois campos.';
      msg.className = 'form-text text-danger';
      confirma.setCustomValidity('As senhas não coincidem.');
    }
  }

  nova.addEventListener('input', function () { atualizarForca(); validarConfirmacao(); });
  confirma.addEventListener('input', validarConfirmacao);
}
