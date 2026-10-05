package br.com.seuprojeto.pascoa.order.adapter.out.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "pascoa-customer-service", url = "${clients.customer-service.url:http://localhost:8082}")
public interface ClienteFeignClient {

    @GetMapping("/api/clientes/{id}")
    Map<String, Object> getCliente(@PathVariable Long id);
}
