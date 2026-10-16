package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.NotifyCommunityUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotifyCommunityDelegate extends BaseDelegate {

    private final NotifyCommunityUseCase useCase;

    public NotifyCommunityDelegate(NotifyCommunityUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        log.debug("Received task to notify community for membership: {}", membershipId);
        useCase.notifyCommunity(new MembershipId(UUID.fromString(membershipId)));
    }
}
