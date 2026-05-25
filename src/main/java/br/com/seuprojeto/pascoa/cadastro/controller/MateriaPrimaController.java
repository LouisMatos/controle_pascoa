package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import br.com.seuprojeto.pascoa.cadastro.entity.Unidade;
import br.com.seuprojeto.pascoa.cadastro.service.MateriaPrimaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/materias-primas")
@RequiredArgsConstructor
public class MateriaPrimaController {

    private final MateriaPrimaService service;

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        var mps = (busca != null && !busca.isBlank())
            ? service.buscar(busca.trim())
            : service.listarTodas();
        model.addAttribute("materiasPrimas", mps);
        model.addAttribute("busca", busca);
        return "materias-primas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("materiaPrima", new MateriaPrima());
        model.addAttribute("unidades", Unidade.values());
        return "materias-primas/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("materiaPrima", service.buscarPorId(id));
        model.addAttribute("unidades", Unidade.values());
        return "materias-primas/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("materiaPrima") MateriaPrima materiaPrima,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("unidades", Unidade.values());
            return "materias-primas/form";
        }
        service.salvar(materiaPrima);
        ra.addFlashAttribute("sucesso", "Matéria-prima salva com sucesso!");
        return "redirect:/materias-primas";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.excluir(id);
            ra.addFlashAttribute("sucesso", "Matéria-prima excluída com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Não foi possível excluir: " + e.getMessage());
        }
        return "redirect:/materias-primas";
    }
}
