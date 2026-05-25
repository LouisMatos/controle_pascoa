package br.com.seuprojeto.pascoa.fichaTecnica.service;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.Unidade;
import br.com.seuprojeto.pascoa.cadastro.repository.MateriaPrimaRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnicaItem;
import br.com.seuprojeto.pascoa.fichaTecnica.repository.FichaTecnicaItemRepository;
import br.com.seuprojeto.pascoa.fichaTecnica.repository.FichaTecnicaRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FichaTecnicaService {

    private final FichaTecnicaRepository fichaRepository;
    private final FichaTecnicaItemRepository itemRepository;
    private final ProdutoRepository produtoRepository;
    private final MateriaPrimaRepository mpRepository;

    @Transactional(readOnly = true)
    public FichaTecnica buscarPorProduto(Long produtoId) {
        return fichaRepository.findByProdutoIdComItens(produtoId)
            .orElse(null);
    }

    @Transactional
    public FichaTecnica buscarOuCriar(Long produtoId) {
        return fichaRepository.findByProdutoId(produtoId).orElseGet(() -> {
            Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + produtoId));
            return fichaRepository.save(FichaTecnica.builder()
                .produto(produto)
                .rendimento(BigDecimal.ONE)
                .unidadeRendimento(Unidade.UN)
                .build());
        });
    }

    @Transactional
    public void salvarInfo(Long produtoId, BigDecimal rendimento, Unidade unidade, String observacoes) {
        FichaTecnica ficha = buscarOuCriar(produtoId);
        ficha.setRendimento(rendimento);
        ficha.setUnidadeRendimento(unidade);
        ficha.setObservacoes(observacoes);
        fichaRepository.save(ficha);
    }

    @Transactional
    public void adicionarItem(Long produtoId, Long mpId, BigDecimal quantidade) {
        FichaTecnica ficha = buscarOuCriar(produtoId);

        if (itemRepository.existsByFichaTecnicaIdAndMateriaPrimaId(ficha.getId(), mpId)) {
            throw new IllegalArgumentException("Esta matéria-prima já está na ficha técnica. Remova-a antes de adicionar novamente.");
        }

        MateriaPrima mp = mpRepository.findById(mpId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Matéria-prima não encontrada: " + mpId));

        itemRepository.save(FichaTecnicaItem.builder()
            .fichaTecnica(ficha)
            .materiaPrima(mp)
            .quantidade(quantidade)
            .build());
    }

    @Transactional
    public void removerItem(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new RecursoNaoEncontradoException("Item não encontrado: " + itemId);
        }
        itemRepository.deleteById(itemId);
    }

    public BigDecimal calcularCustoTotal(FichaTecnica ficha) {
        if (ficha == null || ficha.getItens().isEmpty()) return BigDecimal.ZERO;
        return ficha.getItens().stream()
            .map(FichaTecnicaItem::getCustoItem)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularCustoPorUnidade(FichaTecnica ficha) {
        BigDecimal custoTotal = calcularCustoTotal(ficha);
        if (custoTotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (ficha.getRendimento().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return custoTotal.divide(ficha.getRendimento(), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularMargemLucro(BigDecimal precoVenda, BigDecimal custoPorUnidade) {
        if (precoVenda == null || precoVenda.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (custoPorUnidade.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("100.00");
        return precoVenda.subtract(custoPorUnidade)
            .divide(precoVenda, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
