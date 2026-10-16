package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.MembershipId;

public interface RegisterMembershipUseCase {

    MembershipId register(Command command);

    record Command(String email, String name, int age) {}
}
