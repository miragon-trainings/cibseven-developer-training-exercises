package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.SendRejectionMailUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendRejectionMailDelegate extends BaseDelegate {

    private final SendRejectionMailUseCase useCase;

    public SendRejectionMailDelegate(SendRejectionMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        log.debug("Received task to send rejection mail for membership: {}", membershipId);
        useCase.sendRejectionMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
