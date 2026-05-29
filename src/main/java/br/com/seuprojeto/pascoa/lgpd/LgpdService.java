package br.com.seuprojeto.pascoa.lgpd;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LgpdService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository  pedidoRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAllByOrderByNomeAsc();
    }

    // ── Consentimento ────────────────────────────────────────────────────────

    @Transactional
    public void registrarConsentimento(Long clienteId, boolean consentimento) {
        Cliente c = buscar(clienteId);
        c.setOptIn(consentimento);
        c.setDataConsentimento(consentimento ? LocalDateTime.now() : null);
        clienteRepository.save(c);
    }

    // ── Anonimização (direito ao esquecimento) ────────────────────────────────

    @Transactional
    public void anonimizar(Long clienteId) {
        Cliente c = buscar(clienteId);
        if (Boolean.TRUE.equals(c.getAnonimizado())) {
            throw new IllegalStateException("Os dados deste cliente já foram anonimizados.");
        }
        c.setNome("Titular Anônimo #" + clienteId);
        c.setEmail(null);
        c.setTelefone(null);
        c.setCpf(null);
        c.setEndereco(null);
        c.setOptIn(false);
        c.setDataConsentimento(null);
        c.setPreferenciaCanal(PreferenciaCanal.NENHUM);
        c.setAnonimizado(true);
        clienteRepository.save(c);
    }

    // ── Exportação de dados (direito de acesso / portabilidade) ──────────────

    @Transactional(readOnly = true)
    public byte[] exportarDados(Long clienteId) {
        Cliente c = buscar(clienteId);
        var pedidos = pedidoRepository.ultimosPedidosPorCliente(clienteId);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"titular\": {\n");
        sb.append("    \"id\": ").append(c.getId()).append(",\n");
        sb.append("    \"nome\": ").append(jsonStr(c.getNome())).append(",\n");
        sb.append("    \"email\": ").append(jsonStr(c.getEmail())).append(",\n");
        sb.append("    \"telefone\": ").append(jsonStr(c.getTelefone())).append(",\n");
        sb.append("    \"cpf\": ").append(jsonStr(c.getCpf())).append(",\n");
        sb.append("    \"endereco\": ").append(jsonStr(c.getEndereco())).append(",\n");
        sb.append("    \"optIn\": ").append(Boolean.TRUE.equals(c.getOptIn())).append(",\n");
        sb.append("    \"dataConsentimento\": ").append(
            c.getDataConsentimento() != null ? jsonStr(c.getDataConsentimento().format(FMT)) : "null"
        ).append(",\n");
        sb.append("    \"dataCadastro\": ").append(
            c.getCriadoEm() != null ? jsonStr(c.getCriadoEm().format(FMT)) : "null"
        ).append("\n");
        sb.append("  },\n");

        sb.append("  \"pedidos\": [\n");
        for (int i = 0; i < pedidos.size(); i++) {
            var p = pedidos.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": ").append(p.getId()).append(",\n");
            sb.append("      \"status\": ").append(jsonStr(p.getStatus().name())).append(",\n");
            sb.append("      \"dataEntrega\": ").append(
                p.getDataEntrega() != null ? jsonStr(p.getDataEntrega().toString()) : "null"
            ).append(",\n");
            sb.append("      \"total\": ").append(
                p.getTotalPedido() != null ? p.getTotalPedido().toPlainString() : "0"
            ).append("\n");
            sb.append("    }").append(i < pedidos.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    private String jsonStr(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
