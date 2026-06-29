/* ═══════════════════════════════════════════════════════════════════
   FoodFlow Kanban Controller v7
   Sortable.js drag-and-drop entre colunas. Ao soltar um card em outra
   coluna, submete o formulário POST embutido no card correspondente
   à transição (iniciar / concluir). Drops inválidos são revertidos.

   Estrutura DOM esperada:
     .ff-kanban
       .ff-kanban__column[data-col="pendente|em-andamento|concluida"]
         .ff-kanban__list
           .ff-kanban__card[data-id="<id>"]
             form.kanban-transition[data-action="iniciar|concluir"]
   ═══════════════════════════════════════════════════════════════════ */
(function () {
  'use strict';

  // Mapa: (origem, destino) → action permitida
  var TRANSITIONS = {
    'pendente:em-andamento':   'iniciar',
    'em-andamento:concluida':  'concluir'
  };

  function init() {
    if (typeof Sortable === 'undefined') {
      console.warn('[FFKanban] Sortable.js não carregado — drag-and-drop desabilitado.');
      return;
    }
    document.querySelectorAll('.ff-kanban__list').forEach(function (list) {
      Sortable.create(list, {
        group:    'kanban',
        animation: 180,
        ghostClass: 'sortable-ghost',
        chosenClass: 'sortable-chosen',
        // M-06 — opções específicas para touch (iOS/Android):
        //  - delay 150ms só em touch evita iniciar drag por engano enquanto
        //    o usuário tenta rolar o Kanban horizontalmente;
        //  - touchStartThreshold 5px diferencia tap acidental de drag real;
        //  - fallbackTolerance 5px reduz "tremor" de toque inicial.
        delay: 150,
        delayOnTouchOnly: true,
        touchStartThreshold: 5,
        fallbackTolerance: 5,
        onAdd: function (evt) {
          var fromCol = evt.from.closest('.ff-kanban__column').dataset.col;
          var toCol   = evt.to.closest('.ff-kanban__column').dataset.col;
          var key     = fromCol + ':' + toCol;
          var action  = TRANSITIONS[key];

          if (!action) {
            // Transição inválida — reverte
            evt.from.insertBefore(evt.item, evt.from.children[evt.oldIndex]);
            if (window.ffToast) ffToast('warning', 'Transição inválida nesta direção.');
            return;
          }

          // Encontra o form correspondente à action e submete
          var form = evt.item.querySelector('form.kanban-transition[data-action="' + action + '"]');
          if (form) {
            // Adiciona efeito visual de loading no card
            evt.item.style.opacity = '0.55';
            evt.item.style.pointerEvents = 'none';
            form.submit();
          } else {
            // Sem form: reverte
            evt.from.insertBefore(evt.item, evt.from.children[evt.oldIndex]);
            if (window.ffToast) ffToast('danger', 'Ação não encontrada no card.');
          }
        }
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
