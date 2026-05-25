package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
// OpenPDF — modelo de documento (sem wildcard para evitar conflito com POI)
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
// OpenPDF — PDF writer e tabelas
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
// Apache POI — modelo de planilha (sem wildcard para evitar conflito com OpenPDF)
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Gera arquivos Excel (.xlsx) e PDF a partir dos dados do sistema.
 */
@Service
public class ExportService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale PT_BR = new Locale("pt", "BR");

    // ─────────────────────────────────────────────────────────────────────────
    // EXCEL
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] gerarExcelPedidos(List<Pedido> pedidos) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            Sheet sheet = wb.createSheet("Pedidos");
            sheet.setDefaultColumnWidth(18);

            CellStyle tituloStyle   = estiloTitulo(wb);
            CellStyle headerStyle   = estiloCabecalho(wb);
            CellStyle moedaStyle    = estiloMoeda(wb);
            CellStyle totalMoedaStyle = estiloTotalMoeda(wb);

            // ── Linha 0: título ──────────────────────────────────────────────
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(22);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("Pascoa Artesanal - Lista de Pedidos");
            c0.setCellStyle(tituloStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // ── Linha 1: subtítulo ───────────────────────────────────────────
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(
                "Gerado em: " + LocalDateTime.now().format(DATETIME_FMT));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            // ── Linha 2: vazia ───────────────────────────────────────────────
            sheet.createRow(2);

            // ── Linha 3: cabeçalho das colunas ──────────────────────────────
            String[] headers = {"#", "Cliente", "Telefone", "Data Pedido",
                                 "Data Entrega", "Status", "Total (R$)"};
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(18);
            for (int i = 0; i < headers.length; i++) {
                Cell c = r3.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // ── Linhas de dados ──────────────────────────────────────────────
            int rowNum = 4;
            for (Pedido p : pedidos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getCliente().getNome());
                row.createCell(2).setCellValue(
                    nvl(p.getCliente().getTelefone()));
                row.createCell(3).setCellValue(p.getDataPedido().format(DATETIME_FMT));
                row.createCell(4).setCellValue(
                    p.getDataEntrega() != null ? p.getDataEntrega().format(DATE_FMT) : "");
                row.createCell(5).setCellValue(p.getStatus().getDescricao());
                Cell totalCell = row.createCell(6);
                totalCell.setCellValue(p.getTotalPedido().doubleValue());
                totalCell.setCellStyle(moedaStyle);
            }

            // ── Linha de total geral ─────────────────────────────────────────
            if (!pedidos.isEmpty()) {
                Row totalRow = sheet.createRow(rowNum);
                org.apache.poi.ss.usermodel.Font boldFnt = wb.createFont();
                boldFnt.setBold(true);
                CellStyle boldRight = wb.createCellStyle();
                boldRight.setFont(boldFnt);
                boldRight.setAlignment(HorizontalAlignment.RIGHT);

                Cell labelCell = totalRow.createCell(5);
                labelCell.setCellValue("TOTAL:");
                labelCell.setCellStyle(boldRight);

                Cell grandTotal = totalRow.createCell(6);
                grandTotal.setCellFormula("SUM(G5:G" + rowNum + ")");
                grandTotal.setCellStyle(totalMoedaStyle);
            }

            // ── Auto-size ────────────────────────────────────────────────────
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 10 * 256) {
                    sheet.setColumnWidth(i, 10 * 256);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Estilos Excel ────────────────────────────────────────────────────────

    private CellStyle estiloTitulo(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        return s;
    }

    private CellStyle estiloCabecalho(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private CellStyle estiloMoeda(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private CellStyle estiloTotalMoeda(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setBorderTop(BorderStyle.DOUBLE);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] gerarPdfPedido(Pedido pedido, BigDecimal totalPago, BigDecimal saldo) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40f, 40f, 50f, 50f);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── Fontes com suporte a caracteres portugueses (CP1252) ─────────
            BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,
                    "Cp1252", BaseFont.NOT_EMBEDDED);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD,
                    "Cp1252", BaseFont.NOT_EMBEDDED);

            Color verde    = new Color(45, 106, 79);
            Color cinzaCl  = new Color(245, 245, 245);
            Color cinzaSep = new Color(220, 220, 220);
            Color preto    = Color.BLACK;
            Color branco   = Color.WHITE;
            Color vermelho = new Color(185, 28, 28);
            Color esmeralda = new Color(21, 128, 61);

            Font fTituloW  = new Font(bfBold, 18, Font.NORMAL, branco);
            Font fSubW     = new Font(bf,     11, Font.NORMAL, branco);
            Font fSecao    = new Font(bfBold, 10, Font.NORMAL, verde);
            Font fNormal   = new Font(bf,     10, Font.NORMAL, preto);
            Font fNegrito  = new Font(bfBold, 10, Font.NORMAL, preto);
            Font fPeq      = new Font(bf,      9, Font.NORMAL, new Color(100, 100, 100));
            Font fPeqBold  = new Font(bfBold,  9, Font.NORMAL, preto);
            Font fThW      = new Font(bfBold,  9, Font.NORMAL, branco);

            // ── Cabeçalho verde ──────────────────────────────────────────────
            PdfPTable headerTbl = new PdfPTable(1);
            headerTbl.setWidthPercentage(100f);
            headerTbl.setSpacingAfter(14f);

            PdfPCell hCell = new PdfPCell();
            hCell.setBackgroundColor(verde);
            hCell.setPadding(14f);
            hCell.setBorder(Rectangle.NO_BORDER);

            Paragraph pTit = new Paragraph("Pascoa Artesanal", fTituloW);
            pTit.setAlignment(Element.ALIGN_CENTER);
            hCell.addElement(pTit);

            Paragraph pSub = new Paragraph("Comprovante de Pedido #" + pedido.getId(), fSubW);
            pSub.setAlignment(Element.ALIGN_CENTER);
            hCell.addElement(pSub);

            Paragraph pDt = new Paragraph(
                "Gerado em " + LocalDateTime.now().format(DATETIME_FMT), fSubW);
            pDt.setAlignment(Element.ALIGN_CENTER);
            hCell.addElement(pDt);

            headerTbl.addCell(hCell);
            doc.add(headerTbl);

            // ── Informações do cliente / pedido ──────────────────────────────
            doc.add(new Paragraph("CLIENTE E PEDIDO", fSecao));
            doc.add(espaço());

            PdfPTable infoTbl = new PdfPTable(2);
            infoTbl.setWidthPercentage(100f);
            infoTbl.setWidths(new float[]{35f, 65f});
            infoTbl.setSpacingAfter(12f);

            addInfo(infoTbl, "Cliente:",    pedido.getCliente().getNome(),    fPeqBold, fNormal, cinzaSep);
            if (str(pedido.getCliente().getTelefone()))
                addInfo(infoTbl, "Telefone:", pedido.getCliente().getTelefone(), fPeqBold, fNormal, cinzaSep);
            if (str(pedido.getCliente().getEmail()))
                addInfo(infoTbl, "E-mail:",   pedido.getCliente().getEmail(),    fPeqBold, fNormal, cinzaSep);
            addInfo(infoTbl, "Data do Pedido:",
                    pedido.getDataPedido().format(DATETIME_FMT), fPeqBold, fNormal, cinzaSep);
            if (pedido.getDataEntrega() != null) {
                String entrega = pedido.getDataEntrega().format(DATE_FMT);
                if (pedido.getSlotEntrega() != null)
                    entrega += " as " + pedido.getSlotEntrega()
                            .format(DateTimeFormatter.ofPattern("HH:mm"));
                addInfo(infoTbl, "Previsao de Entrega:", entrega, fPeqBold, fNormal, cinzaSep);
            }
            addInfo(infoTbl, "Status:", pedido.getStatus().getDescricao(), fPeqBold, fNormal, cinzaSep);
            doc.add(infoTbl);

            // ── Tabela de produtos ───────────────────────────────────────────
            doc.add(new Paragraph("PRODUTOS", fSecao));
            doc.add(espaço());

            PdfPTable itensTbl = new PdfPTable(4);
            itensTbl.setWidthPercentage(100f);
            itensTbl.setWidths(new float[]{46f, 11f, 21f, 22f});
            itensTbl.setSpacingAfter(12f);

            // Cabeçalho
            addTh(itensTbl, "Produto",     Element.ALIGN_LEFT,   verde, fThW);
            addTh(itensTbl, "Qtd",         Element.ALIGN_CENTER, verde, fThW);
            addTh(itensTbl, "Preco Unit.", Element.ALIGN_RIGHT,  verde, fThW);
            addTh(itensTbl, "Subtotal",    Element.ALIGN_RIGHT,  verde, fThW);

            // Itens
            for (ItemPedido item : pedido.getItens()) {
                addTd(itensTbl, item.getProduto().getNome(),      Element.ALIGN_LEFT,   fNormal, cinzaSep);
                addTd(itensTbl, String.valueOf(item.getQuantidade()), Element.ALIGN_CENTER, fNormal, cinzaSep);
                addTd(itensTbl, brl(item.getPrecoUnitario()),    Element.ALIGN_RIGHT,  fNormal, cinzaSep);
                addTd(itensTbl, brl(item.getSubtotal()),         Element.ALIGN_RIGHT,  fNormal, cinzaSep);
            }

            // Linha de total
            PdfPCell totLbl = new PdfPCell(new Phrase("TOTAL:", fNegrito));
            totLbl.setColspan(3);
            totLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totLbl.setPadding(5f);
            totLbl.setBorder(Rectangle.TOP);
            itensTbl.addCell(totLbl);

            PdfPCell totVal = new PdfPCell(new Phrase(brl(pedido.getTotalPedido()), fNegrito));
            totVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totVal.setPadding(5f);
            totVal.setBorder(Rectangle.TOP);
            itensTbl.addCell(totVal);

            doc.add(itensTbl);

            // ── Situação financeira ──────────────────────────────────────────
            doc.add(new Paragraph("SITUACAO FINANCEIRA", fSecao));
            doc.add(espaço());

            // 3 colunas: cabeçalhos na linha 1, valores na linha 2
            PdfPTable finTbl = new PdfPTable(3);
            finTbl.setWidthPercentage(70f);
            finTbl.setHorizontalAlignment(Element.ALIGN_LEFT);
            finTbl.setSpacingAfter(14f);

            // Linha 1 — labels (fundo verde)
            for (String lbl : new String[]{"Total", "Pago", "Saldo"}) {
                PdfPCell c = new PdfPCell(new Phrase(lbl, fThW));
                c.setBackgroundColor(verde);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(5f);
                c.setBorder(Rectangle.NO_BORDER);
                finTbl.addCell(c);
            }

            // Linha 2 — valores
            Color saldoCor = saldo.signum() > 0 ? vermelho : esmeralda;
            Font  fSaldo   = new Font(bfBold, 11, Font.NORMAL, saldoCor);

            PdfPCell cTotal = finCell(brl(pedido.getTotalPedido()), fNegrito, cinzaCl);
            PdfPCell cPago  = finCell(brl(totalPago),               fNegrito, cinzaCl);
            PdfPCell cSaldo = finCell(brl(saldo),                   fSaldo,   cinzaCl);
            finTbl.addCell(cTotal);
            finTbl.addCell(cPago);
            finTbl.addCell(cSaldo);

            doc.add(finTbl);

            // ── Observações ──────────────────────────────────────────────────
            if (str(pedido.getObservacoes())) {
                doc.add(new Paragraph("OBSERVACOES", fSecao));
                doc.add(espaço());
                Paragraph obs = new Paragraph(pedido.getObservacoes(), fNormal);
                obs.setSpacingAfter(14f);
                doc.add(obs);
            }

            // ── Link de acompanhamento ───────────────────────────────────────
            if (pedido.getTokenAcompanhamento() != null) {
                String link = baseUrl + "/acompanhamento/" + pedido.getTokenAcompanhamento();
                doc.add(new Paragraph("Acompanhe seu pedido em:", fPeq));
                Font fLink = new Font(bf, 9, Font.UNDERLINE, verde);
                Paragraph linkPara = new Paragraph(link, fLink);
                linkPara.setSpacingAfter(10f);
                doc.add(linkPara);
            }

            // ── Rodapé ───────────────────────────────────────────────────────
            // Linha separadora via tabela com borda superior
            PdfPTable rodapeSep = new PdfPTable(1);
            rodapeSep.setWidthPercentage(100f);
            PdfPCell rodapeSepCell = new PdfPCell(new Phrase(" "));
            rodapeSepCell.setBorder(Rectangle.TOP);
            rodapeSepCell.setBorderColorTop(cinzaSep);
            rodapeSepCell.setPaddingTop(0f);
            rodapeSepCell.setPaddingBottom(4f);
            rodapeSep.addCell(rodapeSepCell);
            doc.add(rodapeSep);

            Paragraph rodape = new Paragraph("Obrigado pela preferencia! Pascoa Artesanal.", fPeq);
            rodape.setAlignment(Element.ALIGN_CENTER);
            doc.add(rodape);

        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Erro ao gerar PDF do pedido #" + pedido.getId(), e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    // ── Helpers de layout PDF ────────────────────────────────────────────────

    /** Linha de informação: [label | valor] com borda inferior */
    private void addInfo(PdfPTable tbl, String label, String value,
                         Font lf, Font vf, Color sepColor) {
        PdfPCell l = new PdfPCell(new Phrase(label, lf));
        l.setPadding(4f);
        l.setBorder(Rectangle.BOTTOM);
        l.setBorderColorBottom(sepColor);
        tbl.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(nvl(value), vf));
        v.setPadding(4f);
        v.setBorder(Rectangle.BOTTOM);
        v.setBorderColorBottom(sepColor);
        tbl.addCell(v);
    }

    /** Célula de cabeçalho de tabela (fundo colorido, texto branco) */
    private void addTh(PdfPTable tbl, String text, int align, Color bg, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setPadding(6f);
        c.setBorder(Rectangle.NO_BORDER);
        tbl.addCell(c);
    }

    /** Célula de dados da tabela de itens */
    private void addTd(PdfPTable tbl, String text, int align, Font f, Color sepColor) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align);
        c.setPadding(5f);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(sepColor);
        tbl.addCell(c);
    }

    /** Célula de valor na tabela financeira */
    private PdfPCell finCell(String text, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6f);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /** Parágrafo vazio de espaçamento */
    private Paragraph espaço() {
        return new Paragraph(" ");
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

    /** Formata BigDecimal como moeda BR (ex.: R$ 1.234,56) */
    private String brl(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(v != null ? v : BigDecimal.ZERO);
    }

    /** Retorna s se não nulo/vazio, caso contrário "" */
    private String nvl(String s) {
        return (s != null) ? s : "";
    }

    /** true se string não-nula e não-vazia */
    private boolean str(String s) {
        return s != null && !s.isBlank();
    }
}
