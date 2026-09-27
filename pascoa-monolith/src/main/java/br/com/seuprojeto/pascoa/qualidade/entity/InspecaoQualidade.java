package br.com.seuprojeto.pascoa.qualidade.entity;

import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.qualidade.dto.ItemVerificadoDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "inspecao_qualidade")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspecaoQualidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_producao_id", nullable = false)
    @ToString.Exclude
    private OrdemProducao ordemProducao;

    @Column(name = "data_inspecao", nullable = false)
    private LocalDateTime dataInspecao;

    @Column(nullable = false, length = 100)
    private String inspetor;

    @Column(nullable = false)
    private Boolean aprovado;

    @Column(length = 1000)
    private String observacoes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "itens_verificados", columnDefinition = "jsonb")
    private List<ItemVerificadoDto> itensVerificados;

    @PrePersist
    private void prePersist() {
        if (this.dataInspecao == null) { this.dataInspecao = LocalDateTime.now(); }
    }

    public long totalVerificados() {
        if (itensVerificados == null) { return 0; }
        return itensVerificados.stream().filter(ItemVerificadoDto::isVerificado).count();
    }

    public long totalItens() {
        return itensVerificados == null ? 0 : itensVerificados.size();
    }
}
