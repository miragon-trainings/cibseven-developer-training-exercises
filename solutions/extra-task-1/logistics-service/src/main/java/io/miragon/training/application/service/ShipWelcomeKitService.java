package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.ShipWelcomeKitUseCase;
import io.miragon.training.application.port.outbound.WelcomeKitShipmentOutPort;
import io.miragon.training.domain.Member;
import org.springframework.stereotype.Service;

@Service
public class ShipWelcomeKitService implements ShipWelcomeKitUseCase {

    private final WelcomeKitShipmentOutPort shipment;

    public ShipWelcomeKitService(WelcomeKitShipmentOutPort shipment) {
        this.shipment = shipment;
    }

    @Override
    public void shipWelcomeKit(Member member) {
        shipment.ship(member);
    }
}
