package io.miragon.training.process.util;

import io.miragon.training.adapter.process.MembershipProcessApi;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.bpm.engine.runtime.ProcessInstance;

/**
 * Small helpers for driving a process instance deterministically in a unit test.
 *
 * <p>The job executor is disabled in the {@code test} profile (see {@code application-test.yaml}).
 * That makes the process wait at every {@code camunda:asyncBefore/After}: no background thread ever
 * picks the job up. The test pushes the process forward from the test thread with
 * {@code execute(job(...))}, one job per call like one run of the job executor, so the timing is
 * fully under our control. These helpers cover the timer and the instance lookup.
 */
public final class ProcessEngineTestUtils {

    private static final String PROCESS_DEFINITION_KEY = MembershipProcessApi.PROCESS_ID.getValue();

    private ProcessEngineTestUtils() {
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
