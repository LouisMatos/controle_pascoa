package br.com.seuprojeto.pascoa.notificacao.repository;

import br.com.seuprojeto.pascoa.notificacao.entity.NotificacaoEnviada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificacaoEnviadaRepository extends JpaRepository<NotificacaoEnviada, Long> {

    @Query("SELECT n FROM NotificacaoEnviada n JOIN FETCH n.pedido p JOIN FETCH p.cliente " +
           "ORDER BY n.dataEnvio DESC")
    List<NotificacaoEnviada> findAllComPedido();

    List<NotificacaoEnviada> findByPedidoIdOrderByDataEnvioDesc(Long pedidoId);
}
