package br.com.seuprojeto.pascoa.order.adapter.out.client;

import br.com.seuprojeto.pascoa.order.application.port.out.ClienteServicePort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteServiceAdapter implements ClienteServicePort {

    private final ClienteFeignClient feignClient;

    @Override
    public Optional<ClienteInfo> findById(Long clienteId) {
        try {
            Map<String, Object> response = feignClient.getCliente(clienteId);
            return Optional.of(new ClienteInfo(
                    toLong(response.get("id")),
                    (String) response.get("nome"),
                    (String) response.get("email")
            ));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro ao consultar cliente {}: {}", clienteId, e.getMessage());
            return Optional.empty();
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Long l) return l;
        return Long.parseLong(String.valueOf(value));
    }
}
