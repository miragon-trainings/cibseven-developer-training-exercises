package io.miragon.training.domain;

public record Name(String value) {

    public Name {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
    }
}
