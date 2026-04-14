package com.felipe.webhook_listener_api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.felipe.webhook_listener_api.entity.WebhookEvent;
import com.felipe.webhook_listener_api.entity.WebhookStatus;
import com.felipe.webhook_listener_api.repository.WebhookEventRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class WebhookEventRepositoryTest {

	@Autowired
	private WebhookEventRepository webhookEventRepository;

	@Test
	void shouldEnforceUniqueExternalEventId() {
		WebhookEvent first = WebhookEvent.builder()
			.externalEventId("evt-123")
			.source("github")
			.action("opened")
			.repository("felipe/webhook-listener-api")
			.eventTimestamp(OffsetDateTime.parse("2026-04-13T18:00:00Z"))
			.receivedAt(OffsetDateTime.parse("2026-04-13T18:00:01Z"))
			.status(WebhookStatus.RECEIVED)
			.build();

		WebhookEvent duplicate = WebhookEvent.builder()
			.externalEventId("evt-123")
			.source("github")
			.action("closed")
			.repository("felipe/webhook-listener-api")
			.eventTimestamp(OffsetDateTime.parse("2026-04-13T18:00:02Z"))
			.receivedAt(OffsetDateTime.parse("2026-04-13T18:00:03Z"))
			.status(WebhookStatus.RECEIVED)
			.build();

		webhookEventRepository.saveAndFlush(first);

		assertThrows(DataIntegrityViolationException.class, () -> webhookEventRepository.saveAndFlush(duplicate));
	}
}
