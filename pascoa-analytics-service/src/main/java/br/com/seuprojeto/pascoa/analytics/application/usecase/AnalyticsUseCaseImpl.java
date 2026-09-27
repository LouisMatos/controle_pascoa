package br.com.seuprojeto.pascoa.analytics.application.usecase;

import br.com.seuprojeto.pascoa.analytics.application.port.in.AnalyticsUseCase;
import br.com.seuprojeto.pascoa.analytics.application.port.out.RegistroVendaRepositoryPort;
import br.com.seuprojeto.pascoa.analytics.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalyticsUseCaseImpl implements AnalyticsUseCase {

    private final RegistroVendaRepositoryPort repository;

    @Override
    public ComparativoSafra comparativoSafras(int anoAtual, int anoAnterior) {
        MetricaSafra atual    = resumoSafra(anoAtual);
        MetricaSafra anterior = resumoSafra(anoAnterior);
        return ComparativoSafra.de(atual, anterior);
    }

    @Override
    public List<RankingProduto> rankingProdutos(int ano, int limite) {
        List<RegistroVenda> registros = repository.findByAno(ano);
        return RankingProduto.de(registros, limite);
    }

    @Override
    public List<MetricaMensal> metricasMensais(int ano) {
        List<RegistroVenda> registros = repository.findByAno(ano);
        return IntStream.rangeClosed(1, 12)
                .mapToObj(mes -> MetricaMensal.de(mes, ano, registros))
                .toList();
    }

    @Override
    public MetricaSafra resumoSafra(int ano) {
        List<RegistroVenda> registros = repository.findByAno(ano);
        return MetricaSafra.de(ano, registros);
    }
}
