package io.miragon.training.adapter.outbound.engine;

import dev.bpmcrafters.processengineapi.process.ProcessInformation;
import dev.bpmcrafters.processengineapi.process.StartProcessApi;
import dev.bpmcrafters.processengineapi.process.StartProcessByDefinitionCmd;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The adapter drives the engine through the engine-neutral {@link StartProcessApi}. We mock it and
 * assert the command the adapter sends: the process key plus the {@code name} variable.
 */
class RemoteWelcomeKitProcessAdapterTest {

    @Test
    void startsTheProcessByKeyWithTheNameVariable() {
        StartProcessApi startProcessApi = mock(StartProcessApi.class);
        when(startProcessApi.startProcess(any()))
                .thenReturn(CompletableFuture.completedFuture(new ProcessInformation("pi-1", Map.of())));
        RemoteWelcomeKitProcessAdapter adapter = new RemoteWelcomeKitProcessAdapter(startProcessApi);

        adapter.startWelcomeKit("Jane");

        ArgumentCaptor<StartProcessByDefinitionCmd> command = ArgumentCaptor.forClass(StartProcessByDefinitionCmd.class);
        verify(startProcessApi).startProcess(command.capture());
        assertThat(command.getValue().getDefinitionKey()).isEqualTo("sendWelcomeKit");
        assertThat(command.getValue().get()).containsEntry("name", "Jane");
    }
}
