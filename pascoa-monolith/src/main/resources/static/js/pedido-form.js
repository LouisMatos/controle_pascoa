    (function () {
      const buscaInput  = document.getElementById('buscaCliente');
      const selectEl    = document.getElementById('clienteSelect');
      if (!buscaInput || !selectEl) return;

      // Snapshot all options on page-load (including the empty placeholder)
      const allOptions = Array.from(selectEl.options).map(o => ({
        value: o.value,
        text:  o.text
      }));

      buscaInput.addEventListener('input', function () {
        const q       = this.value.trim().toLowerCase();
        const current = selectEl.value;

        // Rebuild visible options: always keep placeholder; filter the rest
        selectEl.innerHTML = '';
        allOptions.forEach(o => {
          if (o.value === '' || !q || o.text.toLowerCase().includes(q)) {
            selectEl.add(new Option(o.text, o.value));
          }
        });

        // Restore selection when it is still visible
        if (current) selectEl.value = current;

        // Auto-open as a list when filtering, collapse when empty
        selectEl.size = q ? Math.min(allOptions.length, 8) : 1;
      });

      // Collapse when user selects
      selectEl.addEventListener('change', function () {
        buscaInput.value = '';
        selectEl.size    = 1;
      });
    })();
  