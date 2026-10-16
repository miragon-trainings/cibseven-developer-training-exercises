package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.Notification;

/**
 * Outbound port: publishes a {@link Notification} to an external channel (e.g. a Microsoft Teams
 * channel) so the whole team is notified.
 */
public interface NotificationPublisherOutPort {

    void publish(Notification notification);
}
