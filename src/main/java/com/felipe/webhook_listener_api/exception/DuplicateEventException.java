package com.felipe.webhook_listener_api.exception;

public class DuplicateEventException extends RuntimeException {

	public DuplicateEventException(String message) {
		super(message);
	}
}
