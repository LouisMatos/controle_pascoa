package br.com.seuprojeto.pascoa.seguranca.dto;

import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioForm {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100)
    private String nome;

    @NotBlank(message = "Login é obrigatório")
    @Size(max = 60)
    private String login;

    @Email(message = "E-mail inválido")
    @Size(max = 150)
    private String email;

    // Obrigatória apenas na criação; em branco = manter senha atual
    @Size(min = 0, max = 100)
    private String senha;

    @NotNull(message = "Selecione o perfil")
    private Role role;

    private boolean ativo = true;
}
