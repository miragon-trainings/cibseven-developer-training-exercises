package io.miragon.training.adapter.inbound.rest;

import io.miragon.training.application.port.inbound.ConfirmMembershipUseCase;
import io.miragon.training.application.port.inbound.RegisterMembershipUseCase;
import io.miragon.training.application.port.inbound.RejectMembershipUseCase;
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
    private final RejectMembershipUseCase rejectMembership;

    public MembershipController(
            RegisterMembershipUseCase registerMembership,
            ConfirmMembershipUseCase confirmMembership,
            RejectMembershipUseCase rejectMembership
    ) {
        this.registerMembership = registerMembership;
        this.confirmMembership = confirmMembership;
        this.rejectMembership = rejectMembership;
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

    @PostMapping("/{membershipId}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID membershipId) {
        rejectMembership.rejectMembership(new MembershipId(membershipId));
        return ResponseEntity.ok().build();
    }

    public record MembershipForm(String email, String name, int age) {
    }
}
