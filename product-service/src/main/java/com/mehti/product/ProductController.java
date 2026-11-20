package com.mehti.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	public record ProductRequest(@NotBlank @Size(max = 150) String name,
								 @NotNull @DecimalMin("0.01") BigDecimal price,
								 @PositiveOrZero int stock) {
	}

	public record ReserveRequest(@Positive int quantity) {
	}

	public record ProductResponse(Long id, String name, BigDecimal price, int stock) {
		static ProductResponse from(Product p) {
			return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
		}
	}

	private final ProductRepository products;

	public ProductController(ProductRepository products) {
		this.products = products;
	}

	@GetMapping
	public List<ProductResponse> list() {
		return products.findAll().stream().map(ProductResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable Long id) {
		return ProductResponse.from(find(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse create(@Valid @RequestBody ProductRequest request) {
		return ProductResponse.from(products.save(new Product(request.name().trim(), request.price(), request.stock())));
	}

	/** Called by order-service. 200 = reserved, 404 = unknown product, 409 = insufficient stock. */
	@PostMapping("/{id}/reserve")
	@Transactional
	public ProductResponse reserve(@PathVariable Long id, @Valid @RequestBody ReserveRequest request) {
		find(id);
		if (products.reserve(id, request.quantity()) == 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for product " + id);
		}
		return ProductResponse.from(find(id));
	}

	@ExceptionHandler(ResponseStatusException.class)
	ProblemDetail handle(ResponseStatusException ex) {
		return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
	}

	private Product find(Long id) {
		return products.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
	}
}
