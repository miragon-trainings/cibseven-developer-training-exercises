package io.miragon.training.adapter.outbound.cibseven;

import dev.bpmcrafters.processengineapi.CommonRestrictions;
import dev.bpmcrafters.processengineapi.correlation.CorrelateMessageCmd;
import dev.bpmcrafters.processengineapi.correlation.Correlation;
import dev.bpmcrafters.processengineapi.correlation.CorrelationApi;
import dev.bpmcrafters.processengineapi.process.StartProcessApi;
import dev.bpmcrafters.processengineapi.process.StartProcessByMessageCmd;
import io.miragon.training.adapter.process.Messages;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Outbound process adapter – engine-neutral.
 * Starts the process via {@link StartProcessApi} and correlates the rejection message
 * via {@link CorrelationApi}, instead of the native CIB7 {@code RuntimeService}.
 */
@Component
public class MembershipProcessAdapter implements MembershipProcess {

    private final StartProcessApi startProcessApi;
    private final CorrelationApi correlationApi;

    public MembershipProcessAdapter(StartProcessApi startProcessApi, CorrelationApi correlationApi) {
        this.startProcessApi = startProcessApi;
        this.correlationApi = correlationApi;
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
