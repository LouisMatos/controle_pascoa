package br.com.seuprojeto.pascoa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Endpoint público da página de manutenção — sem restrição de autenticação.
 * A proteção de acesso é feita via SecurityConfig (permitAll) e MaintenanceFilter.
 */
@Controller
@RequiredArgsConstructor
public class ManutencaoController {

    private final ConfiguracaoSistemaRepository sistemaRepository;

    @GetMapping("/manutencao")
    public String pagina(Model model) {
        sistemaRepository.findById(1L).ifPresent(c -> {
            model.addAttribute("mensagem", c.getMensagemManutencao());
            model.addAttribute("previsaoRetorno", c.getPrevisaoRetorno());
        });
        return "manutencao";
    }
}
