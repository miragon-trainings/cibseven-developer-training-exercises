package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.CancelWelcomeKitReservationUseCase;
import io.miragon.training.application.port.outbound.WelcomeKitInventory;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compensation handler for {@link ReserveWelcomeKitService}: releases the reservation. Invoked by the
 * engine when compensation is thrown — never on the normal forward path.
 */
@Service
@Transactional
public class CancelWelcomeKitReservationService implements CancelWelcomeKitReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelWelcomeKitReservationService.class);

    private final WelcomeKitInventory inventory;

    public CancelWelcomeKitReservationService(WelcomeKitInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void cancelWelcomeKitReservation(MembershipId membershipId) {
        inventory.cancelReservation(membershipId);
        log.info("Compensation: cancelled welcome-kit reservation for {}", membershipId.value());
    }
}
