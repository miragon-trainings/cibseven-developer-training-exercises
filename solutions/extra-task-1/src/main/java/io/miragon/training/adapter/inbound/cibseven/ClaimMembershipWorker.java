package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Engine-neutral replacement for the former {@code ClaimMembershipDelegate}.
 * Bound to the BPMN service task via its external-task topic – no CIB7 imports.
 */
@Component
public class ClaimMembershipWorker {

    private static final Logger log = LoggerFactory.getLogger(ClaimMembershipWorker.class);

    private final ClaimMembershipUseCase useCase;

    public ClaimMembershipWorker(ClaimMembershipUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.CLAIM_MEMBERSHIP)
    public Map<String, Object> claimMembership(@Variable(name = "membershipId") String membershipId) {
        log.debug("Received task to claim membership: {}", membershipId);
        var hasEmptySpots = useCase.claimMembership(new MembershipId(UUID.fromString(membershipId)));
        return Map.of("hasEmptySpots", hasEmptySpots);
    }
}
