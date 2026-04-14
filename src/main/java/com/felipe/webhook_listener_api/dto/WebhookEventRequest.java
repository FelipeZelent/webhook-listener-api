package com.felipe.webhook_listener_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record WebhookEventRequest(
	@NotBlank(message = "id is required")
	String id,
	@NotBlank(message = "action is required")
	String action,
	@NotBlank(message = "repository is required")
	String repository,
	@NotNull(message = "timestamp is required")
	OffsetDateTime timestamp
) {
}
