package br.com.seuprojeto.pascoa.estoque.service;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import br.com.seuprojeto.pascoa.cadastro.repository.MateriaPrimaRepository;
import br.com.seuprojeto.pascoa.estoque.dto.AjusteEstoqueForm;
import br.com.seuprojeto.pascoa.estoque.dto.EntradaEstoqueForm;
import br.com.seuprojeto.pascoa.estoque.entity.MovimentacaoEstoque;
import br.com.seuprojeto.pascoa.estoque.entity.TipoMovimentacao;
import br.com.seuprojeto.pascoa.estoque.repository.MovimentacaoEstoqueRepository;
import br.com.seuprojeto.pascoa.shared.exception.EstoqueInsuficienteException;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final MateriaPrimaRepository mpRepository;

    // -----------------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> listarTodas() {
        return movimentacaoRepository.findAllByOrderByDataDesc();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> filtrar(Long mpId, TipoMovimentacao tipo) {
        return movimentacaoRepository.filtrar(mpId, tipo);
    }

    // -----------------------------------------------------------------------
    // ENTRADA — compra de matéria-prima
    // -----------------------------------------------------------------------

    @Transactional
    public void registrarEntrada(EntradaEstoqueForm form) {
        MateriaPrima mp = carregarMp(form.getMateriaPrimaId());

        BigDecimal novoSaldo = mp.getQuantidadeAtual().add(form.getQuantidade());
        mp.setQuantidadeAtual(novoSaldo);
        mp.setCustoUnitario(form.getCustoUnitario()); // atualiza para o preço mais recente
        mpRepository.save(mp);

        movimentacaoRepository.save(MovimentacaoEstoque.builder()
            .materiaPrima(mp)
            .tipo(TipoMovimentacao.ENTRADA)
            .quantidade(form.getQuantidade())
            .saldoApos(novoSaldo)
            .custoUnitario(form.getCustoUnitario())
            .motivo(form.getMotivo())
            .usuario(usuarioAtual())
            .build());
    }

    // -----------------------------------------------------------------------
    // SAIDA — consumo na produção (chamado pelo ProducaoService na Etapa 6)
    // -----------------------------------------------------------------------

    @Transactional
    public void registrarSaida(Long materiaPrimaId, BigDecimal quantidade, String motivo) {
        MateriaPrima mp = carregarMp(materiaPrimaId);

        if (mp.getQuantidadeAtual().compareTo(quantidade) < 0) {
            throw new EstoqueInsuficienteException(
                "Estoque insuficiente de '" + mp.getNome() + "'. " +
                "Disponível: " + mp.getQuantidadeAtual() + " " + mp.getUnidade().getSimbolo() +
                " | Necessário: " + quantidade
            );
        }

        BigDecimal novoSaldo = mp.getQuantidadeAtual().subtract(quantidade);
        mp.setQuantidadeAtual(novoSaldo);
        mpRepository.save(mp);

        movimentacaoRepository.save(MovimentacaoEstoque.builder()
            .materiaPrima(mp)
            .tipo(TipoMovimentacao.SAIDA)
            .quantidade(quantidade)
            .saldoApos(novoSaldo)
            .motivo(motivo)
            .usuario(usuarioAtual())
            .build());
    }

    // -----------------------------------------------------------------------
    // AJUSTE — correção manual de inventário
    // -----------------------------------------------------------------------

    @Transactional
    public void registrarAjuste(AjusteEstoqueForm form) {
        MateriaPrima mp = carregarMp(form.getMateriaPrimaId());

        BigDecimal anterior = mp.getQuantidadeAtual();
        BigDecimal nova = form.getNovaQuantidade();
        BigDecimal delta = nova.subtract(anterior); // positivo = aumento, negativo = redução

        mp.setQuantidadeAtual(nova);
        mpRepository.save(mp);

        String motivoCompleto = String.format("Ajuste de inventário | Anterior: %s | Novo: %s | Motivo: %s",
            anterior, nova, form.getMotivo());

        movimentacaoRepository.save(MovimentacaoEstoque.builder()
            .materiaPrima(mp)
            .tipo(TipoMovimentacao.AJUSTE)
            .quantidade(delta)         // delta com sinal
            .saldoApos(nova)
            .motivo(motivoCompleto)
            .usuario(usuarioAtual())
            .build());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private MateriaPrima carregarMp(Long id) {
        return mpRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Matéria-prima não encontrada: " + id));
    }

    /** Retorna o login do usuário autenticado ou "Sistema" quando não há sessão (jobs, testes). */
    private String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "Sistema";
    }
}
