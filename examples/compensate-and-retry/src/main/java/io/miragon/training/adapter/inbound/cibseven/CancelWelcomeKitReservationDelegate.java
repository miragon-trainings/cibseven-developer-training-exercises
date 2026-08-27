package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.CancelWelcomeKitReservationUseCase;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Compensation handler delegate for the welcome-kit reservation. The engine calls it (via the
 * compensation boundary event + association) only when compensation is thrown.
 */
@Component
public class CancelWelcomeKitReservationDelegate extends BaseDelegate {

    private final CancelWelcomeKitReservationUseCase useCase;

    public CancelWelcomeKitReservationDelegate(CancelWelcomeKitReservationUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var membershipId = (String) execution.getVariable("membershipId");
        useCase.cancelWelcomeKitReservation(new MembershipId(UUID.fromString(membershipId)));
    }
}
