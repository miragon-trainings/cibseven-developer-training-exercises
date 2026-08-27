package io.miragon.training.adapter.outbound.memory;

import io.miragon.training.application.port.outbound.WelcomeKitInventory;
import io.miragon.training.domain.MembershipId;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stand-in for a welcome-kit warehouse, so the reservation (and its compensation) has a
 * visible effect without any external system.
 */
@Component
public class InMemoryWelcomeKitInventory implements WelcomeKitInventory {

    private final Set<UUID> reserved = ConcurrentHashMap.newKeySet();

    @Override
    public void reserve(MembershipId membershipId) {
        reserved.add(membershipId.value());
    }

    @Override
    public void cancelReservation(MembershipId membershipId) {
        reserved.remove(membershipId.value());
    }

    @Override
    public boolean isReserved(MembershipId membershipId) {
        return reserved.contains(membershipId.value());
    }
}
