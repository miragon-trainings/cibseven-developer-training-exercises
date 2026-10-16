package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReSendConfirmationMailService implements ReSendConfirmationMailUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReSendConfirmationMailService.class);

    private final MembershipRepository repository;

    public ReSendConfirmationMailService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public void reSendConfirmationMail(MembershipId membershipId) {
        var membership = repository.find(membershipId);
        log.info("Re-sending confirmation mail to {} (membershipId={})", membership.email().value(), membershipId.value());
    }
}
