package io.miragon.training.domain;

import java.util.UUID;

public record MembershipId(UUID value) {

    public MembershipId() {
        this(UUID.randomUUID());
    }
}
