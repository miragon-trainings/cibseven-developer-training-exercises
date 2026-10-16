package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.ConfirmMembershipUseCase;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.MembershipId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfirmMembershipService implements ConfirmMembershipUseCase {

    private final MembershipProcess process;

    public ConfirmMembershipService(MembershipProcess process) {
        this.process = process;
    }

    @Override
    public void confirm(MembershipId membershipId) {
        process.confirm(membershipId);
    }
}
