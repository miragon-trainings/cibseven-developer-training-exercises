package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.VerifyProvisioningUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validation step. To make the demo deterministic and loop-safe, it fails the first attempt and
 * succeeds from the second — so the instance shows exactly one rollback + redo and cannot loop
 * forever (the {@code attempt} counter only ever increments).
 */
@Service
@Transactional(readOnly = true)
public class VerifyProvisioningService implements VerifyProvisioningUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyProvisioningService.class);

    @Override
    public boolean verify(MembershipId membershipId, int attempt) {
        boolean ok = attempt >= 2;
        log.info("Verify provisioning for {} on attempt {} -> ok={}", membershipId.value(), attempt, ok);
        return ok;
    }
}
