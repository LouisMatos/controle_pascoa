package br.com.seuprojeto.pascoa.financial.application.port.in;

import br.com.seuprojeto.pascoa.financial.domain.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinanceiroUseCase {

    record RegistrarLancamentoCommand(
            TipoLancamento tipo,
            CategoriaLancamento categoria,
            String descricao,
            BigDecimal valor,
            LocalDate data,
            String referenciaId
    ) {}

    Lancamento registrar(RegistrarLancamentoCommand command);

    Lancamento buscarPorId(Long id);

    List<Lancamento> listar(int mes, int ano);

    List<Lancamento> listarPorPeriodo(LocalDate inicio, LocalDate fim);

    ResumoFinanceiro resumo(int mes, int ano);

    DreAnual dre(int ano);
}
