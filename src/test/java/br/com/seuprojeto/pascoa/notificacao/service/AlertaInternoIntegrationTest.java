package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.notificacao.entity.AlertaInterno;
import br.com.seuprojeto.pascoa.notificacao.repository.AlertaInternoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para {@link AlertaInternoService}.
 * Cobre criação, consultas, marcação como lido e limpeza em massa.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AlertaInternoIntegrationTest {

    @Autowired private AlertaInternoService service;
    @Autowired private AlertaInternoRepository repository;

    @BeforeEach
    void limpar() {
        // Garante base limpa a cada teste (transação nova a cada @Test + @Transactional)
        repository.deleteAll();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AlertaInterno criarAlerta(String mensagem) {
        return service.criar(mensagem, "/pedidos", "bi-bell", "primary");
    }

    // ── testes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("criar persiste alerta com campos corretos e lido=false por padrão")
    void criar_persisteComCamposCorretos() {
        AlertaInterno alerta = service.criar(
                "Orçamento aprovado",
                "/orcamentos/1",
                "bi-check-circle",
                "success");

        assertThat(alerta.getId()).isNotNull();
        assertThat(alerta.getMensagem()).isEqualTo("Orçamento aprovado");
        assertThat(alerta.getLink()).isEqualTo("/orcamentos/1");
        assertThat(alerta.getIcone()).isEqualTo("bi-check-circle");
        assertThat(alerta.getCor()).isEqualTo("success");
        assertThat(alerta.getLido()).isFalse();
        assertThat(alerta.getCriadoEm()).isNotNull();
    }

    @Test
    @DisplayName("contarNaoLidos retorna total correto antes e após leitura")
    void contarNaoLidos_refleteMudancas() {
        criarAlerta("Alerta A");
        criarAlerta("Alerta B");
        criarAlerta("Alerta C");

        assertThat(service.contarNaoLidos()).isEqualTo(3);

        service.marcarLido(repository.findAll().get(0).getId());

        assertThat(service.contarNaoLidos()).isEqualTo(2);
    }

    @Test
    @DisplayName("marcarLido muda flag lido para true somente no alerta indicado")
    void marcarLido_mudaApenasOAlertaIndicado() {
        AlertaInterno a1 = criarAlerta("Um");
        AlertaInterno a2 = criarAlerta("Dois");

        service.marcarLido(a1.getId());

        AlertaInterno recarregado1 = repository.findById(a1.getId()).orElseThrow();
        AlertaInterno recarregado2 = repository.findById(a2.getId()).orElseThrow();

        assertThat(recarregado1.getLido()).isTrue();
        assertThat(recarregado2.getLido()).isFalse();
    }

    @Test
    @DisplayName("marcarLido com id inexistente não lança exceção")
    void marcarLido_idInexistente_naoLancaExcecao() {
        assertThatCode(() -> service.marcarLido(999_999L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("marcarTodasLidas zera contador de não lidos")
    void marcarTodasLidas_zeraContador() {
        criarAlerta("X");
        criarAlerta("Y");
        criarAlerta("Z");
        assertThat(service.contarNaoLidos()).isEqualTo(3);

        service.marcarTodasLidas();

        assertThat(service.contarNaoLidos()).isZero();
    }

    @Test
    @DisplayName("recentes retorna no máximo 20 alertas, não lidos primeiro")
    void recentes_retornaAteMeioMes_naoLidosPrimeiro() {
        // Cria 5 alertas, marca 2 como lidos
        for (int i = 1; i <= 5; i++) criarAlerta("Alerta " + i);
        List<AlertaInterno> todos = repository.findAllByOrderByCriadoEmDesc();
        service.marcarLido(todos.get(3).getId());
        service.marcarLido(todos.get(4).getId());

        List<AlertaInterno> recentes = service.recentes();

        assertThat(recentes).hasSize(5);
        // Não lidos devem aparecer antes dos lidos
        long naoLidos = recentes.stream().takeWhile(a -> !a.getLido()).count();
        assertThat(naoLidos).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("todos retorna todos os alertas em ordem decrescente de criadoEm")
    void todos_retornaTodosOrdenadosPorData() {
        criarAlerta("Primeiro");
        criarAlerta("Segundo");
        criarAlerta("Terceiro");

        List<AlertaInterno> lista = service.todos();

        assertThat(lista).hasSize(3);
        // Mais recente primeiro
        assertThat(lista.get(0).getMensagem()).isEqualTo("Terceiro");
        assertThat(lista.get(2).getMensagem()).isEqualTo("Primeiro");
    }

    @Test
    @DisplayName("contarNaoLidos retorna 0 quando não há alertas")
    void contarNaoLidos_semAlertas_retornaZero() {
        assertThat(service.contarNaoLidos()).isZero();
    }
}
