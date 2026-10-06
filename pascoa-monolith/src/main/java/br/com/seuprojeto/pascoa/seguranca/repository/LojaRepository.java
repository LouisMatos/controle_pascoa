package br.com.seuprojeto.pascoa.seguranca.repository;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LojaRepository extends JpaRepository<Loja, Long> {
}
