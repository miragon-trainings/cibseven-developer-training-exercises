package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.ShipWelcomeKitUseCase;
import io.miragon.training.domain.Member;
import org.cibseven.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

/**
 * Remote external-task worker for the {@code shipWelcomeKit} topic (Direction 1: the engine hands the
 * task out, the worker pulls it). It reads the {@code name} the signal carried, ships the kit through
 * the use case, and completes the task so the process can end.
 */
@Component
@ExternalTaskSubscription(topicName = ServiceTasks.SHIP_WELCOME_KIT)
public class ShipWelcomeKitWorker extends BaseExternalTaskWorker {

    private final ShipWelcomeKitUseCase shipWelcomeKit;

    public ShipWelcomeKitWorker(ShipWelcomeKitUseCase shipWelcomeKit) {
        this.shipWelcomeKit = shipWelcomeKit;
    }

    @Override
    protected void executeTask(ExternalTask task, ExternalTaskService taskService) {
        String name = task.getVariable("name");
        log.info("Locked external task {} — shipping welcome kit to {}", task.getId(), name);

        shipWelcomeKit.shipWelcomeKit(new Member(name));

        taskService.complete(task);
    }
}
