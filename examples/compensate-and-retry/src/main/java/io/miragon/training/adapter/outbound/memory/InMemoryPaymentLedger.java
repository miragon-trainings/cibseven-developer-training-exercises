package io.miragon.training.adapter.outbound.memory;

import io.miragon.training.application.port.outbound.PaymentLedger;
import io.miragon.training.domain.MembershipId;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stand-in for a payment provider, so the charge (and its refund) has a visible effect
 * without any external system.
 */
@Component
public class InMemoryPaymentLedger implements PaymentLedger {

    private final Set<UUID> charged = ConcurrentHashMap.newKeySet();

    @Override
    public void charge(MembershipId membershipId) {
        charged.add(membershipId.value());
    }

    @Override
    public void refund(MembershipId membershipId) {
        charged.remove(membershipId.value());
    }

    @Override
    public boolean isCharged(MembershipId membershipId) {
        return charged.contains(membershipId.value());
    }
}
