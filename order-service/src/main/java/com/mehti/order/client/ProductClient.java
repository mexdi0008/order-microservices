package com.mehti.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/** HTTP client for product-service. Translates remote status codes into domain exceptions. */
@Component
public class ProductClient {

	public record ProductDto(Long id, String name, BigDecimal price, int stock) {
	}

	private final RestClient http;

	public ProductClient(RestClient.Builder builder, @Value("${product-service.url}") String baseUrl) {
		SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
		timeouts.setConnectTimeout(2_000);
		timeouts.setReadTimeout(5_000);
		this.http = builder.baseUrl(baseUrl).requestFactory(timeouts).build();
	}

	/** Reserves stock and returns the product snapshot (name, price) used for the order. */
	public ProductDto reserve(Long productId, int quantity) {
		try {
			return http.post().uri("/api/products/{id}/reserve", productId)
					.body(Map.of("quantity", quantity))
					.retrieve()
					.onStatus(s -> s.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
						throw new ProductUnavailableException(HttpStatus.NOT_FOUND, "Product " + productId + " not found");
					})
					.onStatus(s -> s.value() == HttpStatus.CONFLICT.value(), (req, res) -> {
						throw new ProductUnavailableException(HttpStatus.CONFLICT, "Insufficient stock for product " + productId);
					})
					.body(ProductDto.class);
		} catch (ResourceAccessException e) {
			throw new ProductUnavailableException(HttpStatus.SERVICE_UNAVAILABLE, "Product service is unavailable");
		}
	}
}
