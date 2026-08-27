package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.CreateMemberProfileUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateMemberProfileDelegate extends BaseDelegate {

    private final CreateMemberProfileUseCase useCase;

    public CreateMemberProfileDelegate(CreateMemberProfileUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        useCase.createMemberProfile(new MembershipId(UUID.fromString(membershipId)));
    }
}
