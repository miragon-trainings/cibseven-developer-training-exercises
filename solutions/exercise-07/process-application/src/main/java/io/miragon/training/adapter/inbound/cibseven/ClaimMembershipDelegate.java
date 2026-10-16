package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClaimMembershipDelegate extends BaseDelegate {

    private final ClaimMembershipUseCase useCase;

    public ClaimMembershipDelegate(ClaimMembershipUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        log.debug("Received task to claim membership: {}", membershipId);
        var hasEmptySpots = useCase.claimMembership(new MembershipId(UUID.fromString(membershipId)));
        execution.setVariable("hasEmptySpots", hasEmptySpots);
    }
}
