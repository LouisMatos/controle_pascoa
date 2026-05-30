package br.com.seuprojeto.pascoa.financial.application.port.out;

import br.com.seuprojeto.pascoa.financial.domain.model.Lancamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LancamentoRepositoryPort {
    Lancamento save(Lancamento lancamento);
    Optional<Lancamento> findById(Long id);
    List<Lancamento> findByMesAno(int mes, int ano);
    List<Lancamento> findByPeriodo(LocalDate inicio, LocalDate fim);
    List<Lancamento> findByAno(int ano);
    boolean existsByReferenciaIdAndOrigem(String referenciaId, String origem);
}
