package br.com.seuprojeto.pascoa.order.adapter.out.client;

import br.com.seuprojeto.pascoa.order.application.port.out.ProdutoServicePort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProdutoServiceAdapter implements ProdutoServicePort {

    private final ProdutoFeignClient feignClient;

    @Override
    public Optional<ProdutoInfo> findById(Long produtoId) {
        try {
            Map<String, Object> r = feignClient.getProduto(produtoId);
            return Optional.of(new ProdutoInfo(
                    toLong(r.get("id")),
                    (String) r.get("nome"),
                    new BigDecimal(String.valueOf(r.get("preco"))),
                    Boolean.TRUE.equals(r.get("disponivel"))
            ));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro ao consultar produto {}: {}", produtoId, e.getMessage());
            return Optional.empty();
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Long l) return l;
        return Long.parseLong(String.valueOf(value));
    }
}
