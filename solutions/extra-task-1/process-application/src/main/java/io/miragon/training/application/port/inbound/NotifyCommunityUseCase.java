package io.miragon.training.application.port.inbound;

public interface NotifyCommunityUseCase {

    /**
     * Announces a freshly activated membership to the community (e.g. a forum / Teams channel).
     */
    void notifyCommunity();
}
