package br.com.seuprojeto.pascoa.common.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Serve arquivos de upload com Content-Disposition e Content-Type explícitos.
 * Substitui o handler estático /uploads/** para impedir execução de arquivos no browser.
 */
@Controller
public class MediaController {

    private static final Map<String, MediaType> MIME_MAP = Map.of(
        "jpg",  MediaType.IMAGE_JPEG,
        "jpeg", MediaType.IMAGE_JPEG,
        "png",  MediaType.IMAGE_PNG,
        "webp", MediaType.parseMediaType("image/webp"),
        "pdf",  MediaType.APPLICATION_PDF
    );

    @Value("${app.upload.dir:${user.home}/pascoa-uploads}")
    private String uploadDir;

    @GetMapping("/media/{filename:.+}")
    public ResponseEntity<byte[]> servir(@PathVariable String filename) throws IOException {
        // Bloqueia path traversal
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path arquivo = base.resolve(filename).normalize();

        if (!arquivo.startsWith(base) || !Files.exists(arquivo) || !Files.isRegularFile(arquivo)) {
            return ResponseEntity.notFound().build();
        }

        String ext = extensao(filename);
        MediaType mediaType = MIME_MAP.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM);

        // Imagens: inline (exibe no browser); PDFs e outros: attachment (download)
        String disposition = mediaType.getType().equals("image") ? "inline" : "attachment";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"");
        headers.setCacheControl("max-age=86400, public");

        return new ResponseEntity<>(Files.readAllBytes(arquivo), headers, HttpStatus.OK);
    }

    private String extensao(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
