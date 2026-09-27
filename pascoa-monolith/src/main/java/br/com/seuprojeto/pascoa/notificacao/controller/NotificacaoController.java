package br.com.seuprojeto.pascoa.notificacao.controller;

import br.com.seuprojeto.pascoa.notificacao.dto.ResultadoConexaoDto;
import br.com.seuprojeto.pascoa.notificacao.entity.*;
import br.com.seuprojeto.pascoa.notificacao.repository.*;
import br.com.seuprojeto.pascoa.notificacao.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final TemplateNotificacaoRepository templateRepository;
    private final NotificacaoEnviadaRepository enviadaRepository;
    private final ConfiguracaoCanalRepository canalRepository;
    private final WhatsAppService whatsAppService;

    // -----------------------------------------------------------------------
    // Templates
    // -----------------------------------------------------------------------

    @GetMapping("/templates")
    public String listarTemplates(Model model) {
        model.addAttribute("templates", templateRepository.findAllByOrderByEventoGatilhoAscCanalAsc());
        model.addAttribute("eventos", EventoNotificacao.values());
        model.addAttribute("canais", CanalNotificacao.values());
        model.addAttribute("novo", new TemplateNotificacao());
        return "notificacoes/templates";
    }

    @PostMapping("/templates/salvar")
    public String salvarTemplate(@ModelAttribute TemplateNotificacao templateForm,
                                  RedirectAttributes ra) {
        if (templateForm.getId() != null) {
            // Edição: preserva ativo (gerenciado pelo botão toggle, não pelo form)
            templateRepository.findById(templateForm.getId()).ifPresent(existente -> {
                existente.setEventoGatilho(templateForm.getEventoGatilho());
                existente.setCanal(templateForm.getCanal());
                existente.setAssunto(templateForm.getAssunto());
                existente.setCorpo(templateForm.getCorpo());
                existente.setVariaveis(templateForm.getVariaveis());
                templateRepository.save(existente);
            });
        } else {
            // Novo: ativo=true por padrão
            if (templateForm.getAtivo() == null) templateForm.setAtivo(true);
            templateRepository.save(templateForm);
        }
        ra.addFlashAttribute("sucesso", "Template salvo com sucesso.");
        return "redirect:/notificacoes/templates";
    }

    @GetMapping("/templates/{id}/ativar")
    public String ativarTemplate(@PathVariable Long id, RedirectAttributes ra) {
        templateRepository.findById(id).ifPresent(t -> {
            t.setAtivo(!Boolean.TRUE.equals(t.getAtivo()));
            templateRepository.save(t);
        });
        ra.addFlashAttribute("sucesso", "Status do template atualizado.");
        return "redirect:/notificacoes/templates";
    }

    @GetMapping("/templates/{id}/excluir")
    public String excluirTemplate(@PathVariable Long id, RedirectAttributes ra) {
        templateRepository.deleteById(id);
        ra.addFlashAttribute("sucesso", "Template excluído.");
        return "redirect:/notificacoes/templates";
    }

    // -----------------------------------------------------------------------
    // Histórico de envios
    // -----------------------------------------------------------------------

    @GetMapping("/historico")
    public String historico(Model model) {
        model.addAttribute("envios", enviadaRepository.findAllComPedido());
        return "notificacoes/historico";
    }

    // -----------------------------------------------------------------------
    // Configuração de canais
    // -----------------------------------------------------------------------

    @GetMapping("/configuracao")
    public String configuracao(Model model) {
        // garante que exista entrada para cada canal
        for (CanalNotificacao tipo : CanalNotificacao.values()) {
            if (canalRepository.findByTipo(tipo).isEmpty()) {
                canalRepository.save(ConfiguracaoCanal.builder().tipo(tipo).build());
            }
        }
        model.addAttribute("canais", canalRepository.findAll());
        return "notificacoes/configuracao";
    }

    @PostMapping("/configuracao/salvar")
    public String salvarConfiguracao(@ModelAttribute ConfiguracaoCanal configForm,
                                      RedirectAttributes ra) {
        // Carrega entidade existente para preservar o campo `tipo` (unique) e
        // aplicar null-safe para checkboxes não enviados pelo browser quando desmarcados.
        canalRepository.findById(configForm.getId()).ifPresent(canal -> {
            canal.setApiUrl(configForm.getApiUrl());
            canal.setApiKey(configForm.getApiKey());
            canal.setRemetente(configForm.getRemetente());
            canal.setAtivo(Boolean.TRUE.equals(configForm.getAtivo()));
            canal.setTestMode(Boolean.TRUE.equals(configForm.getTestMode()));
            canalRepository.save(canal);
        });
        ra.addFlashAttribute("sucesso", "Configuração salva com sucesso.");
        return "redirect:/notificacoes/configuracao";
    }

    // -----------------------------------------------------------------------
    // Item 5 — Teste e validação da Evolution API
    // -----------------------------------------------------------------------

    /**
     * Verifica o estado da instância WhatsApp na Evolution API.
     * Redireciona de volta para configuração com o resultado como flash attribute.
     */
    @PostMapping("/verificar-conexao")
    public String verificarConexao(RedirectAttributes ra) {
        var optConfig = canalRepository.findByTipo(CanalNotificacao.WHATSAPP);
        if (optConfig.isEmpty()) {
            ra.addFlashAttribute("testeErro", "Canal WhatsApp não configurado.");
            return "redirect:/notificacoes/configuracao";
        }
        ResultadoConexaoDto resultado = whatsAppService.verificarConexao(optConfig.get());
        if (resultado.isConectado()) {
            ra.addFlashAttribute("testeOk", "✔ " + resultado.getMensagem());
        } else {
            ra.addFlashAttribute("testeErro", "✘ " + resultado.getMensagem());
        }
        return "redirect:/notificacoes/configuracao";
    }

    /**
     * Envia uma mensagem de teste real para o número informado,
     * independente do testMode — apenas para validar a configuração.
     */
    @PostMapping("/enviar-teste")
    public String enviarTeste(@RequestParam String numeroTeste,
                               @RequestParam(required = false, defaultValue = "") String mensagemTeste,
                               RedirectAttributes ra) {
        var optConfig = canalRepository.findByTipo(CanalNotificacao.WHATSAPP);
        if (optConfig.isEmpty()) {
            ra.addFlashAttribute("testeErro", "Canal WhatsApp não configurado.");
            return "redirect:/notificacoes/configuracao";
        }
        ConfiguracaoCanal config = optConfig.get();

        if (config.getApiUrl() == null || config.getApiUrl().isBlank()
                || config.getRemetente() == null || config.getRemetente().isBlank()) {
            ra.addFlashAttribute("testeErro",
                "Preencha a URL da API e o nome da instância antes de testar.");
            return "redirect:/notificacoes/configuracao";
        }

        String texto = mensagemTeste.isBlank()
            ? "🧪 Mensagem de teste do Sistema Páscoa — " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : mensagemTeste;

        try {
            whatsAppService.enviarMensagemDeTeste(config, numeroTeste, texto);
            ra.addFlashAttribute("testeOk",
                "✔ Mensagem de teste enviada para " + whatsAppService.formatarNumero(numeroTeste)
                + ". Verifique o WhatsApp!");
        } catch (Exception e) {
            ra.addFlashAttribute("testeErro", "✘ Falha ao enviar: " + e.getMessage());
        }
        return "redirect:/notificacoes/configuracao";
    }
}
