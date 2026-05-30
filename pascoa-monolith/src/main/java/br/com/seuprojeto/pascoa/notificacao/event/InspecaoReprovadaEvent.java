package br.com.seuprojeto.pascoa.notificacao.event;

import br.com.seuprojeto.pascoa.qualidade.entity.InspecaoQualidade;

/**
 * Publicado quando uma inspeção de qualidade é registrada com resultado reprovado.
 */
public record InspecaoReprovadaEvent(InspecaoQualidade inspecao) {}
