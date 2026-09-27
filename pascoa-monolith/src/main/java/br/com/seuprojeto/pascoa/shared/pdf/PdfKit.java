package br.com.seuprojeto.pascoa.shared.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Layout comum dos PDFs do sistema (comprovante de pedido, orçamento).
 * Um PdfKit = um documento; instanciar, montar as seções e chamar finalizar().
 */
public class PdfKit implements AutoCloseable {

    public static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    public static final Color VERDE       = new Color(45, 106, 79);
    public static final Color CINZA_SEP   = new Color(220, 220, 220);
    public static final Color CINZA_CLARO = new Color(245, 245, 245);
    public static final Color VERMELHO    = new Color(185, 28, 28);
    public static final Color ESMERALDA   = new Color(21, 128, 61);

    public final BaseFont bf;
    public final BaseFont bfBold;
    public final Font fSecao;
    public final Font fNormal;
    public final Font fNegrito;
    public final Font fPeq;
    public final Font fPeqBold;
    public final Font fThW;

    private final Font fTituloW;
    private final Font fSubW;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final Document doc = new Document(PageSize.A4, 40f, 40f, 50f, 50f);

    public PdfKit() throws IOException {
        PdfWriter.getInstance(doc, out);
        doc.open();
        bf     = BaseFont.createFont(BaseFont.HELVETICA,      "Cp1252", BaseFont.NOT_EMBEDDED);
        bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, "Cp1252", BaseFont.NOT_EMBEDDED);
        fTituloW = new Font(bfBold, 18, Font.NORMAL, Color.WHITE);
        fSubW    = new Font(bf,     11, Font.NORMAL, Color.WHITE);
        fSecao   = new Font(bfBold, 10, Font.NORMAL, VERDE);
        fNormal  = new Font(bf,     10, Font.NORMAL, Color.BLACK);
        fNegrito = new Font(bfBold, 10, Font.NORMAL, Color.BLACK);
        fPeq     = new Font(bf,      9, Font.NORMAL, new Color(100, 100, 100));
        fPeqBold = new Font(bfBold,  9, Font.NORMAL, Color.BLACK);
        fThW     = new Font(bfBold,  9, Font.NORMAL, Color.WHITE);
    }

    public void add(Element e) throws DocumentException {
        doc.add(e);
    }

    public void cabecalho(String subtitulo) throws DocumentException {
        PdfPTable tbl = new PdfPTable(1);
        tbl.setWidthPercentage(100f);
        tbl.setSpacingAfter(14f);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(VERDE);
        cell.setPadding(14f);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(centralizado("Pascoa Artesanal", fTituloW));
        cell.addElement(centralizado(subtitulo, fSubW));
        cell.addElement(centralizado("Gerado em " + LocalDateTime.now().format(DATETIME_FMT), fSubW));

        tbl.addCell(cell);
        doc.add(tbl);
    }

    public void secao(String titulo) throws DocumentException {
        doc.add(new Paragraph(titulo, fSecao));
        doc.add(new Paragraph(" "));
    }

    public PdfPTable infoTable() {
        PdfPTable tbl = new PdfPTable(2);
        tbl.setWidthPercentage(100f);
        tbl.setWidths(new float[]{35f, 65f});
        tbl.setSpacingAfter(12f);
        return tbl;
    }

    /** Linha [label | valor] com borda inferior. Ignora valor nulo/vazio. */
    public void addInfo(PdfPTable tbl, String label, String value) {
        if (value == null || value.isBlank()) { return; }
        tbl.addCell(infoCell(label, fPeqBold));
        tbl.addCell(infoCell(value, fNormal));
    }

    /** Tabela de itens com o cabeçalho padrão Produto/Qtd/Preço/Subtotal. */
    public PdfPTable itensTable() {
        PdfPTable tbl = new PdfPTable(4);
        tbl.setWidthPercentage(100f);
        tbl.setWidths(new float[]{46f, 11f, 21f, 22f});
        tbl.setSpacingAfter(12f);
        addTh(tbl, "Produto",     Element.ALIGN_LEFT);
        addTh(tbl, "Qtd",         Element.ALIGN_CENTER);
        addTh(tbl, "Preco Unit.", Element.ALIGN_RIGHT);
        addTh(tbl, "Subtotal",    Element.ALIGN_RIGHT);
        return tbl;
    }

    public void addTh(PdfPTable tbl, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, fThW));
        c.setBackgroundColor(VERDE);
        c.setHorizontalAlignment(align);
        c.setPadding(6f);
        c.setBorder(Rectangle.NO_BORDER);
        tbl.addCell(c);
    }

    public void addTd(PdfPTable tbl, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, fNormal));
        c.setHorizontalAlignment(align);
        c.setPadding(5f);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(CINZA_SEP);
        tbl.addCell(c);
    }

    /** Linha final "TOTAL: <valor>" ocupando as 4 colunas da tabela de itens. */
    public void addLinhaTotal(PdfPTable tbl, BigDecimal total) {
        PdfPCell lbl = new PdfPCell(new Phrase("TOTAL:", fNegrito));
        lbl.setColspan(3);
        lbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lbl.setPadding(5f);
        lbl.setBorder(Rectangle.TOP);
        tbl.addCell(lbl);

        PdfPCell val = new PdfPCell(new Phrase(brl(total), fNegrito));
        val.setHorizontalAlignment(Element.ALIGN_RIGHT);
        val.setPadding(5f);
        val.setBorder(Rectangle.TOP);
        tbl.addCell(val);
    }

    public void observacoes(String texto) throws DocumentException {
        if (texto == null || texto.isBlank()) { return; }
        secao("OBSERVACOES");
        Paragraph obs = new Paragraph(texto, fNormal);
        obs.setSpacingAfter(14f);
        doc.add(obs);
    }

    public void link(String chamada, String url) throws DocumentException {
        doc.add(new Paragraph(chamada, fPeq));
        Paragraph p = new Paragraph(url, new Font(bf, 9, Font.UNDERLINE, VERDE));
        p.setSpacingAfter(10f);
        doc.add(p);
    }

    public void rodape() throws DocumentException {
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100f);
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColorTop(CINZA_SEP);
        cell.setPaddingTop(0f);
        cell.setPaddingBottom(4f);
        sep.addCell(cell);
        doc.add(sep);
        doc.add(centralizado("Obrigado pela preferencia! Pascoa Artesanal.", fPeq));
    }

    public byte[] finalizar() {
        close();
        return out.toByteArray();
    }

    @Override
    public void close() {
        if (doc.isOpen()) { doc.close(); }
    }

    public static String brl(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(v != null ? v : BigDecimal.ZERO);
    }

    private Paragraph centralizado(String texto, Font f) {
        Paragraph p = new Paragraph(texto, f);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private PdfPCell infoCell(String texto, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setPadding(4f);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(CINZA_SEP);
        return c;
    }
}
