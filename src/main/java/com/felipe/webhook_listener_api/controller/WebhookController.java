package com.felipe.webhook_listener_api.controller;

import com.felipe.webhook_listener_api.dto.WebhookEventResponse;
import com.felipe.webhook_listener_api.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook ingestion and event query endpoints")
public class WebhookController {

	private final WebhookService webhookService;

	@PostMapping(path = "/github", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Receive GitHub webhook events")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Webhook stored successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid header or payload", content = @Content(schema = @Schema(hidden = true))),
		@ApiResponse(responseCode = "401", description = "Invalid signature", content = @Content(schema = @Schema(hidden = true))),
		@ApiResponse(responseCode = "409", description = "Duplicated event", content = @Content(schema = @Schema(hidden = true)))
	})
	public ResponseEntity<WebhookEventResponse> receiveGitHubWebhook(
		@RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
		@RequestBody String rawBody
	) {
		WebhookEventResponse response = webhookService.processGitHubWebhook(signature, rawBody);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping(path = "/events", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "List received webhook events")
	@ApiResponse(responseCode = "200", description = "Events returned successfully")
	public ResponseEntity<List<WebhookEventResponse>> listEvents() {
		return ResponseEntity.ok(webhookService.listEvents());
	}
}
