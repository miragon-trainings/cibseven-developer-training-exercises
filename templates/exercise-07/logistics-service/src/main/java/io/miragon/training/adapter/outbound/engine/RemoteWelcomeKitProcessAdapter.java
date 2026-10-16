package io.miragon.training.adapter.outbound.engine;

import io.miragon.training.application.port.outbound.WelcomeKitProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Drives the (remote) engine to start the OWNED {@code sendWelcomeKit} process (Direction 2: worker -> engine).
 *
 * <p>TODO Exercise 7: use the typed client of the CIB seven community REST client. Its Spring Boot starter
 * already provides the {@code ProcessDefinitionApi} bean (package {@code org.cibseven.community.rest.client.*}),
 * so inject it via the constructor and start your own process by key. You fill in the concrete arguments
 * yourself:
 *
 * <pre>
 *   // inject ProcessDefinitionApi via the constructor, then:
 *   var request = new StartProcessInstanceDto().variables(...);   // "name" as a VariableValueDto (type String)
 *   processDefinitionApi.startProcessInstanceByKey(..., request); // process key: SendWelcomeKitProcessApi.PROCESS_ID
 * </pre>
 * The client throws the checked {@code ApiException} – wrap it into an unchecked exception.
 */
@Component
public class RemoteWelcomeKitProcessAdapter implements WelcomeKitProcess {

    private static final Logger log = LoggerFactory.getLogger(RemoteWelcomeKitProcessAdapter.class);

    @Override
    public void startWelcomeKit(String memberName) {
        throw new UnsupportedOperationException(
                "TODO Exercise 7: start the process via the typed client (ProcessDefinitionApi)");
    }
}
