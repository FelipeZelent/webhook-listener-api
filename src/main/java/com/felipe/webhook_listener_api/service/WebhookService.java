package com.felipe.webhook_listener_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipe.webhook_listener_api.dto.WebhookEventRequest;
import com.felipe.webhook_listener_api.dto.WebhookEventResponse;
import com.felipe.webhook_listener_api.entity.WebhookEvent;
import com.felipe.webhook_listener_api.entity.WebhookStatus;
import com.felipe.webhook_listener_api.exception.DuplicateEventException;
import com.felipe.webhook_listener_api.exception.InvalidPayloadException;
import com.felipe.webhook_listener_api.repository.WebhookEventRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookService {

	private static final String GITHUB_SOURCE = "github";

	private final SignatureValidationService signatureValidationService;
	private final WebhookEventRepository webhookEventRepository;
	private final ObjectMapper objectMapper;
	private final Validator validator;

	@Transactional
	public WebhookEventResponse processGitHubWebhook(String signature, String rawBody) {
		signatureValidationService.validate(signature, rawBody);

		WebhookEventRequest request = deserialize(rawBody);
		validate(request);

		WebhookEvent event = WebhookEvent.builder()
			.externalEventId(request.id())
			.source(GITHUB_SOURCE)
			.action(request.action())
			.repository(request.repository())
			.eventTimestamp(request.timestamp())
			.receivedAt(OffsetDateTime.now())
			.status(WebhookStatus.RECEIVED)
			.build();

		try {
			return toResponse(webhookEventRepository.saveAndFlush(event));
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateExternalEventIdViolation(exception)) {
				throw new DuplicateEventException("Webhook event with externalEventId " + request.id() + " already exists");
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public List<WebhookEventResponse> listEvents() {
		return webhookEventRepository.findAllByOrderByReceivedAtDesc()
			.stream()
			.map(this::toResponse)
			.toList();
	}

	private WebhookEventRequest deserialize(String rawBody) {
		try {
			return objectMapper.readValue(rawBody, WebhookEventRequest.class);
		} catch (Exception exception) {
			throw new InvalidPayloadException("Request body must contain valid JSON");
		}
	}

	private void validate(WebhookEventRequest request) {
		Map<String, String> validationErrors = new LinkedHashMap<>();

		for (ConstraintViolation<WebhookEventRequest> violation : validator.validate(request)) {
			validationErrors.put(violation.getPropertyPath().toString(), violation.getMessage());
		}

		if (!validationErrors.isEmpty()) {
			throw new InvalidPayloadException("Payload validation failed", validationErrors);
		}
	}

	private WebhookEventResponse toResponse(WebhookEvent event) {
		return new WebhookEventResponse(
			event.getId(),
			event.getExternalEventId(),
			event.getSource(),
			event.getAction(),
			event.getRepository(),
			event.getEventTimestamp(),
			event.getReceivedAt(),
			event.getStatus()
		);
	}

	private boolean isDuplicateExternalEventIdViolation(DataIntegrityViolationException exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof ConstraintViolationException constraintViolationException) {
				String constraintName = constraintViolationException.getConstraintName();
				if ("uk_webhook_events_external_event_id".equalsIgnoreCase(constraintName)) {
					return true;
				}
			}
			if (current instanceof SQLException sqlException) {
				if ("23505".equals(sqlException.getSQLState())) {
					return true;
				}
			}
			String message = current.getMessage();
			if (message != null && message.contains("external_event_id")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
