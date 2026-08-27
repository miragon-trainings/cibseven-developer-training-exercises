package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.RefundMembershipFeeUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Compensation handler delegate for the membership fee. Called by the engine when compensation is
 * thrown; runs before the welcome-kit reservation is cancelled (reverse completion order).
 */
@Component
public class RefundMembershipFeeDelegate extends BaseDelegate {

    private final RefundMembershipFeeUseCase useCase;

    public RefundMembershipFeeDelegate(RefundMembershipFeeUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        useCase.refundMembershipFee(new MembershipId(UUID.fromString(membershipId)));
    }
}
