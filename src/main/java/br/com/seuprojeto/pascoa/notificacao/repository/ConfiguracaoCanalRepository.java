package br.com.seuprojeto.pascoa.notificacao.repository;

import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.ConfiguracaoCanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracaoCanalRepository extends JpaRepository<ConfiguracaoCanal, Long> {

    Optional<ConfiguracaoCanal> findByTipo(CanalNotificacao tipo);
}
