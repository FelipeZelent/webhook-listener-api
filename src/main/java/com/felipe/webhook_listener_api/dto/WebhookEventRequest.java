package com.felipe.webhook_listener_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record WebhookEventRequest(
	@NotBlank(message = "id is required")
	@Size(max = 255, message = "id must be at most 255 characters")
	String id,
	@NotBlank(message = "action is required")
	@Size(max = 100, message = "action must be at most 100 characters")
	String action,
	@NotBlank(message = "repository is required")
	@Size(max = 255, message = "repository must be at most 255 characters")
	String repository,
	@NotNull(message = "timestamp is required")
	OffsetDateTime timestamp
) {
}
