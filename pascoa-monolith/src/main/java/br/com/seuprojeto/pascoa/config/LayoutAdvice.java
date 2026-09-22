package br.com.seuprojeto.pascoa.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutAdvice {

	@ModelAttribute("activeGroup")
	public String activeGroup(HttpServletRequest request) {
		String uri = request.getRequestURI();

		if (uri.startsWith("/clientes") || uri.startsWith("/produtos") ||
		    uri.startsWith("/materias") || uri.startsWith("/fornecedores")) {
			return "cadastros";
		}
		if (uri.startsWith("/pedidos") || uri.startsWith("/orcamentos") || uri.startsWith("/crm")) {
			return "comercial";
		}
		if (uri.startsWith("/producao") || uri.startsWith("/qualidade")) {
			return "producao";
		}
		if (uri.startsWith("/estoque")) return "estoque";
		if (uri.startsWith("/financeiro") || uri.startsWith("/gastos") || uri.startsWith("/analytics")) {
			return "financeiro";
		}
		if (uri.startsWith("/usuarios") || uri.startsWith("/notificacoes") ||
		    uri.startsWith("/auditoria") || uri.startsWith("/lgpd")) {
			return "admin";
		}

		return null;
	}
}
