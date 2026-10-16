package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.Member;

/**
 * Outbound port to the department's shipping system. Behind it sits a stand-in adapter here, but it
 * could just as well be a real fulfilment/logistics API.
 */
public interface WelcomeKitShipmentOutPort {

    void ship(Member member);
}
