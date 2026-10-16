package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.Member;

/** Ships a welcome kit to a newly activated member. */
public interface ShipWelcomeKitUseCase {

    void shipWelcomeKit(Member member);
}
