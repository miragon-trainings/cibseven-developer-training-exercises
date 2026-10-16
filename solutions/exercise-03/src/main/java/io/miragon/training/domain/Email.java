package io.miragon.training.domain;

public record Email(String value) {

    public Email {
        if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}
