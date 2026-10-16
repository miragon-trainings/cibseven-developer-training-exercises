package io.miragon.training.process.util;

import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.externaltask.LockedExternalTask;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.bpm.engine.runtime.ProcessInstance;

import java.util.List;
import java.util.Map;

/**
 * Small helpers that outsource the communication with the engine in a process test — broadcasting the
 * signal, completing external tasks (standing in for the remote worker) and finding the instance. Keeping
 * this out of the test body leaves the test itself about the process <em>behaviour</em>. Mirrors the
 * reference blueprint's process-test helpers (there Kotlin extension functions on {@code ProcessEngine}).
 */
public final class ProcessEngineTestUtils {

    private static final String TEST_WORKER = "test-worker";

    private ProcessEngineTestUtils() {
    }

    /** Broadcasts a signal with variables — as the engine host does when a member is activated. */
    public static void broadcastSignal(ProcessEngine engine, String signalName, Map<String, Object> variables) {
        engine.getRuntimeService()
                .createSignalEvent(signalName)
                .setVariables(variables)
                .send();
    }

    /** Starts a process by key with variables — as the manual/testing start does via the engine's REST API. */
    public static void startProcessByKey(ProcessEngine engine, String processDefinitionKey, Map<String, Object> variables) {
        engine.getRuntimeService().startProcessInstanceByKey(processDefinitionKey, variables);
    }

    /**
     * Fetches, locks and completes the next external task of the given topic — standing in for the remote
     * worker, because an external task is a wait state the in-memory engine parks until it is completed.
     */
    public static void completeExternalTask(ProcessEngine engine, String topic) {
        List<LockedExternalTask> tasks = engine.getExternalTaskService()
                .fetchAndLock(1, TEST_WORKER)
                .topic(topic, 10_000L)
                .execute();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("No external task found for topic '" + topic + "'");
        }
        engine.getExternalTaskService().complete(tasks.get(0).getId(), TEST_WORKER);
    }

    /**
     * Executes the pending async-continuation ("message") jobs one after another until the process reaches
     * its next wait state. Needed because the signal start event is {@code camunda:asyncBefore}: the instance
     * is created and committed at once, and the token only moves to the external task when this async job
     * runs. The job executor stays off in the test, so we drive that continuation from the test thread.
     */
    public static void continueToNextWaitState(ProcessEngine engine) {
        ManagementService managementService = engine.getManagementService();
        for (int i = 0; i < 50; i++) {
            Job job = managementService.createJobQuery().active().messages()
                    .listPage(0, 1).stream().findFirst().orElse(null);
            if (job == null) {
                return;
            }
            managementService.executeJob(job.getId());
        }
    }

    /** Finds the single running instance of the given process definition. */
    public static ProcessInstance findInstance(ProcessEngine engine, String processDefinitionKey) {
        return engine.getRuntimeService()
                .createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .singleResult();
    }
}
