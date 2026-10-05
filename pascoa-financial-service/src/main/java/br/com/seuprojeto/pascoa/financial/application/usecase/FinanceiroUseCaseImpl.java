package br.com.seuprojeto.pascoa.financial.application.usecase;

import br.com.seuprojeto.pascoa.financial.application.port.in.FinanceiroUseCase;
import br.com.seuprojeto.pascoa.financial.application.port.out.LancamentoRepositoryPort;
import br.com.seuprojeto.pascoa.financial.domain.exception.LancamentoNotFoundException;
import br.com.seuprojeto.pascoa.financial.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
@RequiredArgsConstructor
public class FinanceiroUseCaseImpl implements FinanceiroUseCase {

    private final LancamentoRepositoryPort repository;

    @Override
    public Lancamento registrar(RegistrarLancamentoCommand cmd) {
        Lancamento lancamento = Lancamento.builder()
                .tipo(cmd.tipo())
                .categoria(cmd.categoria())
                .descricao(cmd.descricao())
                .valor(cmd.valor())
                .data(cmd.data() != null ? cmd.data() : LocalDate.now())
                .referenciaId(cmd.referenciaId())
                .origem("manual")
                .build();
        return repository.save(lancamento);
    }

    @Override
    @Transactional(readOnly = true)
    public Lancamento buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new LancamentoNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lancamento> listar(int mes, int ano) {
        return repository.findByMesAno(mes, ano);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lancamento> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return repository.findByPeriodo(inicio, fim);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoFinanceiro resumo(int mes, int ano) {
        List<Lancamento> lancamentos = repository.findByMesAno(mes, ano);
        return ResumoFinanceiro.de(mes, ano, lancamentos);
    }

    @Override
    @Transactional(readOnly = true)
    public DreAnual dre(int ano) {
        List<Lancamento> todos = repository.findByAno(ano);

        List<ResumoFinanceiro> porMes = IntStream.rangeClosed(1, 12)
                .mapToObj(mes -> {
                    int mesInt = mes;
                    List<Lancamento> doMes = todos.stream()
                            .filter(l -> l.getData().getMonthValue() == mesInt)
                            .collect(Collectors.toList());
                    return ResumoFinanceiro.de(mes, ano, doMes);
                })
                .collect(Collectors.toList());

        return DreAnual.de(ano, todos, porMes);
    }
}
