package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.ChargeMembershipFeeUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChargeMembershipFeeDelegate extends BaseDelegate {

    private final ChargeMembershipFeeUseCase useCase;

    public ChargeMembershipFeeDelegate(ChargeMembershipFeeUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        useCase.chargeMembershipFee(new MembershipId(UUID.fromString(membershipId)));
    }
}
