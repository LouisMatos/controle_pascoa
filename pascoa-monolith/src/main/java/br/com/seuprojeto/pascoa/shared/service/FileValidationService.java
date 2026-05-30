package br.com.seuprojeto.pascoa.shared.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Valida arquivos enviados via upload detectando o tipo real pelo magic number.
 * Não confia na extensão nem no Content-Type declarado pelo cliente.
 */
@Service
public class FileValidationService {

    @Value("${app.upload.max-size-bytes:5242880}")
    private long maxSizeBytes;

    public record ValidationResult(String mimeType, String extension) {}

    public ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("Nenhum arquivo enviado.");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalStateException(
                "Arquivo excede o tamanho máximo de " + (maxSizeBytes / 1_048_576) + " MB.");
        }

        byte[] header = readHeader(file, 12);
        ValidationResult result = detectType(header);

        if (result == null) {
            throw new IllegalStateException(
                "Tipo de arquivo não permitido. Formatos aceitos: JPEG, PNG, WebP e PDF.");
        }
        return result;
    }

    private ValidationResult detectType(byte[] h) {
        if (h.length >= 3
                && (h[0] & 0xFF) == 0xFF
                && (h[1] & 0xFF) == 0xD8
                && (h[2] & 0xFF) == 0xFF) {
            return new ValidationResult("image/jpeg", "jpg");
        }
        if (h.length >= 4
                && (h[0] & 0xFF) == 0x89
                && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return new ValidationResult("image/png", "png");
        }
        // WebP: RIFF....WEBP
        if (h.length >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return new ValidationResult("image/webp", "webp");
        }
        if (h.length >= 4
                && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F') {
            return new ValidationResult("application/pdf", "pdf");
        }
        return null;
    }

    private byte[] readHeader(MultipartFile file, int bytes) {
        try (InputStream is = file.getInputStream()) {
            return is.readNBytes(bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler o arquivo enviado.", e);
        }
    }
}
