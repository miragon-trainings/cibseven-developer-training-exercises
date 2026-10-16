package io.miragon.training.adapter.outbound.cibseven;

import io.miragon.training.adapter.process.Messages;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.TaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MembershipProcessAdapter implements MembershipProcess {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public MembershipProcessAdapter(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Override
    public void startProcess(Membership membership) {
        runtimeService.createMessageCorrelation(Messages.SUBSCRIPTION_REQUESTED.getValue())
                .processInstanceBusinessKey(membership.id().value().toString())
                .setVariables(Map.of(
                        "membershipId", membership.id().value().toString(),
                        "email", membership.email().value(),
                        "name", membership.name().value(),
                        "age", membership.age().value()
                ))
                .correlateStartMessage();
    }

    @Override
    public void confirm(MembershipId membershipId) {
        var task = taskService.createTaskQuery()
                .taskDefinitionKey(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID)
                .processInstanceBusinessKey(membershipId.value().toString())
                .singleResult();
        if (task == null) {
            throw new IllegalStateException("No open confirmation task for membership " + membershipId.value());
        }
        taskService.complete(task.getId());
    }
}
