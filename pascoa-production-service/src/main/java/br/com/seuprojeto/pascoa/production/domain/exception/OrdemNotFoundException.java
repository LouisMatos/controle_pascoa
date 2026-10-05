package br.com.seuprojeto.pascoa.production.domain.exception;

public class OrdemNotFoundException extends RuntimeException {
    public OrdemNotFoundException(Long id) {
        super("Ordem de produção não encontrada: " + id);
    }
}
