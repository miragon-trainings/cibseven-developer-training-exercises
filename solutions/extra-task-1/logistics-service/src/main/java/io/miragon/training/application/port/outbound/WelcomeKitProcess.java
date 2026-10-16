package io.miragon.training.application.port.outbound;

/**
 * Outbound port to the (remote) engine that runs the owned {@code sendWelcomeKit} process. The
 * business logic depends on this interface, never on HTTP — the engine is just another external
 * system behind a port (implemented by {@code RemoteWelcomeKitProcessAdapter} over {@code /engine-rest}).
 */
public interface WelcomeKitProcess {

    void startWelcomeKit(String memberName);
}
