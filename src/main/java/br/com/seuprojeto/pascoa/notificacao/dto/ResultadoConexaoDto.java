package br.com.seuprojeto.pascoa.notificacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Resultado da verificação de conectividade com a Evolution API.
 */
@Data
@AllArgsConstructor
public class ResultadoConexaoDto {

    /** true quando a instância está com state = "open" */
    private boolean conectado;

    /** Valor exato do campo "state" retornado pela API (open, close, connecting…) */
    private String estado;

    /** Mensagem legível para exibir ao usuário */
    private String mensagem;

    public static ResultadoConexaoDto falha(String mensagem) {
        return new ResultadoConexaoDto(false, "erro", mensagem);
    }
}
