package io.miragon.training.adapter.outbound.shipping;

import io.miragon.training.application.port.outbound.WelcomeKitShipmentOutPort;
import io.miragon.training.domain.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A stand-in for the Logistics department's shipping system: it simply logs the shipment. Because the
 * output sits behind {@link WelcomeKitShipmentOutPort}, swapping this for a real fulfilment/HTTP API
 * later touches nothing but this adapter.
 */
@Component
public class LoggingWelcomeKitShipmentAdapter implements WelcomeKitShipmentOutPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingWelcomeKitShipmentAdapter.class);

    @Override
    public void ship(Member member) {
        log.info("📦 Welcome kit shipped to new Inner Circle member: {}", member.name());
    }
}
