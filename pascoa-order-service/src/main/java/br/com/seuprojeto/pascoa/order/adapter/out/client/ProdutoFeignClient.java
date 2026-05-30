package br.com.seuprojeto.pascoa.order.adapter.out.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "pascoa-product-service", url = "${clients.product-service.url:http://localhost:8084}")
public interface ProdutoFeignClient {

    @GetMapping("/api/produtos/{id}")
    Map<String, Object> getProduto(@PathVariable Long id);
}
