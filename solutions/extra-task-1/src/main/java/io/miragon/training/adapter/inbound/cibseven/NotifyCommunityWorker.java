package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.NotifyCommunityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles the "Notify community" task that runs on a parallel branch after the membership is
 * confirmed – in parallel with the welcome mail.
 */
@Component
public class NotifyCommunityWorker {

    private static final Logger log = LoggerFactory.getLogger(NotifyCommunityWorker.class);

    private final NotifyCommunityUseCase useCase;

    public NotifyCommunityWorker(NotifyCommunityUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.NOTIFY_COMMUNITY)
    public void notifyCommunity() {
        log.debug("Received task to notify the community");
        useCase.notifyCommunity();
    }
}
