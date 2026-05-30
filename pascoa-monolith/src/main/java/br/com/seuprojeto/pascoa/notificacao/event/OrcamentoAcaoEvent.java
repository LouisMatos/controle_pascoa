package br.com.seuprojeto.pascoa.notificacao.event;

import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;

/**
 * Publicado quando um cliente aprova ou recusa um orçamento via link público.
 */
public record OrcamentoAcaoEvent(Orcamento orcamento, boolean aprovado) {}
