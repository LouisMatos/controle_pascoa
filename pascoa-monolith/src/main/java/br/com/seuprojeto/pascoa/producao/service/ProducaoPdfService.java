package br.com.seuprojeto.pascoa.producao.service;

import br.com.seuprojeto.pascoa.common.quantidade.Quantidades;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.shared.pdf.PdfKit;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class ProducaoPdfService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    public byte[] gerar(OrdemProducao ordem, FichaTecnica ficha, ProducaoService.ReceitaCalculada receita) {
        try (PdfKit pdf = new PdfKit()) {
            pdf.cabecalho("Ordem de Producao #" + ordem.getId());

            pdf.secao("ORDEM");
            PdfPTable info = pdf.infoTable();
            pdf.addInfo(info, "Produto:", ordem.getProduto().getNome());
            pdf.addInfo(info, "Quantidade:", Quantidades.formatar(ordem.getQuantidade(), ordem.getProduto().getUnidadeVenda()));
            pdf.addInfo(info, "Status:", ordem.getStatus().getDescricao());
            if (ordem.getPedido() != null) {
                pdf.addInfo(info, "Pedido:", "#" + ordem.getPedido().getId());
            }
            pdf.addInfo(info, "Abertura:", ordem.getDataAbertura().format(PdfKit.DATETIME_FMT));
            if (ordem.getDataConclusao() != null) {
                pdf.addInfo(info, "Conclusao:", ordem.getDataConclusao().format(PdfKit.DATETIME_FMT));
            }
            if (ficha != null) {
                pdf.addInfo(info, "Rendimento da receita:",
                    qtd(ficha.getRendimento()) + " " + ordem.getProduto().getUnidadeVenda().getSimbolo());
            }
            pdf.add(info);

            pdf.secao("RECEITA E CUSTO DOS INSUMOS");
            if (receita == null) {
                pdf.observacoes("Produto sem ficha tecnica cadastrada.");
            } else {
                PdfPTable tbl = new PdfPTable(5);
                tbl.setWidthPercentage(100f);
                tbl.setWidths(new float[]{34f, 16f, 16f, 17f, 17f});
                tbl.setSpacingAfter(12f);
                pdf.addTh(tbl, "Insumo", Element.ALIGN_LEFT);
                pdf.addTh(tbl, "Na receita", Element.ALIGN_RIGHT);
                pdf.addTh(tbl, "Necessario", Element.ALIGN_RIGHT);
                pdf.addTh(tbl, "Custo unit.", Element.ALIGN_RIGHT);
                pdf.addTh(tbl, "Custo", Element.ALIGN_RIGHT);
                for (ProducaoService.LinhaReceita l : receita.linhas()) {
                    pdf.addTd(tbl, l.nome(), Element.ALIGN_LEFT);
                    pdf.addTd(tbl, qtd(l.qtdReceita()) + " " + l.unidade(), Element.ALIGN_RIGHT);
                    pdf.addTd(tbl, qtd(l.qtdNecessaria()) + " " + l.unidade(), Element.ALIGN_RIGHT);
                    pdf.addTd(tbl, l.custoUnitario() == null ? "-" : PdfKit.brl(l.custoUnitario()), Element.ALIGN_RIGHT);
                    pdf.addTd(tbl, PdfKit.brl(l.custo()), Element.ALIGN_RIGHT);
                }
                totalRow(pdf, tbl, "CUSTO TOTAL", PdfKit.brl(receita.custoTotal()));
                totalRow(pdf, tbl, "Custo por unidade", PdfKit.brl(receita.custoPorUnidade()));
                pdf.add(tbl);
            }

            pdf.observacoes(ordem.getObservacoes());
            pdf.rodape("Ordem de producao - uso interno. Pascoa Artesanal.");
            return pdf.finalizar();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erro ao gerar PDF da ordem de producao #" + ordem.getId(), e);
        }
    }

    private static void totalRow(PdfKit pdf, PdfPTable tbl, String rotulo, String valor) {
        tbl.addCell(boldCell(pdf, rotulo, Element.ALIGN_LEFT));
        for (int i = 0; i < 3; i++) {
            tbl.addCell(boldCell(pdf, "", Element.ALIGN_RIGHT));
        }
        tbl.addCell(boldCell(pdf, valor, Element.ALIGN_RIGHT));
    }

    private static PdfPCell boldCell(PdfKit pdf, String texto, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, pdf.fNegrito));
        c.setHorizontalAlignment(align);
        c.setPadding(5f);
        c.setBorder(Rectangle.BOTTOM);
        return c;
    }

    private static String qtd(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(3);
        nf.setMaximumFractionDigits(3);
        return nf.format(v);
    }
}
