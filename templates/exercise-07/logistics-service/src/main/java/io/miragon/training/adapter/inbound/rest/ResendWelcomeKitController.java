package io.miragon.training.adapter.inbound.rest;

import io.miragon.training.application.port.inbound.ResendWelcomeKitUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * A small action owned by the Logistics service: re-send a welcome kit on demand. It starts a fresh
 * {@code sendWelcomeKit} instance through the engine — the moment the department drives the shared
 * engine over REST (Direction 2), through the generated typed client behind {@code WelcomeKitProcess}.
 */
@RestController
@RequestMapping("/api/welcome-kits")
public class ResendWelcomeKitController {

    private final ResendWelcomeKitUseCase resendWelcomeKit;

    public ResendWelcomeKitController(ResendWelcomeKitUseCase resendWelcomeKit) {
        this.resendWelcomeKit = resendWelcomeKit;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resend(@RequestBody ResendForm form) {
        resendWelcomeKit.resendWelcomeKit(form.name());
    }

    public record ResendForm(String name) {
    }
}
