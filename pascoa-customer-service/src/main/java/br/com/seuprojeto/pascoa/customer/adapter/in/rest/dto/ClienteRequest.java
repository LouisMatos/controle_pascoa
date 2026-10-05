package br.com.seuprojeto.pascoa.customer.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.customer.domain.model.PreferenciaCanal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        String telefone,
        String cpf,
        LocalDate dataNascimento,
        String enderecoEntrega,
        @NotNull PreferenciaCanal preferenciaCanal
) {}
