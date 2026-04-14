package com.felipe.webhook_listener_api.exception;

import com.felipe.webhook_listener_api.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidPayloadException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidPayload(
		InvalidPayloadException exception,
		HttpServletRequest request
	) {
		return buildResponse(
			HttpStatus.BAD_REQUEST,
			exception.getMessage(),
			request.getRequestURI(),
			exception.getValidationErrors()
		);
	}

	@ExceptionHandler(InvalidSignatureException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidSignature(
		InvalidSignatureException exception,
		HttpServletRequest request
	) {
		return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(DuplicateEventException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateEvent(
		DuplicateEventException exception,
		HttpServletRequest request
	) {
		return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		exception.getConstraintViolations()
			.forEach(violation -> validationErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));

		return buildResponse(HttpStatus.BAD_REQUEST, "Payload validation failed", request.getRequestURI(), validationErrors);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingRequestHeader(
		MissingRequestHeaderException exception,
		HttpServletRequest request
	) {
		return buildResponse(
			HttpStatus.BAD_REQUEST,
			exception.getHeaderName() + " header is required",
			request.getRequestURI(),
			null
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
		Exception exception,
		HttpServletRequest request
	) {
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error", request.getRequestURI(), null);
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(
		HttpStatus status,
		String message,
		String path,
		Map<String, String> validationErrors
	) {
		return ResponseEntity.status(status).body(
			new ApiErrorResponse(
				OffsetDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path,
				validationErrors
			)
		);
	}
}
