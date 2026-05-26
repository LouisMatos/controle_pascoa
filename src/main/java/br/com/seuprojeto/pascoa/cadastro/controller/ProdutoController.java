package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService service;

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        var produtos = (busca != null && !busca.isBlank())
            ? service.buscar(busca.trim())
            : service.listarTodos();
        model.addAttribute("produtos", produtos);
        model.addAttribute("busca", busca);
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("produto", new Produto());
        model.addAttribute("categorias", Categoria.values());
        return "produtos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("produto", service.buscarPorId(id));
        model.addAttribute("categorias", Categoria.values());
        return "produtos/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("produto") Produto produto,
                         BindingResult result,
                         @RequestParam("fotoFile") MultipartFile fotoFile,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categorias", Categoria.values());
            return "produtos/form";
        }
        try {
            service.salvar(produto, fotoFile);
            ra.addFlashAttribute("sucesso", "Produto salvo com sucesso!");
        } catch (Exception e) {
            model.addAttribute("categorias", Categoria.values());
            model.addAttribute("erroFoto", "Erro ao salvar foto: " + e.getMessage());
            return "produtos/form";
        }
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.excluir(id);
            ra.addFlashAttribute("sucesso", "Produto excluído com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Não foi possível excluir: " + e.getMessage());
        }
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/ativar")
    public String alternarAtivo(@PathVariable Long id, RedirectAttributes ra) {
        service.alternarAtivo(id);
        ra.addFlashAttribute("sucesso", "Status do produto alterado!");
        return "redirect:/produtos";
    }
}
