package com.felipe.webhook_listener_api.repository;

import com.felipe.webhook_listener_api.entity.WebhookEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

	boolean existsByExternalEventId(String externalEventId);

	List<WebhookEvent> findAllByOrderByReceivedAtDesc();
}
