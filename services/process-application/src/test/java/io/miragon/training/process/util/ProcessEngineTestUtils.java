package io.miragon.training.process.util;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.bpm.engine.runtime.ProcessInstance;

/**
 * Small helpers to drive a process instance deterministically in a unit test.
 *
 * <p>The job executor is off in the {@code test} profile (see {@code application-test.yaml}).
 * As a result the process waits at every {@code camunda:asyncBefore/After}: no background thread
 * picks up the job. The test instead pushes the process forward from the test thread with
 * {@code execute(job(...))}, one job per call like one run of the job executor. These helpers
 * cover the timer and the instance lookup.
 *
 * <p>This class is <b>provided</b> in Exercise 4 – you use it to write your tests, you don't have
 * to build it yourself. See {@code docs/en/exercise-04.md}.
 */
public final class ProcessEngineTestUtils {

    private static final String PROCESS_DEFINITION_KEY = "membership";

    private ProcessEngineTestUtils() {
    }

    /**
     * Fires the timer job of the given boundary/catch event directly, independent of its due date.
     * Checks the timer wiring without waiting for real time (from Exercise 5).
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
