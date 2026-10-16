package io.miragon.training.adapter.outbound.cibseven;

import dev.bpmcrafters.processengineapi.CommonRestrictions;
import dev.bpmcrafters.processengineapi.correlation.CorrelateMessageCmd;
import dev.bpmcrafters.processengineapi.correlation.Correlation;
import dev.bpmcrafters.processengineapi.correlation.CorrelationApi;
import dev.bpmcrafters.processengineapi.process.StartProcessApi;
import dev.bpmcrafters.processengineapi.process.StartProcessByMessageCmd;
import dev.bpmcrafters.processengineapi.task.CompleteTaskCmd;
import dev.bpmcrafters.processengineapi.task.TaskInformation;
import dev.bpmcrafters.processengineapi.task.TaskSubscriptionApi;
import dev.bpmcrafters.processengineapi.task.UserTaskCompletionApi;
import dev.bpmcrafters.processengineapi.task.support.UserTaskSupport;
import io.miragon.training.adapter.process.Messages;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Outbound process adapter – engine-neutral.
 * Starts the process via {@link StartProcessApi} and correlates the rejection message
 * via {@link CorrelationApi}, instead of the native CIB7 {@code RuntimeService}.
 * Open confirmation tasks arrive through a user-task subscription and are completed via
 * {@link UserTaskCompletionApi}, instead of the native CIB7 {@code TaskService}.
 */
@Component
public class MembershipProcessAdapter implements MembershipProcess {

    private final StartProcessApi startProcessApi;
    private final CorrelationApi correlationApi;
    private final UserTaskCompletionApi userTaskCompletionApi;
    private final UserTaskSupport confirmationTasks = new UserTaskSupport();

    public MembershipProcessAdapter(
            StartProcessApi startProcessApi,
            CorrelationApi correlationApi,
            TaskSubscriptionApi taskSubscriptionApi,
            UserTaskCompletionApi userTaskCompletionApi
    ) {
        this.startProcessApi = startProcessApi;
        this.correlationApi = correlationApi;
        this.userTaskCompletionApi = userTaskCompletionApi;
        confirmationTasks.subscribe(
                taskSubscriptionApi,
                CommonRestrictions.builder().build(),
                FlowNodes.UserTaskConfirmMembership.ELEMENT_ID,
                Set.of("membershipId")
        );
    }

    @Override
    public void startProcess(Membership membership) {
        var membershipId = membership.id().value().toString();
        startProcessApi.startProcess(
                new StartProcessByMessageCmd(
                        Messages.SUBSCRIPTION_REQUESTED.getValue(),
                        Map.of(
                                "membershipId", membershipId,
                                "email", membership.email().value(),
                                "name", membership.name().value(),
                                "age", membership.age().value(),
                                // Sets the global correlation key so later messages find this instance
                                CommonRestrictions.CORRELATION_KEY, membershipId
                        )
                )
        ).join();
    }

    @Override
    public void confirm(MembershipId membershipId) {
        var id = membershipId.value().toString();
        var taskId = confirmationTasks.getAllTasks().stream()
                .map(TaskInformation::getTaskId)
                .filter(candidate -> id.equals(confirmationTasks.getPayload(candidate).get("membershipId")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No open confirmation task for membership " + id));
        userTaskCompletionApi.completeTask(new CompleteTaskCmd(taskId)).join();
    }

    @Override
    public void rejectMembership(MembershipId membershipId) {
        var id = membershipId.value().toString();
        correlationApi.correlateMessage(
                new CorrelateMessageCmd(
                        Messages.CONFIRMATION_REJECTED.getValue(),
                        Map.of("membershipId", id),
                        Correlation.withKey(id),
                        CommonRestrictions.builder()
                                .withRestriction("useGlobalCorrelationKey", "true")
                                .build()
                )
        ).join();
    }
}
