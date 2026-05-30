package br.com.seuprojeto.pascoa.qualidade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemVerificadoDto {
    private Long checklistItemId;
    private String descricao;
    private boolean verificado;
}
