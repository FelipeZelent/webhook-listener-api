package com.felipe.webhook_listener_api.exception;

public class InvalidSignatureException extends RuntimeException {

	public InvalidSignatureException(String message) {
		super(message);
	}
}
