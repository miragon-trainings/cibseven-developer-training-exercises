package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.ShipWelcomeKitUseCase;
import io.miragon.training.domain.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Worker for the {@code shipWelcomeKit} topic. The remote adapter fetches the external task over REST
 * and hands it to this method; the worker library completes the task on return and reports a failure
 * if the method throws.
 */
@Component
public class ShipWelcomeKitWorker {

    private static final Logger log = LoggerFactory.getLogger(ShipWelcomeKitWorker.class);

    private final ShipWelcomeKitUseCase shipWelcomeKit;

    public ShipWelcomeKitWorker(ShipWelcomeKitUseCase shipWelcomeKit) {
        this.shipWelcomeKit = shipWelcomeKit;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SHIP_WELCOME_KIT)
    public void shipWelcomeKit(@Variable(name = "name") String name) {
        log.info("Received task to ship welcome kit to {}", name);
        shipWelcomeKit.shipWelcomeKit(new Member(name));
    }
}
