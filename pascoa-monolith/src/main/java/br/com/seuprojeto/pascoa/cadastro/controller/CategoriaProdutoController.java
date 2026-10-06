package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.service.CategoriaProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaProdutoController {

    private final CategoriaProdutoService service;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", service.listarTodas());
        return "categorias/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("categoria", new CategoriaProduto());
        return "categorias/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("categoria", service.buscarPorId(id));
        return "categorias/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("categoria") CategoriaProduto categoria,
                         BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "categorias/form";
        }
        try {
            service.salvar(categoria);
        } catch (IllegalArgumentException e) {
            result.rejectValue("nome", "duplicada", e.getMessage());
            return "categorias/form";
        }
        ra.addFlashAttribute("sucesso", "Categoria salva com sucesso!");
        return "redirect:/categorias";
    }

    @PostMapping("/{id}/ativar")
    public String alternarAtivo(@PathVariable Long id, RedirectAttributes ra) {
        service.alternarAtivo(id);
        ra.addFlashAttribute("sucesso", "Status da categoria alterado!");
        return "redirect:/categorias";
    }
}
