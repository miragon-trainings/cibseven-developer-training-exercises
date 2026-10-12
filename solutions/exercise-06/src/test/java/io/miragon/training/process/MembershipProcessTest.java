package io.miragon.training.process;

import io.miragon.bpmn.runtime.path.PathWalk;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;
import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.SendRejectionMailUseCase;
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.training.application.port.outbound.MembershipProcess;
import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import io.miragon.training.domain.Name;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.miragon.training.process.util.ProcessEngineTestUtils.continueToNextWaitState;
import static io.miragon.training.process.util.ProcessEngineTestUtils.findProcessInstance;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the membership process at the "gateway" stage (exercise 4).
 *
 * <p>The real BPMN and the real JavaDelegates run inside an in-memory engine; only the inbound
 * use cases (the business logic behind each delegate) are mocked, so the test verifies the process
 * <em>wiring</em> — sequence flows, the capacity gateway, and the wait state at the user task.
 */
@SpringBootTest
@ActiveProfiles("test")
class MembershipProcessTest {

    @Autowired
    private MembershipProcess membershipProcess;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessEngine processEngine;

    @MockitoBean
    private ClaimMembershipUseCase claimMembershipUseCase;

    @MockitoBean
    private SendConfirmationMailUseCase sendConfirmationMailUseCase;

    @MockitoBean
    private SendRejectionMailUseCase sendRejectionMailUseCase;

    @MockitoBean
    private SendWelcomeMailUseCase sendWelcomeMailUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine);
    }

    @Test
    void happyPath_membershipIsConfirmedAndWelcomeMailIsSent() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);

        Membership membership = new Membership(new Email("jane@example.com"), new Name("Jane"), new Age(30));
        MembershipId id = membership.id();
        membershipProcess.startProcess(membership);

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine);

        assertThat(instance).isWaitingAt(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);

        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult()
                .getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine);

        var happyPath = PathWalk.from(FlowNodes.startEventSubmitRegistration())
                .then(next -> next.serviceTaskClaimMembership())
                .then(next -> next.gatewayHasEmptySpots())
                .then(next -> next.serviceTaskSendConfirmationMail())
                .then(next -> next.userTaskConfirmMembership())
                .then(next -> next.serviceTaskSendWelcomeMail())
                .end(next -> next.endEventMembershipConfirmed());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(happyPath.getIds())
                .hasNotPassed(
                        FlowNodes.ServiceTaskSendRejectionMail.ELEMENT_ID,
                        FlowNodes.EndEventMembershipRejected.ELEMENT_ID);

        verify(claimMembershipUseCase).claimMembership(id);
        verify(sendConfirmationMailUseCase).sendConfirmationMail(id);
        verify(sendWelcomeMailUseCase).sendWelcomeMail(id);
        verify(sendRejectionMailUseCase, never()).sendRejectionMail(any());
    }

    @Test
    void noCapacity_membershipIsRejected() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);

        Membership membership = new Membership(new Email("jack@example.com"), new Name("Jack"), new Age(40));
        MembershipId id = membership.id();
        membershipProcess.startProcess(membership);

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine);

        var rejectionPath = PathWalk.from(FlowNodes.startEventSubmitRegistration())
                .then(next -> next.serviceTaskClaimMembership())
                .then(next -> next.gatewayHasEmptySpots())
                .then(next -> next.serviceTaskSendRejectionMail())
                .end(next -> next.endEventMembershipRejected());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(rejectionPath.getIds())
                .hasNotPassed(
                        FlowNodes.ServiceTaskSendConfirmationMail.ELEMENT_ID,
                        FlowNodes.UserTaskConfirmMembership.ELEMENT_ID,
                        FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID,
                        FlowNodes.EndEventMembershipConfirmed.ELEMENT_ID);

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
        verify(sendWelcomeMailUseCase, never()).sendWelcomeMail(any());
    }
}
