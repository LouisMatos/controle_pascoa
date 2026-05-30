package br.com.seuprojeto.pascoa.inventory.domain.exception;

public class MateriaPrimaNotFoundException extends RuntimeException {
    public MateriaPrimaNotFoundException(Long id) {
        super("Matéria-prima não encontrada: " + id);
    }
}
