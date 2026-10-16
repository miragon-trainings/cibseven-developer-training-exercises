package io.miragon.training.adapter.outbound.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Deploys the process model this service OWNS (in its own {@code src/main/resources/bpmn}) into the shared,
 * remote engine at start-up — the remote counterpart to the embedded engine's classpath auto-deployment.
 * So the engine host stays model-agnostic and this service pushes its own BPMN to the engine over REST.
 */
@Component
public class EngineDeploymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(EngineDeploymentAdapter.class);

    private static final String DEPLOYMENT_NAME = "logistics-service";

    private final RestClient engineRestClient;

    public EngineDeploymentAdapter(RestClient engineRestClient) {
        this.engineRestClient = engineRestClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void deployProcessModel() {
        // TODO Exercise 7: deploy the OWNED model (src/main/resources/bpmn/*.bpmn) into the remote engine.
        //   Resolve the resources (classpath*:bpmn/*.bpmn), then POST them as multipart/form-data to
        //   /deployment/create with the idempotency flags enable-duplicate-filtering + deploy-changed-only.
        //   Retry a few times so the worker may start before the engine is ready. See the deploy(...) hint
        //   below and ProcessModelDeploymentAdapter in miragon-blueprints/cibseven-remote-example.
        log.warn("TODO Exercise 7: EngineDeploymentAdapter does not deploy the owned model yet");
    }

    // Hint: this is the idempotent multipart deployment call the TODO above should make.
    @SuppressWarnings("unused")
    private void deploy(List<Resource> resources) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("deployment-name", DEPLOYMENT_NAME);
        body.add("deployment-source", "logistics-service");
        body.add("enable-duplicate-filtering", "true");
        body.add("deploy-changed-only", "true");
        for (Resource resource : resources) {
            body.add(resource.getFilename(), resource);
        }

        engineRestClient.post()
                .uri("/deployment/create")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
