package io.miragon.training.domain;

/**
 * The content we announce when a new member joins — decoupled from where it came from (a
 * "new member joined" event) and where it goes (a Microsoft Teams channel, ...).
 */
public record Notification(String title, String text) {
}
