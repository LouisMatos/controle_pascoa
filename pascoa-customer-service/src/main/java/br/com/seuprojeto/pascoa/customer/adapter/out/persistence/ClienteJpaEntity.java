package br.com.seuprojeto.pascoa.customer.adapter.out.persistence;

import br.com.seuprojeto.pascoa.customer.domain.model.PreferenciaCanal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
public class ClienteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 14)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "endereco_entrega", length = 300)
    private String enderecoEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferencia_canal", length = 20)
    private PreferenciaCanal preferenciaCanal;

    @Column(name = "pontos_fidelidade", nullable = false)
    private int pontosFidelidade = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
