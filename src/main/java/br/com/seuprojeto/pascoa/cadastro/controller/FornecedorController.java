package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.Fornecedor;
import br.com.seuprojeto.pascoa.cadastro.service.FornecedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fornecedores")
@RequiredArgsConstructor
public class FornecedorController {

    private final FornecedorService service;

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        var fornecedores = (busca != null && !busca.isBlank())
            ? service.buscar(busca.trim())
            : service.listarTodos();
        model.addAttribute("fornecedores", fornecedores);
        model.addAttribute("busca", busca);
        return "fornecedores/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("fornecedor", new Fornecedor());
        return "fornecedores/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("fornecedor", service.buscarPorId(id));
        return "fornecedores/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("fornecedor") Fornecedor fornecedor,
                         BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "fornecedores/form";
        }
        service.salvar(fornecedor);
        ra.addFlashAttribute("sucesso", "Fornecedor salvo com sucesso!");
        return "redirect:/fornecedores";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.excluir(id);
            ra.addFlashAttribute("sucesso", "Fornecedor excluído com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Não foi possível excluir: " + e.getMessage());
        }
        return "redirect:/fornecedores";
    }
}
