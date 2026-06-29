package br.com.seuprojeto.pascoa.tenant.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "white_label_config", schema = "platform")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhiteLabelConfig {

    @Id
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "nome_app", length = 100)
    private String nomeApp;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cor_primaria", length = 7)
    private String corPrimaria;

    @Column(name = "cor_secundaria", length = 7)
    private String corSecundaria;

    @Column(length = 500)
    private String favicon;

    @Column(length = 500)
    private String rodape;
}
