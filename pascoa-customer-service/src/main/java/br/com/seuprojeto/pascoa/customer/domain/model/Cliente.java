package br.com.seuprojeto.pascoa.customer.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;

@Getter
@Builder
@With
public class Cliente {

    private final Long id;
    private final String nome;
    private final String email;
    private final String telefone;
    private final String cpf;
    private final LocalDate dataNascimento;
    private final String enderecoEntrega;
    private final PreferenciaCanal preferenciaCanal;
    private final int pontosFidelidade;
    private final boolean ativo;

    public boolean fazcAniversarioHoje() {
        if (dataNascimento == null) return false;
        LocalDate hoje = LocalDate.now();
        return dataNascimento.getDayOfMonth() == hoje.getDayOfMonth()
                && dataNascimento.getMonth() == hoje.getMonth();
    }

    public Cliente adicionarPontos(int pontos) {
        return this.withPontosFidelidade(this.pontosFidelidade + pontos);
    }

    public Cliente inativar() {
        return this.withAtivo(false);
    }
}
