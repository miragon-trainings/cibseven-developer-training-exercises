package io.miragon.training.process.util;

import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi;
import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.bpm.engine.runtime.ProcessInstance;

/**
 * Small helpers for driving a process instance deterministically in a unit test.
 *
 * <p>The job executor is disabled in the {@code test} profile (see {@code application-test.yaml}).
 * That makes the process wait at every {@code camunda:asyncBefore/After}: no background thread ever
 * picks the job up. These helpers push the process forward from the test thread instead, so the
 * timing is fully under our control and the test stays fast and reproducible.
 */
public final class ProcessEngineTestUtils {

    private static final String PROCESS_DEFINITION_KEY = SubscribeNewsletterProcessApi.PROCESS_ID.getValue();

    private ProcessEngineTestUtils() {
    }

    /**
     * Executes all pending async-continuation ("message") jobs one after another until the process
     * reaches its next wait state (user/receive task, timer, or end).
     */
    public static void continueToNextWaitState(ProcessEngine processEngine) {
        continueToNextWaitState(processEngine, null);
    }

    /**
     * Like {@link #continueToNextWaitState(ProcessEngine)}, but only drives the jobs of a single
     * process instance. Useful when an activity broadcasts a signal that starts a second,
     * independent instance which should <em>not</em> be driven by this call.
     */
    public static void continueToNextWaitState(ProcessEngine processEngine, String processInstanceId) {
        ManagementService managementService = processEngine.getManagementService();
        for (int i = 0; i < 50; i++) {
            var query = managementService.createJobQuery().active().messages();
            if (processInstanceId != null) {
                query = query.processInstanceId(processInstanceId);
            }
            Job job = query.listPage(0, 1).stream().findFirst().orElse(null);
            if (job == null) {
                return;
            }
            managementService.executeJob(job.getId());
        }
    }

    /**
     * Fires the timer job of the given boundary/catch event directly, regardless of its due date.
     * Verifies the timer path is wired correctly without waiting real time.
     */
    public static void fireTimer(ProcessEngine processEngine, String timerActivityId) {
        Job timer = processEngine.getManagementService().createJobQuery()
                .timers()
                .activityId(timerActivityId)
                .singleResult();
        if (timer == null) {
            throw new IllegalStateException("No timer job found for activity '" + timerActivityId + "'");
        }
        processEngine.getManagementService().executeJob(timer.getId());
    }

    /**
     * Finds the running membership process instance that carries the given membership id.
     * Fails the test if no such instance exists.
     */
    public static ProcessInstance findProcessInstance(RuntimeService runtimeService, String membershipId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .variableValueEquals("membershipId", membershipId)
                .singleResult();
        if (instance == null) {
            throw new AssertionError("No process instance found for membershipId " + membershipId);
        }
        return instance;
    }
}
