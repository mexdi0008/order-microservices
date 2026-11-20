package com.mehti.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Seeds a few products on first start so the demo works out of the box. */
@Component
@Profile("!test")
class SampleData implements CommandLineRunner {

	private final ProductRepository products;

	SampleData(ProductRepository products) {
		this.products = products;
	}

	@Override
	public void run(String... args) {
		if (products.count() == 0) {
			products.saveAll(List.of(
					new Product("Mechanical keyboard", new BigDecimal("89.90"), 25),
					new Product("Wireless mouse", new BigDecimal("24.50"), 100),
					new Product("27\" monitor", new BigDecimal("249.00"), 10)));
		}
	}
}
