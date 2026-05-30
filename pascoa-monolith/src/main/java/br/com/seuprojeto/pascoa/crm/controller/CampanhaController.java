package br.com.seuprojeto.pascoa.crm.controller;

import br.com.seuprojeto.pascoa.crm.entity.SegmentoCliente;
import br.com.seuprojeto.pascoa.crm.service.CampanhaQueue;
import br.com.seuprojeto.pascoa.crm.service.CampanhaService;
import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/crm/campanhas")
@RequiredArgsConstructor
public class CampanhaController {

    private final CampanhaService campanhaService;
    private final CampanhaQueue campanhaQueue;

    @GetMapping
    public String form(Model model) {
        model.addAttribute("segmentos", SegmentoCliente.values());
        model.addAttribute("canais", CanalNotificacao.values());
        model.addAttribute("pendentes", campanhaQueue.pendentes());
        model.addAttribute("totalEnviados", campanhaQueue.totalEnviados());
        model.addAttribute("totalFalhas", campanhaQueue.totalFalhas());
        return "crm/campanha";
    }

    @PostMapping("/disparar")
    public String disparar(@RequestParam(required = false) SegmentoCliente segmento,
                           @RequestParam CanalNotificacao canal,
                           @RequestParam(required = false) String assunto,
                           @RequestParam String mensagem,
                           RedirectAttributes ra) {
        if (mensagem == null || mensagem.isBlank()) {
            ra.addFlashAttribute("erro", "A mensagem não pode estar vazia.");
            return "redirect:/crm/campanhas";
        }
        int enfileirados = campanhaService.disparar(segmento, canal, assunto, mensagem);
        if (enfileirados == 0) {
            ra.addFlashAttribute("aviso",
                "Nenhum cliente elegível encontrado para o segmento/canal selecionado.");
        } else {
            ra.addFlashAttribute("sucesso",
                enfileirados + " mensagens enfileiradas. Serão enviadas a ~10/minuto.");
        }
        return "redirect:/crm/campanhas";
    }
}
