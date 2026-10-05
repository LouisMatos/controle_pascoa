(function () {
const canvas = document.getElementById('gastosChart');
if (!canvas) return;
const lista = (v, sep) => v ? v.split(sep) : [];
const chartLabels = lista(canvas.dataset.labels, '|');
const chartRealizado = lista(canvas.dataset.realizado, ',').map(Number);
const chartOrcado = lista(canvas.dataset.orcado, ',').map(Number);

if (chartLabels.length > 0) {
    const ctx = document.getElementById('gastosChart').getContext('2d');
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: chartLabels,
            datasets: [
                {
                    label: 'Realizado',
                    data: chartRealizado,
                    backgroundColor: 'rgba(220, 53, 69, 0.75)',
                    borderColor: 'rgba(220, 53, 69, 1)',
                    borderWidth: 1
                },
                {
                    label: 'Orçado',
                    data: chartOrcado,
                    backgroundColor: 'rgba(13, 110, 253, 0.35)',
                    borderColor: 'rgba(13, 110, 253, 0.8)',
                    borderWidth: 1
                }
            ]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { position: 'top' },
                tooltip: {
                    callbacks: {
                        label: function(ctx) {
                            return ' R$ ' + ctx.parsed.y.toLocaleString('pt-BR', {minimumFractionDigits: 2});
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(v) { return 'R$ ' + v.toLocaleString('pt-BR'); }
                    }
                }
            }
        }
    });
}
})();
