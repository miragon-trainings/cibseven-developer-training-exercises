package io.miragon.training.adapter.inbound.cibseven;

import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskHandler;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Base for all remote external-task workers — the remote counterpart to the embedded process
 * application's {@code BaseDelegate}. It runs the actual work and, on an unexpected exception, reports
 * the task as a {@code handleFailure} back to the remote engine (no retries here — tune per worker).
 *
 * <p>The concrete worker calls {@code externalTaskService.complete(...)} on success (so it can pass
 * output variables), and may call {@code externalTaskService.handleBpmnError(...)} to raise a modelled
 * BPMN error caught by a boundary event.
 */
public abstract class BaseExternalTaskWorker implements ExternalTaskHandler {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            executeTask(externalTask, externalTaskService);
        } catch (Exception e) {
            log.error("Error while processing external task '{}'", externalTask.getTopicName(), e);
            externalTaskService.handleFailure(
                    externalTask,
                    e.getMessage() != null ? e.getMessage() : "Error while processing external task",
                    stackTraceOf(e),
                    0,
                    0L);
        }
    }

    protected abstract void executeTask(ExternalTask externalTask, ExternalTaskService externalTaskService);

    private static String stackTraceOf(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
