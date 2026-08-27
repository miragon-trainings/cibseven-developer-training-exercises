package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.MembershipId;

/**
 * Side-effect the "Charge membership fee" step commits, and its compensation refunds. Backed by an
 * in-memory adapter so the rollback has a tangible, observable effect.
 */
public interface PaymentLedger {

    void charge(MembershipId membershipId);

    void refund(MembershipId membershipId);

    boolean isCharged(MembershipId membershipId);
}
