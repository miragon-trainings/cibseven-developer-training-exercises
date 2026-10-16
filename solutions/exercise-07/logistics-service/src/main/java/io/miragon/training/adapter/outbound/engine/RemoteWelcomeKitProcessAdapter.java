package io.miragon.training.adapter.outbound.engine;

import io.miragon.training.adapter.process.SendWelcomeKitProcessApi;
import io.miragon.training.application.port.outbound.WelcomeKitProcess;
import org.cibseven.community.rest.client.api.ProcessDefinitionApi;
import org.cibseven.community.rest.client.dto.StartProcessInstanceDto;
import org.cibseven.community.rest.client.dto.VariableValueDto;
import org.cibseven.community.rest.client.invoker.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Drives the shared, remote CIB seven engine over its REST API to start the owned {@code sendWelcomeKit}
 * process (Direction 2: worker -> engine). The REST call goes through the <strong>typed</strong>
 * {@link ProcessDefinitionApi} of the CIB seven community REST client (auto-configured by its Spring Boot
 * starter) rather than hand-built REST, so URIs and the typed-variable payload ({@code {value, type}})
 * are compile-checked. The process id comes from the typed process API this service generates from its
 * own BPMN.
 */
@Component
public class RemoteWelcomeKitProcessAdapter implements WelcomeKitProcess {

    private static final Logger log = LoggerFactory.getLogger(RemoteWelcomeKitProcessAdapter.class);

    private final ProcessDefinitionApi processDefinitionApi;

    public RemoteWelcomeKitProcessAdapter(ProcessDefinitionApi processDefinitionApi) {
        this.processDefinitionApi = processDefinitionApi;
    }

    @Override
    public void startWelcomeKit(String memberName) {
        StartProcessInstanceDto request = new StartProcessInstanceDto()
                .variables(Map.of("name", new VariableValueDto().value(memberName).type("String")));

        try {
            processDefinitionApi.startProcessInstanceByKey(SendWelcomeKitProcessApi.PROCESS_ID.getValue(), request);
        } catch (ApiException e) {
            throw new IllegalStateException("Could not start a remote sendWelcomeKit instance", e);
        }
        log.info("Started a remote sendWelcomeKit instance for member {}", memberName);
    }
}
