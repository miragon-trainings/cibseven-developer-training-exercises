package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.StartProvisioningUseCase;
import io.miragon.training.application.port.outbound.ProvisioningProcess;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StartProvisioningService implements StartProvisioningUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartProvisioningService.class);

    private final ProvisioningProcess process;

    public StartProvisioningService(ProvisioningProcess process) {
        this.process = process;
    }

    @Override
    public MembershipId start(Command command) {
        var membershipId = new MembershipId();
        log.info("Starting membership provisioning for {} (membershipId={})", command.email(), membershipId.value());
        process.start(membershipId, command.email(), command.name(), command.age());
        return membershipId;
    }
}
