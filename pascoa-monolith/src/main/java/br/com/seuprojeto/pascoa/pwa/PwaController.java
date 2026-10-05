package br.com.seuprojeto.pascoa.pwa;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Gera ícones PNG do PWA dinamicamente usando Java AWT.
 * Não requer nenhuma dependência adicional.
 */
@Controller
public class PwaController {

    private static final Color GREEN_BG  = new Color(0x19, 0x87, 0x54); // Bootstrap success
    private static final Color GREEN_DARK = new Color(0x13, 0x65, 0x3f);
    private static final Color WHITE      = Color.WHITE;

    /**
     * Serve /icons/icon-192.png e /icons/icon-512.png.
     * O tamanho é extraído do path, mas só 192 e 512 são declarados no manifest.
     */
    @GetMapping(value = "/icons/icon-{size}.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] icon(@PathVariable int size) throws IOException {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

            // ── Fundo: rounded-rect verde ─────────────────────────────────
            int arc = size / 5;
            g.setColor(GREEN_BG);
            g.fill(new RoundRectangle2D.Float(0, 0, size, size, arc, arc));

            // ── Sombra suave do ovo ───────────────────────────────────────
            int padH = size / 7;
            int padV = size / 8;
            int eggW = size - padH * 2;
            int eggH = (int) (eggW * 1.22); // proporção ovo
            int eggX = padH;
            int eggY = (size - eggH) / 2;

            // sombra (deslocada 2% do tamanho)
            int shadow = Math.max(2, size / 64);
            g.setColor(GREEN_DARK);
            g.fill(new Ellipse2D.Float(eggX + shadow, eggY + shadow, eggW, eggH));

            // ── Ovo branco ────────────────────────────────────────────────
            g.setColor(WHITE);
            g.fill(new Ellipse2D.Float(eggX, eggY, eggW, eggH));

            // ── Brilho (lustre) ───────────────────────────────────────────
            int shimW = eggW / 4;
            int shimH = eggH / 5;
            int shimX = eggX + eggW / 4;
            int shimY = eggY + eggH / 8;

            GradientPaint shine = new GradientPaint(
                    shimX, shimY, new Color(255, 255, 255, 180),
                    shimX + shimW, shimY + shimH, new Color(255, 255, 255, 0));
            g.setPaint(shine);
            g.fill(new Ellipse2D.Float(shimX, shimY, shimW, shimH));

        } finally {
            g.dispose();
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", bos);
        return bos.toByteArray();
    }
}
