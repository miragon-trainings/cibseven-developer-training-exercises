package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.NotifyCommunityUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.application.port.outbound.NotificationPublisherOutPort;
import io.miragon.training.domain.MembershipId;
import io.miragon.training.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotifyCommunityService implements NotifyCommunityUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifyCommunityService.class);

    private final MembershipRepository repository;
    private final NotificationPublisherOutPort notificationPublisher;

    public NotifyCommunityService(MembershipRepository repository,
                                  NotificationPublisherOutPort notificationPublisher) {
        this.repository = repository;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public void notifyCommunity(MembershipId membershipId) {
        var membership = repository.find(membershipId);
        var name = membership.name().value();
        log.info("Notifying community about new Inner Circle member {} (membershipId={})", name, membershipId.value());
        notificationPublisher.publish(new Notification(
                "Miravelo Inner Circle",
                "🎉 New Inner Circle member: " + name + "!"));
    }
}
