package io.miragon.training.process.util;

import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.bpm.engine.runtime.ProcessInstance;

/**
 * Small helpers to drive a process instance deterministically in a unit test.
 *
 * <p>The job executor is off in the {@code test} profile (see {@code application-test.yaml}).
 * As a result the process waits at every {@code camunda:asyncBefore/After}: no background thread
 * picks up the job. These helpers instead push the process forward from the test thread –
 * the timing stays fully under control and the test fast and reproducible.
 *
 * <p>This class is <b>provided</b> in Exercise 6 – you use it to write your tests, you don't have
 * to build it yourself. See {@code docs/en/exercise-06.md}.
 */
public final class ProcessEngineTestUtils {

    private static final String PROCESS_DEFINITION_KEY = "subscribeNewsletter";

    private ProcessEngineTestUtils() {
    }

    /**
     * Executes the open async-continuation ("message") jobs one after another until the instance
     * reaches its next wait state (User/Receive Task, timer, or end).
     */
    public static void continueToNextWaitState(ProcessEngine processEngine) {
        continueToNextWaitState(processEngine, null);
    }

    /**
     * Like {@link #continueToNextWaitState(ProcessEngine)}, but drives only the jobs of a single
     * instance. Useful once an activity throws a signal that starts a second, independent instance
     * which should <em>not</em> be driven along by this call (from Exercise 8).
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
     * Fires the timer job of the given boundary/catch event directly, independent of its due date.
     * Checks the timer wiring without waiting for real time (from Exercise 7).
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
     * Finds the running membership process instance with the given membershipId.
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
