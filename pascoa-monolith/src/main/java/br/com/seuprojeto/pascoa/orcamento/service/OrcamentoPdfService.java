package br.com.seuprojeto.pascoa.orcamento.service;

import br.com.seuprojeto.pascoa.common.quantidade.Quantidades;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.entity.OrcamentoItem;
import br.com.seuprojeto.pascoa.shared.pdf.PdfKit;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OrcamentoPdfService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public byte[] gerar(Orcamento orc) {
        try (PdfKit pdf = new PdfKit()) {
            pdf.cabecalho("Orcamento #" + orc.getId());

            pdf.secao("CLIENTE E ORCAMENTO");
            PdfPTable infoTbl = pdf.infoTable();
            pdf.addInfo(infoTbl, "Cliente:",      orc.getCliente().getNome());
            pdf.addInfo(infoTbl, "Validade:",     orc.getValidade().format(PdfKit.DATE_FMT));
            pdf.addInfo(infoTbl, "Status:",       orc.getStatus().getDescricao());
            pdf.addInfo(infoTbl, "Data Criacao:", orc.getDataCriacao().format(PdfKit.DATETIME_FMT));
            pdf.addInfo(infoTbl, "Telefone:",     orc.getCliente().getTelefone());
            pdf.addInfo(infoTbl, "E-mail:",       orc.getCliente().getEmail());
            pdf.add(infoTbl);

            pdf.secao("ITENS DO ORCAMENTO");
            PdfPTable itensTbl = pdf.itensTable();
            for (OrcamentoItem item : orc.getItens()) {
                pdf.addTd(itensTbl, item.getProduto().getNome(),          Element.ALIGN_LEFT);
                pdf.addTd(itensTbl, Quantidades.formatar(item.getQuantidade(), item.getProduto().getUnidadeVenda()), Element.ALIGN_CENTER);
                pdf.addTd(itensTbl, PdfKit.brl(item.getPrecoUnitario()),  Element.ALIGN_RIGHT);
                pdf.addTd(itensTbl, PdfKit.brl(item.getSubtotal()),       Element.ALIGN_RIGHT);
            }
            pdf.addLinhaTotal(itensTbl, orc.getTotal());
            pdf.add(itensTbl);

            pdf.observacoes(orc.getObservacoes());

            if (orc.getTokenAprovacao() != null && orc.isPendente()) {
                pdf.link("Para aprovar ou recusar este orcamento, acesse:",
                         baseUrl + "/orcamento-publico/" + orc.getTokenAprovacao());
            }

            pdf.rodape();
            return pdf.finalizar();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erro ao gerar PDF do orcamento #" + orc.getId(), e);
        }
    }
}
