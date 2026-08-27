package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.MembershipId;

public interface ProvisioningProcess {

    void start(MembershipId membershipId, String email, String name, int age);
}
