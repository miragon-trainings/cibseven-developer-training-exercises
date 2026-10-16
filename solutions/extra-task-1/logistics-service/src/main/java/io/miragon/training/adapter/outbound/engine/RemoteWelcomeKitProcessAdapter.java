package io.miragon.training.adapter.outbound.engine;

import dev.bpmcrafters.processengineapi.process.StartProcessApi;
import dev.bpmcrafters.processengineapi.process.StartProcessByDefinitionCmd;
import io.miragon.training.adapter.process.SendWelcomeKitProcessApi;
import io.miragon.training.application.port.outbound.WelcomeKitProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Outbound process adapter – engine-neutral. Starts the owned {@code sendWelcomeKit} process via
 * {@link StartProcessApi}; the CIB seven remote adapter turns that into a call against {@code /engine-rest}.
 */
@Component
public class RemoteWelcomeKitProcessAdapter implements WelcomeKitProcess {

    private static final Logger log = LoggerFactory.getLogger(RemoteWelcomeKitProcessAdapter.class);

    private final StartProcessApi startProcessApi;

    public RemoteWelcomeKitProcessAdapter(StartProcessApi startProcessApi) {
        this.startProcessApi = startProcessApi;
    }

    @Override
    public void startWelcomeKit(String memberName) {
        startProcessApi.startProcess(new StartProcessByDefinitionCmd(
                SendWelcomeKitProcessApi.PROCESS_ID.getValue(),
                Map.of("name", memberName)
        )).join();
        log.info("Started a remote sendWelcomeKit instance for member {}", memberName);
    }
}
