package io.miragon.training.domain;

/**
 * The member a welcome kit is shipped to. Pure domain — it knows nothing about the engine,
 * the signal that triggered the process, or how the kit is shipped.
 */
public record Member(String name) {
}
