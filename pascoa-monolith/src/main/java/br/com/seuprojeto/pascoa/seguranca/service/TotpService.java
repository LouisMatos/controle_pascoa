package br.com.seuprojeto.pascoa.seguranca.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class TotpService {

    private static final int TOTP_DIGITS = 6;
    private static final int SECRET_BYTES = 20;
    private static final int QR_SIZE = 200;

    private final TimeBasedOneTimePasswordGenerator generator;
    private final Base32 base32 = new Base32();

    public TotpService() {
        try {
            this.generator = new TimeBasedOneTimePasswordGenerator();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao inicializar gerador TOTP", e);
        }
    }

    /** Gera um novo segredo aleatório codificado em Base32. */
    public String gerarSegredo() {
        byte[] bytes = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return base32.encodeToString(bytes).replace("=", "");
    }

    /**
     * Valida um código TOTP com janela de ±1 step (±30 s) para tolerância de clock skew.
     */
    public boolean validar(String segredoBase32, int codigo) {
        try {
            byte[] decoded = base32.decode(segredoBase32);
            Key key = new SecretKeySpec(decoded, generator.getAlgorithm());
            Instant agora = Instant.now();
            for (int offset = -1; offset <= 1; offset++) {
                Instant t = agora.plus(Duration.ofSeconds(30L * offset));
                if (generator.generateOneTimePassword(key, t) == codigo) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** URI otpauth:// para configuração em aplicativos como Google Authenticator. */
    public String qrCodeUri(String segredo, String login) {
        return "otpauth://totp/Sistema%20P%C3%A1scoa:" + login
            + "?secret=" + segredo
            + "&issuer=SistemaPascoa"
            + "&digits=" + TOTP_DIGITS;
    }

    /** Gera imagem do QR Code como data URI base64 (PNG). */
    public String qrCodeDataUri(String segredo, String login) {
        try {
            String uri = qrCodeUri(segredo, login);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(uri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar QR Code", e);
        }
    }
}
