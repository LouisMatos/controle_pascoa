package br.com.seuprojeto.pascoa.common.quantidade;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("fmt")
public class QuantidadeFormatter {

    public String quantidade(BigDecimal valor) {
        return Quantidades.formatar(valor);
    }

    public String quantidade(BigDecimal valor, UnidadeVenda unidade) {
        return Quantidades.formatar(valor, unidade);
    }
}
