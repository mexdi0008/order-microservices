package com.mehti.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiTest {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper json;

	@Test
	void createsAndReservesStock() throws Exception {
		long id = create("Desk lamp", 3);

		reserve(id, 2).andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(1));
		reserve(id, 2).andExpect(status().isConflict());
		reserve(999_999, 1).andExpect(status().isNotFound());
		mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(1));
	}

	@Test
	void concurrentReservationsNeverOversell() throws Exception {
		long id = create("Limited item", 10);
		AtomicInteger succeeded = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(8);
		for (int i = 0; i < 30; i++) {
			pool.submit(() -> {
				try {
					if (reserve(id, 1).andReturn().getResponse().getStatus() == 200) succeeded.incrementAndGet();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			});
		}
		pool.shutdown();
		assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

		assertThat(succeeded.get()).isEqualTo(10);
		mvc.perform(get("/api/products/" + id)).andExpect(jsonPath("$.stock").value(0));
	}

	@Test
	void validatesInput() throws Exception {
		mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\",\"price\":0,\"stock\":-1}"))
				.andExpect(status().isBadRequest());
	}

	private long create(String name, int stock) throws Exception {
		String body = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\",\"price\":19.99,\"stock\":" + stock + "}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("id").asLong();
	}

	private org.springframework.test.web.servlet.ResultActions reserve(long id, int qty) throws Exception {
		return mvc.perform(post("/api/products/" + id + "/reserve").contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":" + qty + "}"));
	}
}
