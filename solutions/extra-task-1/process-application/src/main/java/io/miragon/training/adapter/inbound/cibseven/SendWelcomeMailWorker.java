package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendWelcomeMailWorker {

    private static final Logger log = LoggerFactory.getLogger(SendWelcomeMailWorker.class);

    private final SendWelcomeMailUseCase useCase;

    public SendWelcomeMailWorker(SendWelcomeMailUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_WELCOME_MAIL)
    public void sendWelcomeMail(@Variable(name = "membershipId") String membershipId) {
        log.debug("Received task to send welcome mail for membership: {}", membershipId);
        useCase.sendWelcomeMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
