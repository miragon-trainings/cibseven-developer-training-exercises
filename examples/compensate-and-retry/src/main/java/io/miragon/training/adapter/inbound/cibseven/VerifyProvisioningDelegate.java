package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.VerifyProvisioningUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads and increments the persisted {@code attempt} counter, asks the use case whether provisioning
 * is OK, and writes {@code provisioningOk} for the gateway. Because {@code attempt} only ever
 * increments, the retry loop runs at most once.
 */
@Component
public class VerifyProvisioningDelegate extends BaseDelegate {

    private final VerifyProvisioningUseCase useCase;

    public VerifyProvisioningDelegate(VerifyProvisioningUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        int attempt = (int) Optional.ofNullable(execution.getVariable("attempt")).orElse(0) + 1;
        boolean ok = useCase.verify(new MembershipId(UUID.fromString(membershipId)), attempt);
        execution.setVariable("attempt", attempt);
        execution.setVariable("provisioningOk", ok);
    }
}
