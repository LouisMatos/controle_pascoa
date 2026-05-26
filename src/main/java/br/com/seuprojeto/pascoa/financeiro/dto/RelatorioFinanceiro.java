package br.com.seuprojeto.pascoa.financeiro.dto;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RelatorioFinanceiro {

    // Financeiro
    private BigDecimal faturamentoEntregues;
    private BigDecimal totalRecebido;
    private BigDecimal pipeline;
    private BigDecimal totalGastosVariaveisMes; // Gastos variáveis do mês atual
    private BigDecimal totalDespesasFixasMensais; // Despesas fixas mensais cadastradas

    // Contagem por status
    private long totalPedidos;
    private long pedidosNovos;
    private long pedidosConfirmados;
    private long pedidosEmProducao;
    private long pedidosProntos;
    private long pedidosEntregues;
    private long pedidosCancelados;

    // Estoque
    private List<MateriaPrima> materiaPrimasCriticas;

    // Ranking e margens
    private List<TopProdutoDto> topProdutos;
    private List<MargemProdutoDto> margens;
}
