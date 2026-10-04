(function () {
    const canvas = document.getElementById('chartMensal');
    if (!canvas) return;
    const labels = canvas.dataset.labels, fatAtual = canvas.dataset.atual, fatAnt = canvas.dataset.anterior;
    const anoSel = Number(canvas.dataset.anoSel), anoAnt = Number(canvas.dataset.anoAnt);

    const labelsArr   = JSON.parse(labels);
    const fatAtualArr = JSON.parse(fatAtual);
    const fatAntArr   = JSON.parse(fatAnt);

    const datasets = [
        {
            label: 'Faturamento ' + anoSel,
            data: fatAtualArr,
            borderColor: '#0d6efd',
            backgroundColor: 'rgba(13,110,253,.15)',
            fill: true,
            tension: 0.3,
        }
    ];

    if (anoAnt > 0 && fatAntArr.length > 0) {
        datasets.push({
            label: 'Faturamento ' + anoAnt,
            data: fatAntArr,
            borderColor: '#adb5bd',
            backgroundColor: 'rgba(173,181,189,.10)',
            fill: false,
            tension: 0.3,
            borderDash: [5, 5],
        });
    }

    new Chart(document.getElementById('chartMensal'), {
        type: 'line',
        data: { labels: labelsArr, datasets },
        options: {
            responsive: true,
            plugins: {
                legend: { position: 'top' },
                tooltip: {
                    callbacks: {
                        label: function(ctx) {
                            return ctx.dataset.label + ': R$ ' +
                                Number(ctx.parsed.y).toLocaleString('pt-BR', {minimumFractionDigits:2});
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(v) { return 'R$ ' + Number(v).toLocaleString('pt-BR'); }
                    }
                }
            }
        }
    });
})();
