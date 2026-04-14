package com.felipe.webhook_listener_api.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class InvalidPayloadException extends RuntimeException {

	private final Map<String, String> validationErrors;

	public InvalidPayloadException(String message) {
		this(message, null);
	}

	public InvalidPayloadException(String message, Map<String, String> validationErrors) {
		super(message);
		this.validationErrors = validationErrors;
	}
}
