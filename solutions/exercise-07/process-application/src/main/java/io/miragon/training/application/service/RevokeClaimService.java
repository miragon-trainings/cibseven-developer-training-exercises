package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.RevokeClaimUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RevokeClaimService implements RevokeClaimUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevokeClaimService.class);

    private final MembershipRepository repository;

    public RevokeClaimService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public void revokeClaim(MembershipId membershipId) {
        var membership = repository.find(membershipId);
        log.info("Revoking membership claim for {} (membershipId={})", membership.email().value(), membershipId.value());
    }
}
