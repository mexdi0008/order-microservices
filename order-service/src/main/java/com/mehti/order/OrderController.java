package com.mehti.order;

import com.mehti.order.client.ProductClient;
import com.mehti.order.client.ProductUnavailableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	public record OrderRequest(@NotNull Long productId, @Positive @Max(100) int quantity) {
	}

	public record OrderResponse(Long id, Long productId, String productName, int quantity,
								BigDecimal unitPrice, BigDecimal total, Instant createdAt) {
		static OrderResponse from(Order o) {
			return new OrderResponse(o.getId(), o.getProductId(), o.getProductName(), o.getQuantity(),
					o.getUnitPrice(), o.getTotal(), o.getCreatedAt());
		}
	}

	private final OrderRepository orders;
	private final ProductClient productClient;

	public OrderController(OrderRepository orders, ProductClient productClient) {
		this.orders = orders;
		this.productClient = productClient;
	}

	/** Reserves stock in product-service first; the order is stored only if the reservation succeeded. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse create(@Valid @RequestBody OrderRequest request) {
		ProductClient.ProductDto product = productClient.reserve(request.productId(), request.quantity());
		Order order = orders.save(new Order(product.id(), product.name(), request.quantity(), product.price()));
		return OrderResponse.from(order);
	}

	@GetMapping("/{id}")
	public OrderResponse get(@PathVariable Long id) {
		return orders.findById(id).map(OrderResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
	}

	@GetMapping
	public List<OrderResponse> list() {
		return orders.findAll().stream().map(OrderResponse::from).toList();
	}

	@ExceptionHandler(ProductUnavailableException.class)
	ProblemDetail handleProduct(ProductUnavailableException ex) {
		return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	ProblemDetail handleStatus(ResponseStatusException ex) {
		return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
	}
}
