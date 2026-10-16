package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.RegisterMembershipUseCase;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import io.miragon.training.domain.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegisterMembershipService implements RegisterMembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterMembershipService.class);

    private final MembershipRepository repository;
    private final MembershipProcess process;

    public RegisterMembershipService(MembershipRepository repository, MembershipProcess process) {
        this.repository = repository;
        this.process = process;
    }

    @Override
    public MembershipId register(Command command) {
        log.info("Registering membership for {}", command.email());
        var membership = new Membership(new Email(command.email()), new Name(command.name()), new Age(command.age()));
        repository.save(membership);
        process.startProcess(membership);
        return membership.id();
    }
}
