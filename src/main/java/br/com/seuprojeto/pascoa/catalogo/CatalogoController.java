package br.com.seuprojeto.pascoa.catalogo;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Controller
@RequestMapping("/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final ProdutoService produtoService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ── Vitrine ────────────────────────────────────────────────────────────

    @GetMapping
    public String index(@RequestParam(required = false) String categoria, Model model) {
        List<Produto> produtos = produtoService.listarAtivos();

        Categoria categoriaFiltro = null;
        if (categoria != null && !categoria.isBlank()) {
            try {
                categoriaFiltro = Categoria.valueOf(categoria.toUpperCase());
                final Categoria cf = categoriaFiltro;
                produtos = produtos.stream().filter(p -> p.getCategoria() == cf).toList();
            } catch (IllegalArgumentException ignored) {
                // categoria inválida — exibe todos
            }
        }

        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("categoriaAtiva", categoriaFiltro);
        return "catalogo/index";
    }

    // ── Detalhe de produto ─────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, HttpServletRequest request) {
        Produto produto = produtoService.buscarPorId(id);
        String urlDetalhe = buildBaseUrl(request) + "/catalogo/" + id;
        model.addAttribute("produto", produto);
        model.addAttribute("urlDetalhe", urlDetalhe);
        return "catalogo/produto";
    }

    // ── QR Code ────────────────────────────────────────────────────────────

    @GetMapping("/qr/{id}")
    public ResponseEntity<byte[]> qrCode(@PathVariable Long id, HttpServletRequest request) {
        try {
            String url = buildBaseUrl(request) + "/catalogo/" + id;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Auxiliar ───────────────────────────────────────────────────────────

    private String buildBaseUrl(HttpServletRequest request) {
        int port = request.getServerPort();
        boolean defaultPort = (port == 80 && "http".equals(request.getScheme()))
                || (port == 443 && "https".equals(request.getScheme()));
        return request.getScheme() + "://" + request.getServerName()
                + (defaultPort ? "" : ":" + port);
    }
}
