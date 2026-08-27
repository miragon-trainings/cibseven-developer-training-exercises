package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.ReserveWelcomeKitUseCase;
import io.miragon.training.application.port.outbound.WelcomeKitInventory;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compensable step A: reserve a welcome kit. The reservation is what the compensation handler
 * ({@link CancelWelcomeKitReservationService}) undoes.
 */
@Service
@Transactional
public class ReserveWelcomeKitService implements ReserveWelcomeKitUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReserveWelcomeKitService.class);

    private final WelcomeKitInventory inventory;

    public ReserveWelcomeKitService(WelcomeKitInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void reserveWelcomeKit(MembershipId membershipId) {
        inventory.reserve(membershipId);
        log.info("Reserved welcome kit for {}", membershipId.value());
    }
}
