package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.RevokeClaimUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RevokeClaimDelegate extends BaseDelegate {

    private final RevokeClaimUseCase useCase;

    public RevokeClaimDelegate(RevokeClaimUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        log.debug("Received task to revoke membership claim: {}", membershipId);
        useCase.revokeClaim(new MembershipId(UUID.fromString(membershipId)));
    }
}
