package io.miragon.training.domain;

import java.util.UUID;

/**
 * Identity of a membership being provisioned. Pure domain type, no framework dependencies.
 */
public record MembershipId(UUID value) {

    public MembershipId() {
        this(UUID.randomUUID());
    }
}
