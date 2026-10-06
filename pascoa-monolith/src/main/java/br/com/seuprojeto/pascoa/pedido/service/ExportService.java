package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.common.quantidade.Quantidades;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.shared.pdf.PdfKit;
// OpenPDF — sem wildcard para evitar conflito com POI
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
// Apache POI — modelo de planilha (sem wildcard para evitar conflito com OpenPDF)
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera arquivos Excel (.xlsx) e PDF a partir dos dados do sistema.
 */
@Service
public class ExportService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;


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
                "Gerado em: " + LocalDateTime.now().format(PdfKit.DATETIME_FMT));
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
                row.createCell(3).setCellValue(p.getDataPedido().format(PdfKit.DATETIME_FMT));
                row.createCell(4).setCellValue(
                    p.getDataEntrega() != null ? p.getDataEntrega().format(PdfKit.DATE_FMT) : "");
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
        try (PdfKit pdf = new PdfKit()) {
            pdf.cabecalho("Comprovante de Pedido #" + pedido.getId());

            pdf.secao("CLIENTE E PEDIDO");
            PdfPTable infoTbl = pdf.infoTable();
            pdf.addInfo(infoTbl, "Cliente:",  pedido.getCliente().getNome());
            pdf.addInfo(infoTbl, "Telefone:", pedido.getCliente().getTelefone());
            pdf.addInfo(infoTbl, "E-mail:",   pedido.getCliente().getEmail());
            pdf.addInfo(infoTbl, "Data do Pedido:", pedido.getDataPedido().format(PdfKit.DATETIME_FMT));
            if (pedido.getDataEntrega() != null) {
                String entrega = pedido.getDataEntrega().format(PdfKit.DATE_FMT);
                if (pedido.getSlotEntrega() != null) {
                    entrega += " as " + pedido.getSlotEntrega().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
                pdf.addInfo(infoTbl, "Previsao de Entrega:", entrega);
            }
            pdf.addInfo(infoTbl, "Status:", pedido.getStatus().getDescricao());
            pdf.add(infoTbl);

            pdf.secao("PRODUTOS");
            PdfPTable itensTbl = pdf.itensTable();
            for (ItemPedido item : pedido.getItens()) {
                pdf.addTd(itensTbl, item.getProduto().getNome(),          Element.ALIGN_LEFT);
                pdf.addTd(itensTbl, Quantidades.formatar(item.getQuantidade(), item.getProduto().getUnidadeVenda()), Element.ALIGN_CENTER);
                pdf.addTd(itensTbl, PdfKit.brl(item.getPrecoUnitario()),  Element.ALIGN_RIGHT);
                pdf.addTd(itensTbl, PdfKit.brl(item.getSubtotal()),       Element.ALIGN_RIGHT);
            }
            pdf.addLinhaTotal(itensTbl, pedido.getTotalPedido());
            pdf.add(itensTbl);

            pdf.secao("SITUACAO FINANCEIRA");
            PdfPTable finTbl = new PdfPTable(3);
            finTbl.setWidthPercentage(70f);
            finTbl.setHorizontalAlignment(Element.ALIGN_LEFT);
            finTbl.setSpacingAfter(14f);
            for (String lbl : new String[]{"Total", "Pago", "Saldo"}) {
                pdf.addTh(finTbl, lbl, Element.ALIGN_CENTER);
            }
            Color saldoCor = saldo.signum() > 0 ? PdfKit.VERMELHO : PdfKit.ESMERALDA;
            finTbl.addCell(finCell(PdfKit.brl(pedido.getTotalPedido()), pdf.fNegrito));
            finTbl.addCell(finCell(PdfKit.brl(totalPago),               pdf.fNegrito));
            finTbl.addCell(finCell(PdfKit.brl(saldo), new Font(pdf.bfBold, 11, Font.NORMAL, saldoCor)));
            pdf.add(finTbl);

            pdf.observacoes(pedido.getObservacoes());

            if (pedido.getTokenAcompanhamento() != null) {
                pdf.link("Acompanhe seu pedido em:",
                         baseUrl + "/acompanhamento/" + pedido.getTokenAcompanhamento());
            }

            pdf.rodape();
            return pdf.finalizar();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erro ao gerar PDF do pedido #" + pedido.getId(), e);
        }
    }

    /** Célula de valor na tabela financeira */
    private PdfPCell finCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(PdfKit.CINZA_CLARO);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6f);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /** Retorna s se não nulo, caso contrário "" */
    private String nvl(String s) {
        return (s != null) ? s : "";
    }
}
