package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.NotifyCommunityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotifyCommunityService implements NotifyCommunityUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifyCommunityService.class);

    @Override
    public void notifyCommunity() {
        log.info("Publishing in the community: a new member just joined the Miravelo Inner Circle! 🎉");
    }
}
