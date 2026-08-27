package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.MembershipId;

public interface StartProvisioningUseCase {

    MembershipId start(Command command);

    record Command(String email, String name, int age) {
    }
}
