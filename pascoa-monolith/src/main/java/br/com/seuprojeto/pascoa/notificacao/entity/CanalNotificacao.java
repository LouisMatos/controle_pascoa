package br.com.seuprojeto.pascoa.notificacao.entity;

public enum CanalNotificacao {
    WHATSAPP,
    EMAIL,
    /** Canal de SMS — usado como fallback quando WhatsApp falha. Requer configuração de provedor. */
    SMS
}
