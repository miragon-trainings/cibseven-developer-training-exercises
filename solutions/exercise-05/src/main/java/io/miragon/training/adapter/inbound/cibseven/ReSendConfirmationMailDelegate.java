package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReSendConfirmationMailDelegate extends BaseDelegate {

    private final ReSendConfirmationMailUseCase useCase;

    public ReSendConfirmationMailDelegate(ReSendConfirmationMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        log.debug("Received task to re-send confirmation mail for membership: {}", membershipId);
        useCase.reSendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
