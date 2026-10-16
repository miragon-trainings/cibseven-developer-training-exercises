package io.miragon.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Logistics department's own service — a standalone JVM that knows the shared CIB seven engine
 * only through its REST API. It OWNS the {@code sendWelcomeKit} process and talks to the engine solely
 * through the process-engine-api, backed by the CIB seven remote adapter.
 */
@SpringBootApplication
public class TrainingWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingWorkerApplication.class, args);
    }
}
