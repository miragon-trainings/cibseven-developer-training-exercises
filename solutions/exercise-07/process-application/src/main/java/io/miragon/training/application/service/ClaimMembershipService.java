package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class ClaimMembershipService implements ClaimMembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClaimMembershipService.class);

    private static final int MAX_SPOTS = 1000;
    private final AtomicInteger claimedSpots = new AtomicInteger(0);

    private final MembershipRepository repository;

    public ClaimMembershipService(MembershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean claimMembership(MembershipId membershipId) {
        var membership = repository.find(membershipId);
        var claimed = claimedSpots.incrementAndGet();
        var hasSpot = claimed <= MAX_SPOTS;
        log.info("Claiming membership for {} – spot {}/{}, hasSpot={}", membership.email().value(), claimed, MAX_SPOTS, hasSpot);
        return hasSpot;
    }
}
