package io.miragon.training.adapter.inbound.rest;

// TODO Exercise 4: Uncomment this class (remove the /* and */ lines).
/*
import io.miragon.training.application.port.inbound.ConfirmMembershipUseCase;
import io.miragon.training.application.port.inbound.RegisterMembershipUseCase;
import io.miragon.training.domain.MembershipId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final RegisterMembershipUseCase registerMembership;
    private final ConfirmMembershipUseCase confirmMembership;

    public MembershipController(RegisterMembershipUseCase registerMembership, ConfirmMembershipUseCase confirmMembership) {
        this.registerMembership = registerMembership;
        this.confirmMembership = confirmMembership;
    }

    @PostMapping
    public ResponseEntity<String> register(@RequestBody MembershipForm form) {
        var membershipId = registerMembership.register(
                new RegisterMembershipUseCase.Command(form.email(), form.name(), form.age())
        );
        return ResponseEntity.ok(membershipId.value().toString());
    }

    @PostMapping("/{membershipId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable UUID membershipId) {
        confirmMembership.confirm(new MembershipId(membershipId));
        return ResponseEntity.ok().build();
    }

    public record MembershipForm(String email, String name, int age) {}
}
*/
