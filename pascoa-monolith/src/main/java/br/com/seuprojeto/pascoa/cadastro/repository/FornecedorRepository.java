package br.com.seuprojeto.pascoa.cadastro.repository;

import br.com.seuprojeto.pascoa.cadastro.entity.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findAllByOrderByNomeAsc();

    List<Fornecedor> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}
