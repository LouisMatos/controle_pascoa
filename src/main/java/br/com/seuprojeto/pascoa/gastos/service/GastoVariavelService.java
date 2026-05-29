package br.com.seuprojeto.pascoa.gastos.service;

import br.com.seuprojeto.pascoa.gastos.dto.GastoDashboardDto;
import br.com.seuprojeto.pascoa.gastos.dto.GastoVariavelForm;
import br.com.seuprojeto.pascoa.gastos.dto.OrcamentoGastoForm;
import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import br.com.seuprojeto.pascoa.gastos.entity.GastoVariavel;
import br.com.seuprojeto.pascoa.gastos.entity.OrcamentoGasto;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import br.com.seuprojeto.pascoa.gastos.repository.OrcamentoGastoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GastoVariavelService {

    private final GastoVariavelRepository gastoRepo;
    private final OrcamentoGastoRepository orcamentoRepo;

    @Transactional(readOnly = true)
    public List<GastoVariavel> listar(int ano, int mes, CategoriaGasto categoria) {
        if (categoria != null) {
            return gastoRepo.findByReferenciaAnoAndReferenciaMesAndCategoriaOrderByDataLancamentoDesc(ano, mes, categoria);
        }
        return gastoRepo.findByReferenciaAnoAndReferenciaMesOrderByDataLancamentoDesc(ano, mes);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    @Transactional
    public GastoVariavel salvar(GastoVariavelForm form, String usuario) {
        GastoVariavel gasto = GastoVariavel.builder()
                .descricao(form.getDescricao())
                .valor(form.getValor())
                .dataLancamento(form.getDataLancamento())
                .categoria(form.getCategoria())
                .referenciaMes(form.getDataLancamento().getMonthValue())
                .referenciaAno(form.getDataLancamento().getYear())
                .observacoes(form.getObservacoes())
                .criadoPor(usuario)
                .build();
        return gastoRepo.save(gasto);
    }

    @Transactional(readOnly = true)
    public GastoVariavel buscarPorId(Long id) {
        return gastoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto não encontrado: " + id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    @Transactional
    public GastoVariavel atualizar(Long id, GastoVariavelForm form) {
        GastoVariavel gasto = buscarPorId(id);
        gasto.setDescricao(form.getDescricao());
        gasto.setValor(form.getValor());
        gasto.setDataLancamento(form.getDataLancamento());
        gasto.setCategoria(form.getCategoria());
        gasto.setReferenciaMes(form.getDataLancamento().getMonthValue());
        gasto.setReferenciaAno(form.getDataLancamento().getYear());
        gasto.setObservacoes(form.getObservacoes());
        return gastoRepo.save(gasto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    @Transactional
    public void excluir(Long id) {
        gastoRepo.deleteById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    @Transactional
    public int importarCSV(MultipartFile file, String usuario) throws Exception {
        int importados = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String linha;
            boolean primeiraLinha = true;

            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty()) continue;
                if (primeiraLinha) { primeiraLinha = false; continue; }

                String[] campos = parseCsvLine(linha);
                if (campos.length < 4) continue;

                try {
                    String descricao = campos[0].trim();
                    BigDecimal valor = new BigDecimal(campos[1].trim().replace(",", "."));
                    LocalDate data = LocalDate.parse(campos[2].trim(), fmt);
                    CategoriaGasto categoria = CategoriaGasto.valueOf(campos[3].trim().toUpperCase());
                    String obs = campos.length > 4 ? campos[4].trim() : null;

                    GastoVariavel gasto = GastoVariavel.builder()
                            .descricao(descricao)
                            .valor(valor)
                            .dataLancamento(data)
                            .categoria(categoria)
                            .referenciaMes(data.getMonthValue())
                            .referenciaAno(data.getYear())
                            .observacoes(obs)
                            .criadoPor(usuario)
                            .build();
                    gastoRepo.save(gasto);
                    importados++;
                } catch (Exception ignored) {
                    // Linha inválida: pula sem interromper o lote
                }
            }
        }
        return importados;
    }

    private String[] parseCsvLine(String linha) {
        List<String> campos = new ArrayList<>();
        boolean dentroAspas = false;
        StringBuilder sb = new StringBuilder();
        for (char c : linha.toCharArray()) {
            if (c == '"') {
                dentroAspas = !dentroAspas;
            } else if (c == ',' && !dentroAspas) {
                campos.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        campos.add(sb.toString());
        return campos.toArray(new String[0]);
    }

    @Transactional(readOnly = true)
    public GastoDashboardDto gerarDashboard(int ano, int mes) {
        BigDecimal totalRealizado = gastoRepo.sumTotal(ano, mes);

        Map<CategoriaGasto, BigDecimal> realizadoPorCategoria = gastoRepo
                .sumPorCategoria(ano, mes).stream()
                .collect(Collectors.toMap(
                        r -> (CategoriaGasto) r[0],
                        r -> (BigDecimal) r[1]));

        Map<CategoriaGasto, BigDecimal> orcadoPorCategoria = orcamentoRepo
                .findByReferenciaAnoAndReferenciaMesOrderByCategoria(ano, mes).stream()
                .collect(Collectors.toMap(
                        OrcamentoGasto::getCategoria,
                        OrcamentoGasto::getValorOrcado));

        List<GastoDashboardDto.CategoriaResumo> resumos = Arrays.stream(CategoriaGasto.values())
                .map(cat -> {
                    BigDecimal realizado = realizadoPorCategoria.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal orcado = orcadoPorCategoria.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal saldo = orcado.subtract(realizado);
                    return new GastoDashboardDto.CategoriaResumo(cat, realizado, orcado, saldo);
                })
                .filter(r -> r.realizado().compareTo(BigDecimal.ZERO) > 0
                          || r.orcado().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal totalOrcado = orcadoPorCategoria.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new GastoDashboardDto(mes, ano, totalRealizado, totalOrcado, resumos);
    }

    @Transactional
    public void salvarOrcamento(OrcamentoGastoForm form) {
        OrcamentoGasto orc = orcamentoRepo
                .findByCategoriaAndReferenciaAnoAndReferenciaMes(
                        form.getCategoria(), form.getReferenciaAno(), form.getReferenciaMes())
                .orElse(new OrcamentoGasto());
        orc.setCategoria(form.getCategoria());
        orc.setValorOrcado(form.getValorOrcado());
        orc.setReferenciaMes(form.getReferenciaMes());
        orc.setReferenciaAno(form.getReferenciaAno());
        orcamentoRepo.save(orc);
    }

    @Transactional(readOnly = true)
    public List<OrcamentoGasto> listarOrcamentos(int ano, int mes) {
        return orcamentoRepo.findByReferenciaAnoAndReferenciaMesOrderByCategoria(ano, mes);
    }
}
