package io.miragon.training.adapter.outbound.cibseven;

// TODO Exercise 3: Uncomment this class (remove the /* and */ lines)
//  and then implement the TODOs in startProcess(...) and confirm(...).
//  It needs the CIB Seven engine that you switch on in Exercise 1.
/*
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
        // TODO Exercise 3: Start the process instance yourself via the RuntimeService.
        //  - Correlate the start message "Message_SubscriptionRequested" (createMessageCorrelation).
        //  - Set the business key of the new instance to membership.id().value().toString().
        //  - Pass these four process variables as a Map (the keys must be named exactly like this):
        //      "membershipId" <- membership.id().value().toString()
        //      "email"          <- membership.email().value()
        //      "name"           <- membership.name().value()
        //      "age"            <- membership.age().value()
        //  How you assemble the correlation builder and the variables Map is something you build
        //  yourself – there is deliberately no ready-made call line here.
        throw new UnsupportedOperationException("Exercise 3: Start the process via the RuntimeService");
    }

    @Override
    public void confirm(MembershipId membershipId) {
        // TODO Exercise 3: Complete the open confirmation User Task yourself via the TaskService.
        //  - Find the task with definition key "userTask_confirmMembership" whose process instance has
        //    the business key membershipId.value().toString() (createTaskQuery).
        //  - Complete it (taskService.complete). Fail if no such task exists.
        //  How you build the task query and complete the task is something you build yourself –
        //  there is deliberately no ready-made call line here.
        throw new UnsupportedOperationException("Exercise 3: Complete the confirmation task via the TaskService");
    }
}
*/
