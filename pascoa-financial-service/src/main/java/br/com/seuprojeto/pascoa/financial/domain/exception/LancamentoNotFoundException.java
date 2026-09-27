package br.com.seuprojeto.pascoa.financial.domain.exception;

public class LancamentoNotFoundException extends RuntimeException {
    public LancamentoNotFoundException(Long id) {
        super("Lançamento não encontrado: " + id);
    }
}
