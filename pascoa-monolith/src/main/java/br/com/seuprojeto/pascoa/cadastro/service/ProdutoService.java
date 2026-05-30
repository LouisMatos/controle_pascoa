package br.com.seuprojeto.pascoa.cadastro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import br.com.seuprojeto.pascoa.shared.service.FileValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository repository;
    private final FileValidationService fileValidationService;

    @Value("${app.upload.dir:${user.home}/pascoa-uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<Produto> listarTodos() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Produto> listarAtivos() {
        return repository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Produto> buscar(String nome) {
        return repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome);
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
    }

    @Transactional
    public Produto salvar(Produto produto) {
        return salvar(produto, null);
    }

    @Transactional
    public Produto salvar(Produto produto, MultipartFile fotoFile) {
        if (produto.getAtivo() == null) {
            produto.setAtivo(true);
        }
        if (fotoFile != null && !fotoFile.isEmpty()) {
            produto.setFoto(salvarFoto(fotoFile));
        }
        return repository.save(produto);
    }

    private String salvarFoto(MultipartFile file) {
        FileValidationService.ValidationResult info = fileValidationService.validate(file);
        String nomeFoto = UUID.randomUUID() + "." + info.extension();
        Path destino = Paths.get(uploadDir).resolve(nomeFoto);
        try (InputStream is = file.getInputStream()) {
            Files.createDirectories(destino.getParent());
            Files.copy(is, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar foto: " + e.getMessage(), e);
        }
        return nomeFoto;
    }

    @Transactional
    public void excluir(Long id) {
        Produto produto = buscarPorId(id);
        repository.delete(produto);
    }

    @Transactional
    public void alternarAtivo(Long id) {
        Produto produto = buscarPorId(id);
        produto.setAtivo(!produto.getAtivo());
        repository.save(produto);
    }
}
