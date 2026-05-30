package br.com.seuprojeto.pascoa.analytics.application.port.in;

import br.com.seuprojeto.pascoa.analytics.domain.model.*;

import java.util.List;

public interface AnalyticsUseCase {

    ComparativoSafra comparativoSafras(int anoAtual, int anoAnterior);

    List<RankingProduto> rankingProdutos(int ano, int limite);

    List<MetricaMensal> metricasMensais(int ano);

    MetricaSafra resumoSafra(int ano);
}
