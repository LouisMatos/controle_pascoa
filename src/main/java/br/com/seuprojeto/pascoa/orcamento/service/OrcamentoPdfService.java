package br.com.seuprojeto.pascoa.orcamento.service;

import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.entity.OrcamentoItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class OrcamentoPdfService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale PT_BR = new Locale("pt", "BR");

    public byte[] gerar(Orcamento orc) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40f, 40f, 50f, 50f);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      "Cp1252", BaseFont.NOT_EMBEDDED);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, "Cp1252", BaseFont.NOT_EMBEDDED);

            Color verde    = new Color(45, 106, 79);
            Color cinzaSep = new Color(220, 220, 220);
            Color cinzaCl  = new Color(245, 245, 245);
            Color preto    = Color.BLACK;
            Color branco   = Color.WHITE;

            Font fTituloW = new Font(bfBold, 18, Font.NORMAL, branco);
            Font fSubW    = new Font(bf,     11, Font.NORMAL, branco);
            Font fSecao   = new Font(bfBold, 10, Font.NORMAL, verde);
            Font fNormal  = new Font(bf,     10, Font.NORMAL, preto);
            Font fNegrito = new Font(bfBold, 10, Font.NORMAL, preto);
            Font fPeq     = new Font(bf,      9, Font.NORMAL, new Color(100, 100, 100));
            Font fPeqBold = new Font(bfBold,  9, Font.NORMAL, preto);
            Font fThW     = new Font(bfBold,  9, Font.NORMAL, branco);

            // ── Cabeçalho ────────────────────────────────────────────────────
            PdfPTable hdrTbl = new PdfPTable(1);
            hdrTbl.setWidthPercentage(100f);
            hdrTbl.setSpacingAfter(14f);
            PdfPCell hCell = new PdfPCell();
            hCell.setBackgroundColor(verde);
            hCell.setPadding(14f);
            hCell.setBorder(Rectangle.NO_BORDER);

            Paragraph pTit = new Paragraph("Pascoa Artesanal", fTituloW);
            pTit.setAlignment(Element.ALIGN_CENTER);
            hCell.addElement(pTit);

            Paragraph pSub = new Paragraph("Orcamento #" + orc.getId(), fSubW);
            pSub.setAlignment(Element.ALIGN_CENTER);
            hCell.addElement(pSub);

            Paragraph pDt = new Paragraph("Gerado em " + LocalDateTime.now().format(DATETIME_FMT), fSubW);
            pDt.setAlignment(Element.ALIGN_CENTER);
            hCell.addElement(pDt);

            hdrTbl.addCell(hCell);
            doc.add(hdrTbl);

            // ── Dados do cliente e validade ───────────────────────────────────
            doc.add(new Paragraph("CLIENTE E ORCAMENTO", fSecao));
            doc.add(espaco());

            PdfPTable infoTbl = new PdfPTable(2);
            infoTbl.setWidthPercentage(100f);
            infoTbl.setWidths(new float[]{35f, 65f});
            infoTbl.setSpacingAfter(12f);

            addInfo(infoTbl, "Cliente:",      orc.getCliente().getNome(),              fPeqBold, fNormal, cinzaSep);
            addInfo(infoTbl, "Validade:",     orc.getValidade().format(DATE_FMT),      fPeqBold, fNormal, cinzaSep);
            addInfo(infoTbl, "Status:",       orc.getStatus().getDescricao(),          fPeqBold, fNormal, cinzaSep);
            addInfo(infoTbl, "Data Criacao:", orc.getDataCriacao().format(DATETIME_FMT), fPeqBold, fNormal, cinzaSep);
            if (orc.getCliente().getTelefone() != null)
                addInfo(infoTbl, "Telefone:", orc.getCliente().getTelefone(), fPeqBold, fNormal, cinzaSep);
            if (orc.getCliente().getEmail() != null)
                addInfo(infoTbl, "E-mail:", orc.getCliente().getEmail(), fPeqBold, fNormal, cinzaSep);

            doc.add(infoTbl);

            // ── Tabela de itens ───────────────────────────────────────────────
            doc.add(new Paragraph("ITENS DO ORCAMENTO", fSecao));
            doc.add(espaco());

            PdfPTable itensTbl = new PdfPTable(4);
            itensTbl.setWidthPercentage(100f);
            itensTbl.setWidths(new float[]{46f, 11f, 21f, 22f});
            itensTbl.setSpacingAfter(12f);

            addTh(itensTbl, "Produto",      Element.ALIGN_LEFT,   verde, fThW);
            addTh(itensTbl, "Qtd",          Element.ALIGN_CENTER, verde, fThW);
            addTh(itensTbl, "Preco Unit.",  Element.ALIGN_RIGHT,  verde, fThW);
            addTh(itensTbl, "Subtotal",     Element.ALIGN_RIGHT,  verde, fThW);

            for (OrcamentoItem item : orc.getItens()) {
                addTd(itensTbl, item.getProduto().getNome(),          Element.ALIGN_LEFT,   fNormal, cinzaSep);
                addTd(itensTbl, String.valueOf(item.getQuantidade()), Element.ALIGN_CENTER, fNormal, cinzaSep);
                addTd(itensTbl, brl(item.getPrecoUnitario()),         Element.ALIGN_RIGHT,  fNormal, cinzaSep);
                addTd(itensTbl, brl(item.getSubtotal()),              Element.ALIGN_RIGHT,  fNormal, cinzaSep);
            }

            PdfPCell totLbl = new PdfPCell(new Phrase("TOTAL:", fNegrito));
            totLbl.setColspan(3);
            totLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totLbl.setPadding(5f);
            totLbl.setBorder(Rectangle.TOP);
            itensTbl.addCell(totLbl);

            PdfPCell totVal = new PdfPCell(new Phrase(brl(orc.getTotal()), fNegrito));
            totVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totVal.setPadding(5f);
            totVal.setBorder(Rectangle.TOP);
            itensTbl.addCell(totVal);

            doc.add(itensTbl);

            // ── Observações ───────────────────────────────────────────────────
            if (orc.getObservacoes() != null && !orc.getObservacoes().isBlank()) {
                doc.add(new Paragraph("OBSERVACOES", fSecao));
                doc.add(espaco());
                Paragraph obs = new Paragraph(orc.getObservacoes(), fNormal);
                obs.setSpacingAfter(14f);
                doc.add(obs);
            }

            // ── Link de aprovação ─────────────────────────────────────────────
            if (orc.getTokenAprovacao() != null && orc.isPendente()) {
                String link = baseUrl + "/orcamento-publico/" + orc.getTokenAprovacao();
                doc.add(new Paragraph("Para aprovar ou recusar este orcamento, acesse:", fPeq));
                Font fLink = new Font(bf, 9, Font.UNDERLINE, verde);
                Paragraph linkPara = new Paragraph(link, fLink);
                linkPara.setSpacingAfter(10f);
                doc.add(linkPara);
            }

            // ── Rodapé ────────────────────────────────────────────────────────
            PdfPTable rodSep = new PdfPTable(1);
            rodSep.setWidthPercentage(100f);
            PdfPCell rodCell = new PdfPCell(new Phrase(" "));
            rodCell.setBorder(Rectangle.TOP);
            rodCell.setBorderColorTop(cinzaSep);
            rodCell.setPaddingBottom(4f);
            rodSep.addCell(rodCell);
            doc.add(rodSep);

            Paragraph rod = new Paragraph("Obrigado pela preferencia! Pascoa Artesanal.", fPeq);
            rod.setAlignment(Element.ALIGN_CENTER);
            doc.add(rod);

        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Erro ao gerar PDF do orcamento #" + orc.getId(), e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    private void addInfo(PdfPTable t, String lbl, String val, Font lf, Font vf, Color sep) {
        PdfPCell l = new PdfPCell(new Phrase(lbl, lf));
        l.setPadding(4f); l.setBorder(Rectangle.BOTTOM); l.setBorderColorBottom(sep);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(val != null ? val : "", vf));
        v.setPadding(4f); v.setBorder(Rectangle.BOTTOM); v.setBorderColorBottom(sep);
        t.addCell(v);
    }

    private void addTh(PdfPTable t, String text, int align, Color bg, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg); c.setHorizontalAlignment(align);
        c.setPadding(6f); c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
    }

    private void addTd(PdfPTable t, String text, int align, Font f, Color sep) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align); c.setPadding(5f);
        c.setBorder(Rectangle.BOTTOM); c.setBorderColorBottom(sep);
        t.addCell(c);
    }

    private Paragraph espaco() { return new Paragraph(" "); }

    private String brl(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(v != null ? v : BigDecimal.ZERO);
    }
}
