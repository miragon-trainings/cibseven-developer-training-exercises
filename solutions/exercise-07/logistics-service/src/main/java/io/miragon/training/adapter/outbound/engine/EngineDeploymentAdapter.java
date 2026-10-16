package io.miragon.training.adapter.outbound.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Deploys the process model this service OWNS (carried in its own {@code src/main/resources/bpmn}) into
 * the shared, remote engine at start-up — the remote counterpart to the embedded engine's classpath
 * auto-deployment. This realises the "the service owns and deploys its process" pattern (Pattern A):
 * the engine host stays model-agnostic, and this outbound adapter pushes the BPMN to
 * {@code POST /engine-rest/deployment/create}.
 *
 * <p>The deployment is <strong>idempotent</strong> ({@code enable-duplicate-filtering} +
 * {@code deploy-changed-only}), so restarts and multiple worker instances never pile up duplicate
 * deployments. It retries briefly so the worker may start alongside a not-yet-ready engine.
 *
 * <p>Unlike the process-driving calls (which go through the generated typed client), this one stays on
 * a hand-built {@link RestClient}: {@code deployment/create} is a multipart upload with a dynamic file
 * field name, which the OpenAPI spec models as a single binary field — so the generated client cannot
 * faithfully express it. This is the one documented exception to "drive the engine through the client".
 */
@Component
public class EngineDeploymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(EngineDeploymentAdapter.class);

    private static final String DEPLOYMENT_NAME = "logistics-service";
    private static final String MODEL_PATTERN = "classpath*:bpmn/*.bpmn";
    private static final int MAX_ATTEMPTS = 20;
    private static final long RETRY_DELAY_MS = 3_000L;

    private final RestClient engineRestClient;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public EngineDeploymentAdapter(RestClient engineRestClient) {
        this.engineRestClient = engineRestClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void deployProcessModel() throws IOException {
        List<Resource> resources = List.of(resolver.getResources(MODEL_PATTERN));
        if (resources.isEmpty()) {
            log.warn("No process resources found on the classpath — nothing to deploy");
            return;
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                deploy(resources);
                log.info("Deployed {} process resource(s) to the remote engine: {}", resources.size(), filenames(resources));
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Could not deploy the process model after {} attempts — is the engine running?", MAX_ATTEMPTS, e);
                    return;
                }
                log.warn("Engine not reachable yet (attempt {}/{}), retrying in {}ms", attempt, MAX_ATTEMPTS, RETRY_DELAY_MS);
                sleep();
            }
        }
    }

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

    private static List<String> filenames(List<Resource> resources) {
        List<String> names = new ArrayList<>();
        for (Resource resource : resources) {
            names.add(resource.getFilename());
        }
        return names;
    }

    private static void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
