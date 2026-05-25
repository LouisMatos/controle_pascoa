package br.com.seuprojeto.pascoa.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public String handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex,
                                              RedirectAttributes ra,
                                              HttpServletRequest request) {
        ra.addFlashAttribute("erro", ex.getMessage());
        return redirectBack(request);
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public String handleEstoqueInsuficiente(EstoqueInsuficienteException ex,
                                             RedirectAttributes ra,
                                             HttpServletRequest request) {
        ra.addFlashAttribute("erro", ex.getMessage());
        return redirectBack(request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex,
                                      RedirectAttributes ra,
                                      HttpServletRequest request) {
        ra.addFlashAttribute("erro", ex.getMessage());
        return redirectBack(request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex,
                                       RedirectAttributes ra,
                                       HttpServletRequest request) {
        ra.addFlashAttribute("erro",
            "Não é possível excluir este registro pois ele está vinculado a outros dados do sistema.");
        return redirectBack(request);
    }

    // -----------------------------------------------------------------------
    // Helper — redireciona para a página anterior (Referer) ou dashboard
    // -----------------------------------------------------------------------

    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isBlank() ? referer : "/dashboard");
    }
}
