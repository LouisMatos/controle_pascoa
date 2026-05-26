package br.com.seuprojeto.pascoa.cadastro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    @Value("${app.upload.dir:${user.home}/pascoa-uploads}")
    private String uploadDir;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

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
        try {
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            if (ext == null || ext.isBlank()) ext = "jpg";
            String nomeFoto = UUID.randomUUID() + "." + ext;
            Path destino = Paths.get(uploadDir).resolve(nomeFoto);
            Files.createDirectories(destino.getParent());
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return nomeFoto;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar foto: " + e.getMessage(), e);
        }
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
