package br.com.seuprojeto.pascoa.seguranca.controller;

import br.com.seuprojeto.pascoa.seguranca.dto.UsuarioForm;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** Converte strings em branco para null antes da validação — permite @Email opcional. */
    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new UsuarioForm());
        model.addAttribute("roles", Role.values());
        return "usuarios/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Usuario u = usuarioService.buscarPorId(id);
        UsuarioForm form = new UsuarioForm();
        form.setId(u.getId());
        form.setNome(u.getNome());
        form.setLogin(u.getLogin());
        form.setEmail(u.getEmail());
        form.setRole(u.getRole());
        form.setAtivo(u.isAtivo());
        model.addAttribute("form", form);
        model.addAttribute("roles", Role.values());
        return "usuarios/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("form") UsuarioForm form,
                         BindingResult result, Model model, RedirectAttributes ra) {
        // Senha obrigatória na criação; se fornecida em qualquer caso, deve ter mínimo 8 caracteres
        if (form.getId() == null && (form.getSenha() == null || form.getSenha().isBlank())) {
            result.rejectValue("senha", "required", "Senha é obrigatória para novos usuários");
        } else if (form.getSenha() != null && !form.getSenha().isBlank() && form.getSenha().length() < 8) {
            result.rejectValue("senha", "size", "Senha deve ter no mínimo 8 caracteres");
        }
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "usuarios/form";
        }
        try {
            usuarioService.salvar(form);
            ra.addFlashAttribute("sucesso", "Usuário salvo com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes ra) {
        try {
            Usuario u = usuarioService.buscarPorId(id);
            if (u.getLogin().equals(authentication.getName())) {
                ra.addFlashAttribute("erro",
                    "Você não pode desativar seu próprio usuário.");
                return "redirect:/usuarios";
            }
            usuarioService.alternarAtivo(id);
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
