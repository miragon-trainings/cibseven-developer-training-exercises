package io.miragon.training;

import org.springframework.boot.SpringApplication;
// TODO Exercise 1: Uncomment the imports
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Application entry point.
 *
 * <p>TODO Exercise 1: Enable the {@code @SpringBootApplication} and {@code @EnableJpaRepositories}
 * annotations. Only then do Spring Boot auto-configuration and the CIB Seven engine's automatic
 * BPMN deployment kick in (all {@code *.bpmn} files under {@code src/main/resources} are deployed
 * on start-up). Without the annotations only an empty context starts – no engine, no Cockpit.
 */
// TODO Exercise 1: Uncomment the annotations
// @SpringBootApplication
// @EnableJpaRepositories
public class TrainingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}
