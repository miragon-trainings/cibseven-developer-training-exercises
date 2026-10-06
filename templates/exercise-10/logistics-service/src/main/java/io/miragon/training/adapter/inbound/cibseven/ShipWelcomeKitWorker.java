package io.miragon.training.adapter.inbound.cibseven;

import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskService;

/**
 * TODO Exercise 10: Implement the external-task worker for the "Ship welcome kit" task.
 *
 * <p>Only the skeleton is here on purpose – the worker is your job (after you have adapted the process
 * and generated the Process-API):
 * <ol>
 *   <li>Make the class a Spring bean ({@code @Component}) and subscribe to the topic with
 *       {@code @ExternalTaskSubscription(topicName = ServiceTasks.SHIP_WELCOME_KIT)}.
 *       The constant only comes into being once you have marked the service task in the model as an
 *       external task with a topic and regenerated the Process-API.</li>
 *   <li>Read the process variable {@code name} from the {@link ExternalTask}, ship the kit via
 *       {@code ShipWelcomeKitUseCase} (inject it via the constructor) and complete the task via the
 *       {@link ExternalTaskService}. You fill in the concrete arguments yourself:
 *       <pre>
 *   String name = task.getVariable(...);   // process variable "name"
 *   shipWelcomeKit.shipWelcomeKit(...);    // ship the kit via the use case
 *   taskService.complete(...);             // complete the external task
 *       </pre></li>
 * </ol>
 * The shared error handling is already in {@link BaseExternalTaskWorker} – let your worker inherit from it.
 */
public class ShipWelcomeKitWorker extends BaseExternalTaskWorker {

    @Override
    protected void executeTask(ExternalTask task, ExternalTaskService taskService) {
        throw new UnsupportedOperationException("TODO Exercise 10: implement the shipWelcomeKit worker");
    }
}
