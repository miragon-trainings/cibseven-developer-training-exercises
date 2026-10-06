package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendConfirmationMailWorker {

    private static final Logger log = LoggerFactory.getLogger(SendConfirmationMailWorker.class);

    private final SendConfirmationMailUseCase useCase;

    public SendConfirmationMailWorker(SendConfirmationMailUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_CONFIRMATION_MAIL)
    public void sendConfirmationMail(@Variable(name = "membershipId") String membershipId) {
        log.debug("Received task to send confirmation mail for membership: {}", membershipId);
        useCase.sendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
