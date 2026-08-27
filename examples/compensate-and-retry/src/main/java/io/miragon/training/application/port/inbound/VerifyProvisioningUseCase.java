package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.MembershipId;

public interface VerifyProvisioningUseCase {

    /**
     * Checks whether provisioning succeeded on the given attempt (1-based). The demo implementation
     * fails the first attempt and succeeds from the second, so exactly one rollback + redo happens.
     */
    boolean verify(MembershipId membershipId, int attempt);
}
