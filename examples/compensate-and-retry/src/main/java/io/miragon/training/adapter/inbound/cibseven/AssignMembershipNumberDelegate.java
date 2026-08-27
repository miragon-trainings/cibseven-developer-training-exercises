package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.AssignMembershipNumberUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AssignMembershipNumberDelegate extends BaseDelegate {

    private final AssignMembershipNumberUseCase useCase;

    public AssignMembershipNumberDelegate(AssignMembershipNumberUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        var membershipNumber = useCase.assignMembershipNumber(new MembershipId(UUID.fromString(membershipId)));
        execution.setVariable("membershipNumber", membershipNumber);
    }
}
