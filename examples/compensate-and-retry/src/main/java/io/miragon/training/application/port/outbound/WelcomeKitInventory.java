package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.MembershipId;

/**
 * Side-effect the "Reserve welcome kit" step commits, and its compensation undoes. Backed by an
 * in-memory adapter so the rollback has a tangible, observable effect.
 */
public interface WelcomeKitInventory {

    void reserve(MembershipId membershipId);

    void cancelReservation(MembershipId membershipId);

    boolean isReserved(MembershipId membershipId);
}
