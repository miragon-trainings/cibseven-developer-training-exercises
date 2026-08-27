package io.miragon.training.adapter.outbound.cibseven;

import io.miragon.training.application.port.outbound.ProvisioningProcess;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProvisioningProcessAdapter implements ProvisioningProcess {

    /** Process id from provision-membership.bpmn. Kept as a literal (no bpmn-to-code in this example). */
    private static final String PROCESS_KEY = "provisionMembership";

    private final RuntimeService runtimeService;

    public ProvisioningProcessAdapter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void start(MembershipId membershipId, String email, String name, int age) {
        runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                membershipId.value().toString(),
                Map.of(
                        "membershipId", membershipId.value().toString(),
                        "email", email,
                        "name", name,
                        "age", age
                )
        );
    }
}
