package io.miragon.training.adapter.inbound.cibseven;

import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import org.cibseven.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component
public class SendWelcomeMailDelegate extends BaseDelegate {

    private final SendWelcomeMailUseCase useCase;

    public SendWelcomeMailDelegate(SendWelcomeMailUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    protected void executeTask(DelegateExecution execution) {
        var email = (String) execution.getVariable("email");
        log.debug("Received task to send welcome mail to {}", email);
        useCase.sendWelcomeMail(email);
    }
}
