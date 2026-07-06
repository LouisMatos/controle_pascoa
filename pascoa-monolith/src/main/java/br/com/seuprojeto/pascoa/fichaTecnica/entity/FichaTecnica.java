package br.com.seuprojeto.pascoa.fichaTecnica.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.Unidade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fichas_tecnicas")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichaTecnica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false, unique = true)
    @ToString.Exclude
    private Produto produto;

    @NotNull(message = "Rendimento é obrigatório")
    @DecimalMin(value = "0.001", message = "Rendimento deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal rendimento;

    @NotNull(message = "Unidade do rendimento é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_rendimento", nullable = false, length = 5)
    private Unidade unidadeRendimento;

    @Size(max = 500)
    @Column(length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "fichaTecnica", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<FichaTecnicaItem> itens = new ArrayList<>();
}
