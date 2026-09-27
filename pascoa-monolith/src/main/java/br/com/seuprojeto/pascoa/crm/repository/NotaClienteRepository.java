package br.com.seuprojeto.pascoa.crm.repository;

import br.com.seuprojeto.pascoa.crm.entity.NotaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotaClienteRepository extends JpaRepository<NotaCliente, Long> {

    List<NotaCliente> findByClienteIdOrderByCriadoEmDesc(Long clienteId);
}
