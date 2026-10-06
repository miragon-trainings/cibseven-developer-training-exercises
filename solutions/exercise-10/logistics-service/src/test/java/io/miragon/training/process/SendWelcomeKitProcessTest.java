package io.miragon.training.process;

import io.miragon.training.adapter.process.SendWelcomeKitProcessApi;
import io.miragon.training.adapter.process.SendWelcomeKitProcessApi.FlowNodes;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.adapter.process.Signals;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.miragon.training.process.util.ProcessEngineTestUtils.broadcastSignal;
import static io.miragon.training.process.util.ProcessEngineTestUtils.completeExternalTask;
import static io.miragon.training.process.util.ProcessEngineTestUtils.continueToNextWaitState;
import static io.miragon.training.process.util.ProcessEngineTestUtils.findInstance;
import static io.miragon.training.process.util.ProcessEngineTestUtils.startProcessByKey;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;

/**
 * Process-model behaviour test for the {@code sendWelcomeKit} model — it lives here because this service
 * OWNS the model. It spins up a standalone in-memory engine, deploys the model from this service's own
 * resources, and asserts the topology with CIB seven Assert. Engine communication (broadcasting the
 * signal, completing the external task) is outsourced to {@code ProcessEngineTestUtils}.
 *
 * <p>The process is minimal — signal start → one external service task → end — so a single test covers it
 * end to end: the broadcast starts it, the worker (stood in for) completes the task, and all three
 * elements are passed in order.
 */
class SendWelcomeKitProcessTest {

    private ProcessEngine processEngine;

    @BeforeEach
    void setUp() {
        // Job executor stays off by default, so the external-task wait state is driven by hand.
        processEngine = new StandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:sendWelcomeKitTest-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=1000")
                .buildProcessEngine();
        // The process owner deploys its own model — here from this service's own resources on the classpath.
        processEngine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("bpmn/send-welcome-kit.bpmn")
                .deploy();
        init(processEngine);
    }

    @AfterEach
    void tearDown() {
        processEngine.close();
    }

    @Test
    void theSignalStartsTheProcessAndTheWorkerShipsTheKitToTheEnd() {
        broadcastSignal(processEngine, Signals.MEMBER_ACTIVATED.getValue(), Map.of("name", "Jane"));
        ProcessInstance instance = findInstance(processEngine, SendWelcomeKitProcessApi.PROCESS_ID.getValue());

        // The signal start is asyncBefore: the instance commits at once and only reaches the external task
        // once its async continuation runs. With the job executor off, we drive that from the test thread.
        continueToNextWaitState(processEngine);
        completeExternalTask(processEngine, ServiceTasks.SHIP_WELCOME_KIT); // stands in for the remote worker

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(
                        FlowNodes.StartEventMemberActivated.ELEMENT_ID,
                        FlowNodes.ServiceTaskShipWelcomeKit.ELEMENT_ID,
                        FlowNodes.EndEventWelcomeKitShipped.ELEMENT_ID);
    }

    @Test
    void theManualStartAlsoRunsTheProcess() {
        // The second (none) start event lets the service drive a run itself, over the engine's REST API
        // (RemoteWelcomeKitProcessAdapter) — for a manual re-send or a test, independent of the signal.
        startProcessByKey(processEngine, SendWelcomeKitProcessApi.PROCESS_ID.getValue(), Map.of("name", "Jane"));
        ProcessInstance instance = findInstance(processEngine, SendWelcomeKitProcessApi.PROCESS_ID.getValue());

        completeExternalTask(processEngine, ServiceTasks.SHIP_WELCOME_KIT);

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(
                        FlowNodes.StartEventManualStart.ELEMENT_ID,
                        FlowNodes.ServiceTaskShipWelcomeKit.ELEMENT_ID,
                        FlowNodes.EndEventWelcomeKitShipped.ELEMENT_ID);
    }
}
