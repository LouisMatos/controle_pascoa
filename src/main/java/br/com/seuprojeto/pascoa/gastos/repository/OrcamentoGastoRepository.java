package br.com.seuprojeto.pascoa.gastos.repository;

import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import br.com.seuprojeto.pascoa.gastos.entity.OrcamentoGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrcamentoGastoRepository extends JpaRepository<OrcamentoGasto, Long> {

    List<OrcamentoGasto> findByReferenciaAnoAndReferenciaMesOrderByCategoria(int ano, int mes);

    Optional<OrcamentoGasto> findByCategoriaAndReferenciaAnoAndReferenciaMes(
            CategoriaGasto categoria, int ano, int mes);
}
