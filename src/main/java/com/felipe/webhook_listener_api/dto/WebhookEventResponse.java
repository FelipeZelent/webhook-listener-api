package com.felipe.webhook_listener_api.dto;

import com.felipe.webhook_listener_api.entity.WebhookStatus;
import java.time.OffsetDateTime;

public record WebhookEventResponse(
	Long id,
	String externalEventId,
	String source,
	String action,
	String repository,
	OffsetDateTime eventTimestamp,
	OffsetDateTime receivedAt,
	WebhookStatus status
) {
}
