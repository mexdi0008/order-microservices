package com.mehti.order;

import com.mehti.order.client.ProductClient;
import com.mehti.order.client.ProductUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private OrderRepository orders;
	@MockitoBean
	private ProductClient productClient;

	@Test
	void createsOrderWithPriceFromProductService() throws Exception {
		when(productClient.reserve(eq(1L), anyInt()))
				.thenReturn(new ProductClient.ProductDto(1L, "Wireless mouse", new BigDecimal("24.50"), 97));

		order(1L, 3)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.productName").value("Wireless mouse"))
				.andExpect(jsonPath("$.total").value(73.50));
	}

	@Test
	void doesNotStoreOrderWhenStockIsInsufficient() throws Exception {
		when(productClient.reserve(eq(2L), anyInt()))
				.thenThrow(new ProductUnavailableException(HttpStatus.CONFLICT, "Insufficient stock for product 2"));
		long before = orders.count();

		order(2L, 5).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Insufficient stock for product 2"));

		org.assertj.core.api.Assertions.assertThat(orders.count()).isEqualTo(before);
	}

	@Test
	void returns503WhenProductServiceIsDown() throws Exception {
		when(productClient.reserve(eq(3L), anyInt()))
				.thenThrow(new ProductUnavailableException(HttpStatus.SERVICE_UNAVAILABLE, "Product service is unavailable"));

		order(3L, 1).andExpect(status().isServiceUnavailable());
	}

	@Test
	void validatesQuantity() throws Exception {
		order(1L, 0).andExpect(status().isBadRequest());
	}

	private org.springframework.test.web.servlet.ResultActions order(long productId, int quantity) throws Exception {
		return mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"));
	}
}
