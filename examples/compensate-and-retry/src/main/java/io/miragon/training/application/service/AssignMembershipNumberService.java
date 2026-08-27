package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.AssignMembershipNumberUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plain step: assign a membership number. Like {@link CreateMemberProfileService} it is never rolled
 * back — the number stays stable across retries.
 */
@Service
@Transactional
public class AssignMembershipNumberService implements AssignMembershipNumberUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssignMembershipNumberService.class);

    @Override
    public String assignMembershipNumber(MembershipId membershipId) {
        var membershipNumber = "IC-" + membershipId.value().toString().substring(0, 8).toUpperCase();
        log.info("Assign membership number {} to {} (kept even across retries)", membershipNumber, membershipId.value());
        return membershipNumber;
    }
}
