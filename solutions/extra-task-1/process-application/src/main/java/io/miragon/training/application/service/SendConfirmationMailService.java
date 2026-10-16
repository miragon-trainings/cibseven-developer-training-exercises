package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SendConfirmationMailService implements SendConfirmationMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendConfirmationMailService.class);

    private final MembershipRepository repository;

    public SendConfirmationMailService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sendConfirmationMail(MembershipId membershipId) {
        var membership = repository.find(membershipId);
        log.info("Sending confirmation mail to {} (membershipId={})", membership.email().value(), membershipId.value());
    }
}
