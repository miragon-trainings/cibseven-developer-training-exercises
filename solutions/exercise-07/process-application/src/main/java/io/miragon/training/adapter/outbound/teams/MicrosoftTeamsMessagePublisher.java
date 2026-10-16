package io.miragon.training.adapter.outbound.teams;

import io.miragon.training.application.port.outbound.NotificationPublisherOutPort;
import io.miragon.training.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Publishes a {@link Notification} as an Adaptive Card to a Microsoft Teams channel via a Power
 * Automate "Workflows" webhook (template "When a Teams webhook request is received"). No token
 * needed — the webhook URL is the secret (supply it via {@code notification.teams.webhook-url}).
 */
@Component
public class MicrosoftTeamsMessagePublisher implements NotificationPublisherOutPort {

    private static final Logger log = LoggerFactory.getLogger(MicrosoftTeamsMessagePublisher.class);

    private final RestClient restClient;
    private final String webhookUrl;

    public MicrosoftTeamsMessagePublisher(RestClient restClient,
                                          @Value("${notification.teams.webhook-url}") String webhookUrl) {
        this.restClient = restClient;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void publish(Notification notification) {
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(adaptiveCardMessage(notification))
                .retrieve()
                .toBodilessEntity();

        log.info("Published notification to Teams: {}", notification.title());
    }

    private Map<String, Object> adaptiveCardMessage(Notification notification) {
        Map<String, Object> title = Map.of(
                "type", "TextBlock",
                "text", notification.title(),
                "weight", "Bolder",
                "size", "Medium",
                "wrap", true
        );
        Map<String, Object> text = Map.of(
                "type", "TextBlock",
                "text", notification.text(),
                "wrap", true
        );
        Map<String, Object> card = Map.of(
                "$schema", "http://adaptivecards.io/schemas/adaptive-card.json",
                "type", "AdaptiveCard",
                "version", "1.4",
                "body", List.of(title, text)
        );
        Map<String, Object> attachment = Map.of(
                "contentType", "application/vnd.microsoft.card.adaptive",
                "content", card
        );
        return Map.of(
                "type", "message",
                "attachments", List.of(attachment)
        );
    }
}
