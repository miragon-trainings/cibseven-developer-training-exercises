package io.miragon.training.application.port.inbound;

/**
 * Re-sends a welcome kit on demand (e.g. the first one got lost in shipping) by starting a fresh
 * {@code sendWelcomeKit} process instance — without waiting for a new membership signal.
 */
public interface ResendWelcomeKitUseCase {

    void resendWelcomeKit(String memberName);
}
