package br.com.seuprojeto.pascoa.crm.entity;

import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;

public record CampanhaItem(
        Long clienteId,
        String nomeCliente,
        String destinatario,
        CanalNotificacao canal,
        String assunto,
        String mensagem
) {}
