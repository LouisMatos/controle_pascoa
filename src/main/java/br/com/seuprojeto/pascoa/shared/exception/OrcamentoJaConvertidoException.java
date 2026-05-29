package br.com.seuprojeto.pascoa.shared.exception;

public class OrcamentoJaConvertidoException extends RuntimeException {

    private final Long pedidoId;

    public OrcamentoJaConvertidoException(Long pedidoId) {
        super("Este orçamento já foi convertido no pedido #" + pedidoId + ".");
        this.pedidoId = pedidoId;
    }

    public Long getPedidoId() {
        return pedidoId;
    }
}
