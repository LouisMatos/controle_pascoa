package br.com.seuprojeto.pascoa.cadastro.entity;

import br.com.seuprojeto.pascoa.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fornecedores")
@Data
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fornecedor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 18)
    @Column(length = 18)
    private String cnpj;

    @Size(max = 20)
    @Column(length = 20)
    private String telefone;

    @Email(message = "E-mail inválido")
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @Size(max = 500)
    @Column(length = 500)
    private String observacoes;
}
