package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SendWelcomeMailService implements SendWelcomeMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendWelcomeMailService.class);

    private final MembershipRepository repository;

    public SendWelcomeMailService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sendWelcomeMail(MembershipId membershipId) {
        var membership = repository.find(membershipId);
        log.info("Sending welcome mail to {} (membershipId={})", membership.email().value(), membershipId.value());
    }
}
