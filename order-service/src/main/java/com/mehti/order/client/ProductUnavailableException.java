package com.mehti.order.client;

import org.springframework.http.HttpStatus;

public class ProductUnavailableException extends RuntimeException {

	private final HttpStatus status;

	public ProductUnavailableException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
