package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.RevokeClaimUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Compensation handler for the claim. Still a plain worker – the compensation
 * itself stays modelled in the BPMN (compensation boundary + compensating end event).
 */
@Component
public class RevokeClaimWorker {

    private static final Logger log = LoggerFactory.getLogger(RevokeClaimWorker.class);

    private final RevokeClaimUseCase useCase;

    public RevokeClaimWorker(RevokeClaimUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.REVOKE_CLAIM)
    public void revokeClaim(@Variable(name = "membershipId") String membershipId) {
        log.debug("Received task to revoke membership claim: {}", membershipId);
        useCase.revokeClaim(new MembershipId(UUID.fromString(membershipId)));
    }
}
