package br.com.seuprojeto.pascoa.configengine.service;

import br.com.seuprojeto.pascoa.configengine.domain.TenantConfig;
import br.com.seuprojeto.pascoa.configengine.domain.TipoNegocio;
import br.com.seuprojeto.pascoa.configengine.dto.ConfigEfetivaResponse;
import br.com.seuprojeto.pascoa.configengine.exception.ConfigException;
import br.com.seuprojeto.pascoa.configengine.repository.TenantConfigRepository;
import br.com.seuprojeto.pascoa.configengine.template.TemplateCatalog;
import br.com.seuprojeto.pascoa.configengine.template.TemplateNegocio;
import br.com.seuprojeto.pascoa.configengine.template.TemplateNegocio.AtributoProduto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ConfigEngineService {

    private final TenantConfigRepository repo;
    private final ObjectMapper objectMapper;

    /** Aplica o template do {@link TipoNegocio} ao tenant — clona os campos. Sobrescreve se já existir. */
    public TenantConfig aplicarTemplate(String tenantId, TipoNegocio tipo) {
        TemplateNegocio tpl = TemplateCatalog.buscar(tipo);
        TenantConfig cfg = repo.findById(tenantId).orElseGet(() ->
                TenantConfig.builder().tenantId(tenantId).build());
        cfg.setTipoNegocio(tipo);
        cfg.setCategoriasJson      (writeJson(tpl.categorias()));
        cfg.setUnidadesJson        (writeJson(tpl.unidades()));
        cfg.setFasesProducaoJson   (writeJson(tpl.fasesProducao()));
        cfg.setAtributosProdutoJson(writeJson(tpl.atributosProduto()));
        cfg.setModeloPreco         (tpl.modeloPreco());
        cfg.setAntecedenciaMinimaDias(tpl.antecedenciaMinimaDias());
        log.info("Template {} aplicado ao tenant {}", tipo, tenantId);
        return repo.save(cfg);
    }

    @Transactional(readOnly = true)
    public ConfigEfetivaResponse buscarEfetiva(String tenantId) {
        TenantConfig cfg = obrigatoria(tenantId);
        return new ConfigEfetivaResponse(
                cfg.getTenantId(),
                cfg.getTipoNegocio(),
                readList(cfg.getCategoriasJson(),    String.class),
                readList(cfg.getUnidadesJson(),      String.class),
                readList(cfg.getFasesProducaoJson(), String.class),
                readList(cfg.getAtributosProdutoJson(), AtributoProduto.class),
                cfg.getModeloPreco(),
                cfg.getAntecedenciaMinimaDias());
    }

    @Transactional(readOnly = true)
    public List<String> categorias(String tenantId) {
        return readList(obrigatoria(tenantId).getCategoriasJson(), String.class);
    }

    public List<String> adicionarCategoria(String tenantId, String categoria) {
        TenantConfig cfg = obrigatoria(tenantId);
        List<String> atual = new java.util.ArrayList<>(readList(cfg.getCategoriasJson(), String.class));
        if (!atual.contains(categoria)) atual.add(categoria);
        cfg.setCategoriasJson(writeJson(atual));
        return atual;
    }

    public List<String> substituirFasesProducao(String tenantId, List<String> novas) {
        TenantConfig cfg = obrigatoria(tenantId);
        cfg.setFasesProducaoJson(writeJson(novas));
        return novas;
    }

    @Transactional(readOnly = true)
    public List<String> fasesProducao(String tenantId) {
        return readList(obrigatoria(tenantId).getFasesProducaoJson(), String.class);
    }

    @Transactional(readOnly = true)
    public List<AtributoProduto> atributosProduto(String tenantId) {
        return readList(obrigatoria(tenantId).getAtributosProdutoJson(), AtributoProduto.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private TenantConfig obrigatoria(String tenantId) {
        return repo.findById(tenantId).orElseThrow(() ->
                new ConfigException("Config não inicializada para tenant: " + tenantId
                        + " — chame POST /config/{id}/aplicar-template/{tipo} primeiro"));
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new ConfigException("Falha ao serializar JSON", e); }
    }

    private <T> List<T> readList(String json, Class<T> itemType) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, itemType));
        } catch (Exception e) {
            throw new ConfigException("Falha ao desserializar JSON", e);
        }
    }
}
