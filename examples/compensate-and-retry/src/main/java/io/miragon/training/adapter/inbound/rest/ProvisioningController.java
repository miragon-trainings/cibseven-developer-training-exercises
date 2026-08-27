package io.miragon.training.adapter.inbound.rest;

import io.miragon.training.application.port.inbound.StartProvisioningUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provisioning")
public class ProvisioningController {

    private final StartProvisioningUseCase startProvisioning;

    public ProvisioningController(StartProvisioningUseCase startProvisioning) {
        this.startProvisioning = startProvisioning;
    }

    @PostMapping
    public ResponseEntity<String> start(@RequestBody ProvisioningForm form) {
        var membershipId = startProvisioning.start(
                new StartProvisioningUseCase.Command(form.email(), form.name(), form.age())
        );
        return ResponseEntity.ok(membershipId.value().toString());
    }

    public record ProvisioningForm(String email, String name, int age) {
    }
}
