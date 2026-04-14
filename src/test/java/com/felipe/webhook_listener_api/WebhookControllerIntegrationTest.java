package com.felipe.webhook_listener_api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.felipe.webhook_listener_api.entity.WebhookEvent;
import com.felipe.webhook_listener_api.entity.WebhookStatus;
import com.felipe.webhook_listener_api.repository.WebhookEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "webhook.github.secret=test-secret")
class WebhookControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebhookEventRepository webhookEventRepository;

	@BeforeEach
	void setUp() {
		webhookEventRepository.deleteAll();
	}

	@Test
	void shouldStoreWebhookWhenPayloadAndSignatureAreValid() throws Exception {
		String payload = """
			{
			  "id": "evt-123",
			  "action": "opened",
			  "repository": "felipe/webhook-listener-api",
			  "timestamp": "2026-04-13T18:00:00Z"
			}
			""";

		mockMvc.perform(
				post("/webhooks/github")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Signature", signatureFor(payload))
					.content(payload)
			)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.externalEventId", is("evt-123")))
			.andExpect(jsonPath("$.source", is("github")))
			.andExpect(jsonPath("$.status", is("RECEIVED")));
	}

	@Test
	void shouldRejectRequestWithoutSignatureHeader() throws Exception {
		String payload = """
			{
			  "id": "evt-123",
			  "action": "opened",
			  "repository": "felipe/webhook-listener-api",
			  "timestamp": "2026-04-13T18:00:00Z"
			}
			""";

		mockMvc.perform(
				post("/webhooks/github")
					.contentType(MediaType.APPLICATION_JSON)
					.content(payload)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message", is("X-Signature header is required")));
	}

	@Test
	void shouldRejectRequestWithInvalidSignature() throws Exception {
		String payload = """
			{
			  "id": "evt-123",
			  "action": "opened",
			  "repository": "felipe/webhook-listener-api",
			  "timestamp": "2026-04-13T18:00:00Z"
			}
			""";

		mockMvc.perform(
				post("/webhooks/github")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Signature", "sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
					.content(payload)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message", is("Invalid webhook signature")));
	}

	@Test
	void shouldRejectInvalidPayload() throws Exception {
		String payload = """
			{
			  "id": "evt-123",
			  "repository": "felipe/webhook-listener-api",
			  "timestamp": "2026-04-13T18:00:00Z"
			}
			""";

		mockMvc.perform(
				post("/webhooks/github")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Signature", signatureFor(payload))
					.content(payload)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message", is("Payload validation failed")))
			.andExpect(jsonPath("$.validationErrors.action", is("action is required")));
	}

	@Test
	void shouldRejectDuplicatedEvent() throws Exception {
		String payload = """
			{
			  "id": "evt-123",
			  "action": "opened",
			  "repository": "felipe/webhook-listener-api",
			  "timestamp": "2026-04-13T18:00:00Z"
			}
			""";

		mockMvc.perform(
				post("/webhooks/github")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Signature", signatureFor(payload))
					.content(payload)
			)
			.andExpect(status().isCreated());

		mockMvc.perform(
				post("/webhooks/github")
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Signature", signatureFor(payload))
					.content(payload)
			)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", is("Webhook event with externalEventId evt-123 already exists")));
	}

	@Test
	void shouldListEventsOrderedByReceivedAtDescending() throws Exception {
		webhookEventRepository.save(
			WebhookEvent.builder()
				.externalEventId("evt-older")
				.source("github")
				.action("opened")
				.repository("felipe/old")
				.eventTimestamp(OffsetDateTime.parse("2026-04-13T18:00:00Z"))
				.receivedAt(OffsetDateTime.parse("2026-04-13T18:00:01Z"))
				.status(WebhookStatus.RECEIVED)
				.build()
		);
		webhookEventRepository.save(
			WebhookEvent.builder()
				.externalEventId("evt-newer")
				.source("github")
				.action("closed")
				.repository("felipe/new")
				.eventTimestamp(OffsetDateTime.parse("2026-04-13T18:00:02Z"))
				.receivedAt(OffsetDateTime.parse("2026-04-13T18:00:03Z"))
				.status(WebhookStatus.RECEIVED)
				.build()
		);

		mockMvc.perform(get("/webhooks/events"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].externalEventId", is("evt-newer")))
			.andExpect(jsonPath("$[1].externalEventId", is("evt-older")));
	}

	private String signatureFor(String payload) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		return "sha256=" + HexFormat.of().formatHex(signature);
	}
}
