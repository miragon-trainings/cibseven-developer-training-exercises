package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.CreateMemberProfileUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plain step: create the member profile. It carries no compensation handler, so it is never rolled
 * back by the retry loop.
 */
@Service
@Transactional
public class CreateMemberProfileService implements CreateMemberProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateMemberProfileService.class);

    @Override
    public void createMemberProfile(MembershipId membershipId) {
        log.info("Create member profile for {} (kept even across retries)", membershipId.value());
    }
}
