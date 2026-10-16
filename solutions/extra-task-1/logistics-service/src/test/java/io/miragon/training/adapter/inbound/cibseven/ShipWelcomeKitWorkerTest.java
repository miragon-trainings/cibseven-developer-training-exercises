package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.ShipWelcomeKitUseCase;
import io.miragon.training.domain.Member;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The worker is a plain class: build it with a mocked use case, call the worker method, verify.
 * No engine, no adapter.
 */
class ShipWelcomeKitWorkerTest {

    private final ShipWelcomeKitUseCase shipWelcomeKit = mock(ShipWelcomeKitUseCase.class);
    private final ShipWelcomeKitWorker worker = new ShipWelcomeKitWorker(shipWelcomeKit);

    @Test
    void shipsTheKitToTheMember() {
        worker.shipWelcomeKit("Jane");

        verify(shipWelcomeKit).shipWelcomeKit(new Member("Jane"));
    }
}
