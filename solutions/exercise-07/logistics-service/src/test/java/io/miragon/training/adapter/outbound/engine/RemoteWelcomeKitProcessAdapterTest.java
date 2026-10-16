package io.miragon.training.adapter.outbound.engine;

import org.cibseven.community.rest.client.api.ProcessDefinitionApi;
import org.cibseven.community.rest.client.dto.StartProcessInstanceDto;
import org.cibseven.community.rest.client.dto.VariableValueDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The adapter drives the engine through the typed client. We mock the {@link ProcessDefinitionApi} and
 * assert what the adapter hands it: the process key plus the typed variable payload ({@code {value, type}}).
 * No live engine needed.
 */
class RemoteWelcomeKitProcessAdapterTest {

    @Test
    void startsTheProcessByKeyWithTheTypedNameVariable() throws Exception {
        ProcessDefinitionApi processDefinitionApi = mock(ProcessDefinitionApi.class);
        RemoteWelcomeKitProcessAdapter adapter = new RemoteWelcomeKitProcessAdapter(processDefinitionApi);

        adapter.startWelcomeKit("Jane");

        ArgumentCaptor<StartProcessInstanceDto> request = ArgumentCaptor.forClass(StartProcessInstanceDto.class);
        verify(processDefinitionApi).startProcessInstanceByKey(eq("sendWelcomeKit"), request.capture());
        VariableValueDto name = request.getValue().getVariables().get("name");
        assertThat(name.getValue()).isEqualTo("Jane");
        assertThat(name.getType()).isEqualTo("String");
    }
}
