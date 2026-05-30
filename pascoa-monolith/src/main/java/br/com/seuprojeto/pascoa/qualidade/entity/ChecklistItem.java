package br.com.seuprojeto.pascoa.qualidade.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checklist_qualidade")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @ToString.Exclude
    private Produto produto;

    @Column(nullable = false, length = 300)
    private String item;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
