package br.com.seuprojeto.pascoa.producao.controller;

import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import br.com.seuprojeto.pascoa.producao.service.ProducaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Interface mobile-first do operador de produção — FASE 9.6 do design v7.
 *
 * Renderiza a tela {@code producao/mobile.html} otimizada para celular
 * (touch targets >= 44px, lista vertical de cards grandes, botão "Avançar
 * fase" no rodapé fixo). Reaproveita as ações POST do
 * {@link ProducaoController} para iniciar/concluir ordens.
 */
@Controller
@RequestMapping("/producao/mobile")
@RequiredArgsConstructor
public class ProducaoMobileController {

    private final ProducaoService producaoService;

    @GetMapping
    public String mobile(Model model) {
        model.addAttribute("pendentes",   producaoService.listarPorStatus(StatusOrdem.PENDENTE));
        model.addAttribute("emAndamento", producaoService.listarPorStatus(StatusOrdem.EM_ANDAMENTO));
        return "producao/mobile";
    }
}
