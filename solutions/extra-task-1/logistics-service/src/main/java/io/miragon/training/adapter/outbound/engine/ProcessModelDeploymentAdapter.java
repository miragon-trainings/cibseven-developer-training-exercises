package io.miragon.training.adapter.outbound.engine;

import dev.bpmcrafters.processengineapi.deploy.DeployBundleCommand;
import dev.bpmcrafters.processengineapi.deploy.DeploymentApi;
import dev.bpmcrafters.processengineapi.deploy.NamedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deploys the process model this service OWNS into the shared, remote engine at start-up, through the
 * engine-neutral {@link DeploymentApi}. The worker library's own auto-deployment does not kick in with
 * the remote adapter (its condition is evaluated before the adapter registers the {@code DeploymentApi}),
 * so this adapter takes over.
 */
@Component
public class ProcessModelDeploymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProcessModelDeploymentAdapter.class);

    private static final String MODEL_PATTERN = "classpath*:bpmn/*.bpmn";

    private final DeploymentApi deploymentApi;

    public ProcessModelDeploymentAdapter(DeploymentApi deploymentApi) {
        this.deploymentApi = deploymentApi;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void deployProcessModel() throws IOException {
        List<NamedResource> models = new ArrayList<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources(MODEL_PATTERN)) {
            models.add(new NamedResource(resource.getFilename(), resource.getInputStream(), Map.of()));
        }
        deploymentApi.deploy(new DeployBundleCommand(models, null)).join();
        log.info("Deployed {} process model(s) to the remote engine", models.size());
    }
}
