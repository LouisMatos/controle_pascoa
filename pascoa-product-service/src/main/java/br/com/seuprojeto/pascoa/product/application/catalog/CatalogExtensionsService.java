package br.com.seuprojeto.pascoa.product.application.catalog;

import br.com.seuprojeto.pascoa.product.adapter.in.rest.dto.*;
import br.com.seuprojeto.pascoa.product.adapter.out.persistence.*;
import br.com.seuprojeto.pascoa.product.domain.exception.ProdutoNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * v6 Etapa 8 — Extensões do catálogo: atributos dinâmicos, variantes e ficha nutricional.
 * Não toca no fluxo Produto/ProdutoUseCase legado da v5 — só estende.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogExtensionsService {

    private final ProdutoJpaRepository produtoRepo;
    private final ProdutoAtributoJpaRepository atributoRepo;
    private final ProdutoVarianteJpaRepository varianteRepo;
    private final ProdutoFichaNutricionalJpaRepository fichaRepo;
    private final ObjectMapper objectMapper;

    // ── Atributos dinâmicos ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, String> listarAtributos(Long produtoId) {
        garantirProduto(produtoId);
        return atributoRepo.findByProdutoId(produtoId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProdutoAtributoJpaEntity::getAtributoKey,
                        a -> a.getAtributoValue() != null ? a.getAtributoValue() : ""));
    }

    public Map<String, String> definirAtributo(Long produtoId, AtributoRequest req) {
        garantirProduto(produtoId);
        var existing = atributoRepo.findByProdutoIdAndAtributoKey(produtoId, req.chave());
        var entity = existing.orElseGet(() -> ProdutoAtributoJpaEntity.builder()
                .produtoId(produtoId).atributoKey(req.chave()).build());
        entity.setAtributoValue(req.valor());
        atributoRepo.save(entity);
        return listarAtributos(produtoId);
    }

    public void removerAtributo(Long produtoId, String chave) {
        garantirProduto(produtoId);
        atributoRepo.deleteByProdutoIdAndAtributoKey(produtoId, chave);
    }

    // ── Variantes ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VarianteResponse> listarVariantes(Long produtoId) {
        garantirProduto(produtoId);
        return varianteRepo.findByProdutoId(produtoId).stream().map(this::toVarianteResponse).toList();
    }

    public VarianteResponse criarVariante(Long produtoId, VarianteRequest req) {
        garantirProduto(produtoId);
        var entity = ProdutoVarianteJpaEntity.builder()
                .produtoId(produtoId)
                .sku(req.sku())
                .nome(req.nome())
                .preco(req.preco())
                .atributosJson(writeJson(req.atributos()))
                .disponivel(req.disponivel() == null || req.disponivel())
                .build();
        return toVarianteResponse(varianteRepo.save(entity));
    }

    public VarianteResponse atualizarVariante(Long varianteId, VarianteRequest req) {
        var v = varianteRepo.findById(varianteId)
                .orElseThrow(() -> new ProdutoNotFoundException(varianteId));
        v.setSku(req.sku());
        v.setNome(req.nome());
        v.setPreco(req.preco());
        v.setAtributosJson(writeJson(req.atributos()));
        if (req.disponivel() != null) v.setDisponivel(req.disponivel());
        return toVarianteResponse(varianteRepo.save(v));
    }

    public void removerVariante(Long varianteId) {
        if (!varianteRepo.existsById(varianteId)) throw new ProdutoNotFoundException(varianteId);
        varianteRepo.deleteById(varianteId);
    }

    // ── Ficha nutricional ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProdutoFichaNutricionalJpaEntity buscarFicha(Long produtoId) {
        garantirProduto(produtoId);
        return fichaRepo.findById(produtoId).orElse(null);
    }

    public ProdutoFichaNutricionalJpaEntity salvarFicha(Long produtoId, FichaNutricionalRequest req) {
        garantirProduto(produtoId);
        var ficha = fichaRepo.findById(produtoId)
                .orElseGet(() -> ProdutoFichaNutricionalJpaEntity.builder().produtoId(produtoId).build());
        ficha.setPorcaoG(req.porcaoG());
        ficha.setCalorias(req.calorias());
        ficha.setProteinasG(req.proteinasG());
        ficha.setCarboidratosG(req.carboidratosG());
        ficha.setGordurasG(req.gordurasG());
        ficha.setGordurasSaturadasG(req.gordurasSaturadasG());
        ficha.setFibrasG(req.fibrasG());
        ficha.setSodioMg(req.sodioMg());
        ficha.setAcucaresG(req.acucaresG());
        if (req.semGluten()   != null) ficha.setSemGluten(req.semGluten());
        if (req.semLactose()  != null) ficha.setSemLactose(req.semLactose());
        if (req.vegano()      != null) ficha.setVegano(req.vegano());
        if (req.vegetariano() != null) ficha.setVegetariano(req.vegetariano());
        ficha.setAlergenos(req.alergenos());
        return fichaRepo.save(ficha);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private void garantirProduto(Long id) {
        if (!produtoRepo.existsById(id)) throw new ProdutoNotFoundException(id);
    }

    private VarianteResponse toVarianteResponse(ProdutoVarianteJpaEntity e) {
        return new VarianteResponse(e.getId(), e.getProdutoId(), e.getSku(), e.getNome(),
                e.getPreco(), readMap(e.getAtributosJson()), e.isDisponivel());
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalArgumentException("JSON serialization failed", e); }
    }

    private Map<String, String> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return Map.of(); }
    }
}
