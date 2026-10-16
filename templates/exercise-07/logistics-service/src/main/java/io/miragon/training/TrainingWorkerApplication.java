package io.miragon.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Logistics department's own service — a standalone JVM that knows the shared CIB seven engine
 * only through its REST API. It OWNS the {@code sendWelcomeKit} process: it carries the model, deploys
 * it into the engine at start-up, fulfils its service task as an external task, and drives it over REST.
 */
@SpringBootApplication
public class TrainingWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingWorkerApplication.class, args);
    }
}
