package io.miragon.training.adapter.process;

import dev.bpmcrafters.processengineapi.adapter.cibseven.embedded.shared.EngineCommandExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the process-engine-api onto the embedded CIB7 engine.
 */
@Configuration
public class EngineApiConfiguration {

    /**
     * Executes engine commands in the same thread as the calling operation, so that
     * engine state and business data share one transaction (combined commit / rollback).
     * Without this same-thread executor, engine and business data may diverge.
     */
    @Bean
    public EngineCommandExecutor engineCommandExecutor() {
        return new EngineCommandExecutor(Runnable::run);
    }
}
