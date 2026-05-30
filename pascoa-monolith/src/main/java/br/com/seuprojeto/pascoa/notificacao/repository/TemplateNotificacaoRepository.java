package br.com.seuprojeto.pascoa.notificacao.repository;

import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.TemplateNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateNotificacaoRepository extends JpaRepository<TemplateNotificacao, Long> {

    List<TemplateNotificacao> findAllByOrderByEventoGatilhoAscCanalAsc();

    Optional<TemplateNotificacao> findByEventoGatilhoAndCanalAndAtivoTrue(
            EventoNotificacao evento, CanalNotificacao canal);
}
