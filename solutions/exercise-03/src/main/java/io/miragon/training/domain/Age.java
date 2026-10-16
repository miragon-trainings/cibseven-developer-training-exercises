package io.miragon.training.domain;

public record Age(int value) {

    public Age {
        if (value < 0) {
            throw new IllegalArgumentException("Age must not be negative");
        }
    }
}
