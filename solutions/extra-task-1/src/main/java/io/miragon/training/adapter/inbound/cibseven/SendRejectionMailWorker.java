package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.SendRejectionMailUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendRejectionMailWorker {

    private static final Logger log = LoggerFactory.getLogger(SendRejectionMailWorker.class);

    private final SendRejectionMailUseCase useCase;

    public SendRejectionMailWorker(SendRejectionMailUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_REJECTION_MAIL)
    public void sendRejectionMail(@Variable(name = "membershipId") String membershipId) {
        log.debug("Received task to send rejection mail for membership: {}", membershipId);
        useCase.sendRejectionMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
