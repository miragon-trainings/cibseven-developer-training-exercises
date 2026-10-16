package io.miragon.training.domain;

public record Membership(MembershipId id, Email email, Name name, Age age) {

    public Membership(Email email, Name name, Age age) {
        this(new MembershipId(), email, name, age);
    }
}
