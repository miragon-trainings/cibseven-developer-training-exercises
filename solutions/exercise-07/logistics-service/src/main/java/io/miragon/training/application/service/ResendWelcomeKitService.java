package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.ResendWelcomeKitUseCase;
import io.miragon.training.application.port.outbound.WelcomeKitProcess;
import org.springframework.stereotype.Service;

@Service
public class ResendWelcomeKitService implements ResendWelcomeKitUseCase {

    private final WelcomeKitProcess welcomeKitProcess;

    public ResendWelcomeKitService(WelcomeKitProcess welcomeKitProcess) {
        this.welcomeKitProcess = welcomeKitProcess;
    }

    @Override
    public void resendWelcomeKit(String memberName) {
        welcomeKitProcess.startWelcomeKit(memberName);
    }
}
