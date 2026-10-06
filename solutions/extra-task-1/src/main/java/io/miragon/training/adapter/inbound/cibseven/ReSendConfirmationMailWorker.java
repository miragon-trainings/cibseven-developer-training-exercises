package io.miragon.training.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.training.adapter.process.ServiceTasks;
import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReSendConfirmationMailWorker {

    private static final Logger log = LoggerFactory.getLogger(ReSendConfirmationMailWorker.class);

    private final ReSendConfirmationMailUseCase useCase;

    public ReSendConfirmationMailWorker(ReSendConfirmationMailUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.RE_SEND_CONFIRMATION_MAIL)
    public void reSendConfirmationMail(@Variable(name = "membershipId") String membershipId) {
        log.debug("Received task to re-send confirmation mail for membership: {}", membershipId);
        useCase.reSendConfirmationMail(new MembershipId(UUID.fromString(membershipId)));
    }
}
