package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.ProductDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(WebClient.Builder webClientBuilder, @Value("${product.service.url}") String productServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(productServiceUrl)
                .build();
    }

    public List<ProductDTO> getProductsByIds(List<Long> productIds) {
        return webClient.post()
                .uri("/api/products/batch")
                .bodyValue(productIds)
                .retrieve()
                .bodyToFlux(ProductDTO.class)
                .collectList()
                .block();
    }

    public ProductDTO getProductById(Long productId) {
        return webClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDTO.class)
                .block();
    }
}
