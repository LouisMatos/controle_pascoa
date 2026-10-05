  // Mapa id → {id, nome, preco} lido a partir dos data-* attributes das <option>
  const produtoMap = {};
  document.querySelectorAll('#selProduto option[value]').forEach(opt => {
    const id = parseInt(opt.value);
    if (id) {
      produtoMap[id] = {
        id:    id,
        nome:  opt.dataset.nome,
        preco: parseFloat(opt.dataset.preco)
      };
    }
  });

const hoje = new Date();
document.getElementById('inpDataEntrega').min =
  hoje.getFullYear() + '-' + String(hoje.getMonth() + 1).padStart(2, '0') + '-' + String(hoje.getDate()).padStart(2, '0');

// ============================================================
// Estado do wizard
// ============================================================
let stepAtual = 1;
let itensSelecionados = []; // [{produtoId, nome, quantidade, preco}]
let clienteSelecionadoId = null;
let clienteSelecionadoNome = '';
let clienteSelecionadoTelefone = '';
let clienteSelecionadoEmail = '';

// ============================================================
// Passo 1 — Busca de cliente
// ============================================================
document.querySelectorAll('.cliente-row').forEach(row => {
  row.addEventListener('click', function() {
    const radio = this.querySelector('.cliente-radio');
    radio.checked = true;
    selecionarCliente(this.dataset);
  });
});

document.querySelectorAll('.cliente-radio').forEach(radio => {
  radio.addEventListener('change', function() {
    const row = this.closest('.cliente-row');
    selecionarCliente(row.dataset);
  });
});

function selecionarCliente(data) {
  clienteSelecionadoId   = data.id;
  clienteSelecionadoNome = data.nome;
  clienteSelecionadoTelefone = data.telefone || '';
  clienteSelecionadoEmail    = data.email    || '';

  document.getElementById('csNome').textContent    = data.nome;
  document.getElementById('csTelefone').textContent = data.telefone || '—';
  document.getElementById('csEmail').textContent   = data.email ? '✉ ' + data.email : '';
  document.getElementById('clienteSelecionado').classList.remove('d-none');
  document.getElementById('clienteErro').classList.add('d-none');
}

document.getElementById('buscaCliente').addEventListener('input', function() {
  const termo = this.value.toLowerCase();
  document.querySelectorAll('.cliente-row').forEach(row => {
    const texto = (row.dataset.nome + row.dataset.telefone + row.dataset.email).toLowerCase();
    row.style.display = texto.includes(termo) ? '' : 'none';
  });
});

// ============================================================
// Passo 2 — Produtos
// ============================================================
function adicionarItem() {
  const sel = document.getElementById('selProduto');
  const qtdInput = document.getElementById('inpQtd');
  const pid = parseInt(sel.value);
  const qtd = parseInt(qtdInput.value);

  if (!pid) { sel.classList.add('is-invalid'); return; }
  sel.classList.remove('is-invalid');
  if (!qtd || qtd < 1) { qtdInput.classList.add('is-invalid'); return; }
  qtdInput.classList.remove('is-invalid');

  // Atualiza se já existe
  const existente = itensSelecionados.find(i => i.produtoId === pid);
  if (existente) {
    existente.quantidade += qtd;
    renderizarTabela();
  } else {
    const p = produtoMap[pid];
    itensSelecionados.push({ produtoId: pid, nome: p.nome, quantidade: qtd, preco: p.preco });
    renderizarTabela();
  }
  sel.value = '';
  qtdInput.value = 1;
  document.getElementById('itensErro').classList.add('d-none');
}

function removerItem(idx) {
  itensSelecionados.splice(idx, 1);
  renderizarTabela();
}

function renderizarTabela() {
  const corpo = document.getElementById('corpoItens');
  corpo.innerHTML = '';
  let total = 0;

  if (itensSelecionados.length === 0) {
    corpo.innerHTML = '<tr id="linhaVazia"><td colspan="5" class="text-center text-muted py-3 small">' +
      '<i class="bi bi-inbox me-1"></i>Nenhum produto adicionado ainda.</td></tr>';
  } else {
    itensSelecionados.forEach((item, idx) => {
      const sub = item.quantidade * item.preco;
      total += sub;
      corpo.innerHTML += `
        <tr>
          <td>${item.nome}</td>
          <td class="text-center">${item.quantidade}</td>
          <td class="text-end">R$ ${formatarMoeda(item.preco)}</td>
          <td class="text-end fw-semibold">R$ ${formatarMoeda(sub)}</td>
          <td class="text-center">
            <button type="button" class="btn btn-outline-danger btn-sm py-0 px-1"
                    data-remover="${idx}"><i class="bi bi-trash"></i></button>
          </td>
        </tr>`;
    });
  }
  document.getElementById('totalGeral').textContent = 'R$ ' + formatarMoeda(total);
}

// ============================================================
// Navegação entre passos
// ============================================================
function irPara(passo) {
  // Validações antes de avançar
  if (passo > 1 && !clienteSelecionadoId) {
    document.getElementById('clienteErro').classList.remove('d-none');
    irParaStep(1); return;
  }
  if (passo > 2 && itensSelecionados.length === 0) {
    document.getElementById('itensErro').classList.remove('d-none');
    irParaStep(2); return;
  }
  if (passo === 4) {
    const inpData = document.getElementById('inpDataEntrega');
    if (inpData.value && inpData.value < inpData.min) {
      inpData.classList.add('is-invalid');
      irParaStep(3); return;
    }
    inpData.classList.remove('is-invalid');
    construirResumo();
  }
  irParaStep(passo);
}

function irParaStep(passo) {
  document.querySelectorAll('.wizard-step').forEach(el => el.classList.add('d-none'));
  document.getElementById('step' + passo).classList.remove('d-none');
  stepAtual = passo;
  atualizarIndicadores(passo);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function atualizarIndicadores(atual) {
  document.querySelectorAll('.step-indicator').forEach(el => {
    const s = parseInt(el.dataset.step);
    el.classList.remove('active', 'done');
    if (s === atual) { el.classList.add('active'); el.setAttribute('aria-current', 'step'); }
    else {
      el.removeAttribute('aria-current');
      if (s < atual) el.classList.add('done');
    }
  });
  // Linhas de progresso
  document.querySelectorAll('.step-line').forEach((line, i) => {
    line.style.background = (i < atual - 1) ? '#198754' : '#dee2e6';
  });
}

// ============================================================
// Passo 4 — Resumo
// ============================================================
function construirResumo() {
  // Preenche campos ocultos
  document.getElementById('hClienteId').value = clienteSelecionadoId;
  document.getElementById('hDataEntrega').value = document.getElementById('inpDataEntrega').value;
  document.getElementById('hSlotEntrega').value  = document.getElementById('inpSlotEntrega').value;
  document.getElementById('hObservacoes').value  = document.getElementById('inpObservacoes').value;

  // Remove campos de produto anteriores (se o usuário voltou)
  document.querySelectorAll('[data-wizard-item]').forEach(el => el.remove());

  // Adiciona novos campos de produto ao form
  const form = document.getElementById('wizardForm');
  itensSelecionados.forEach(item => {
    const inp1 = document.createElement('input');
    inp1.type = 'hidden'; inp1.name = 'produtoId';
    inp1.value = item.produtoId; inp1.dataset.wizardItem = '1';
    form.appendChild(inp1);

    const inp2 = document.createElement('input');
    inp2.type = 'hidden'; inp2.name = 'quantidade';
    inp2.value = item.quantidade; inp2.dataset.wizardItem = '1';
    form.appendChild(inp2);
  });

  // Resumo — cliente
  document.getElementById('sumNome').textContent = clienteSelecionadoNome;
  const contato = [clienteSelecionadoTelefone, clienteSelecionadoEmail].filter(Boolean).join(' | ');
  document.getElementById('sumContato').textContent = contato || '—';

  // Resumo — entrega
  const dt = document.getElementById('inpDataEntrega').value;
  const hr = document.getElementById('inpSlotEntrega').value;
  const obs = document.getElementById('inpObservacoes').value;
  document.getElementById('sumEntrega').textContent =
    (dt ? formatarData(dt) : 'A definir') + (hr ? ' às ' + hr : '');
  document.getElementById('sumObservacoes').textContent = obs || '';

  // Resumo — itens
  const tbody = document.getElementById('sumItens');
  tbody.innerHTML = '';
  let total = 0;
  itensSelecionados.forEach(item => {
    const sub = item.quantidade * item.preco;
    total += sub;
    tbody.innerHTML += `<tr>
      <td>${item.nome}</td>
      <td class="text-center">${item.quantidade}</td>
      <td class="text-end">R$ ${formatarMoeda(item.preco)}</td>
      <td class="text-end fw-semibold">R$ ${formatarMoeda(sub)}</td>
    </tr>`;
  });
  document.getElementById('sumTotal').textContent = 'R$ ' + formatarMoeda(total);
}

// ============================================================
// Utilitários
// ============================================================
function formatarMoeda(valor) {
  return parseFloat(valor).toFixed(2).replace('.', ',');
}

function formatarData(iso) {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

document.addEventListener('click', function (e) {
  var go = e.target.closest('[data-goto]');
  if (go) return irPara(parseInt(go.dataset.goto));
  if (e.target.closest('[data-add-item]')) return adicionarItem();
  var rm = e.target.closest('[data-remover]');
  if (rm) removerItem(parseInt(rm.dataset.remover));
});
