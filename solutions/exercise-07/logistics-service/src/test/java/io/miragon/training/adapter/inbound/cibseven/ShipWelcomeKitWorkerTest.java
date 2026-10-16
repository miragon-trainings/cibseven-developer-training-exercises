package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.ShipWelcomeKitUseCase;
import io.miragon.training.domain.Member;
import org.cibseven.bpm.client.task.ExternalTask;
import org.cibseven.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The worker is a plain class: build it with a mocked use case, call {@code execute} with mocks, verify.
 * No engine, no broker.
 */
class ShipWelcomeKitWorkerTest {

    private final ShipWelcomeKitUseCase shipWelcomeKit = mock(ShipWelcomeKitUseCase.class);
    private final ExternalTask task = mock(ExternalTask.class);
    private final ExternalTaskService service = mock(ExternalTaskService.class);
    private final ShipWelcomeKitWorker worker = new ShipWelcomeKitWorker(shipWelcomeKit);

    @Test
    void shipsTheKitAndCompletesTheTask() {
        when(task.<String>getVariable("name")).thenReturn("Jane");

        worker.execute(task, service);

        verify(shipWelcomeKit).shipWelcomeKit(new Member("Jane"));
        verify(service).complete(task);
    }

    @Test
    void reportsFailureWhenShippingThrows() {
        when(task.<String>getVariable("name")).thenReturn("Jane");
        doThrow(new RuntimeException("boom")).when(shipWelcomeKit).shipWelcomeKit(any());

        worker.execute(task, service);

        // The base worker turns any exception into a handleFailure. We assert a failure was reported,
        // not an exact retry count (retries stay a worker-tuning concern).
        verify(service).handleFailure(eq(task), eq("boom"), anyString(), eq(0), anyLong());
    }
}
