package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.RejectMembershipUseCase;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RejectMembershipService implements RejectMembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(RejectMembershipService.class);

    private final MembershipProcess process;

    public RejectMembershipService(MembershipProcess process) {
        this.process = process;
    }

    @Override
    public void rejectMembership(MembershipId membershipId) {
        log.info("Rejecting membership confirmation for membershipId={}", membershipId.value());
        process.rejectMembership(membershipId);
    }
}
