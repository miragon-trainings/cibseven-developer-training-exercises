package io.miragon.training.process;

import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.application.port.inbound.NotifyCommunityUseCase;
import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.RevokeClaimUseCase;
import io.miragon.training.application.port.inbound.SendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.SendRejectionMailUseCase;
import io.miragon.training.application.port.inbound.SendWelcomeMailUseCase;
import io.miragon.bpmn.runtime.path.PathWalk;
import io.miragon.training.adapter.process.MembershipProcessApi.FlowNodes;
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

import static io.miragon.training.process.util.ProcessEngineTestUtils.findProcessInstance;
import static io.miragon.training.process.util.ProcessEngineTestUtils.fireTimer;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the membership process at the "boundary events" stage (exercise 5).
 *
 * <p>On top of the gateway process (exercise 3) this adds a confirmation subprocess with three
 * boundary events: a non-interrupting daily resend timer, an interrupting abort timer, and an
 * interrupting reject message. Each of those paths gets its own test.
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
    private ReSendConfirmationMailUseCase reSendConfirmationMailUseCase;

    @MockitoBean
    private SendRejectionMailUseCase sendRejectionMailUseCase;

    @MockitoBean
    private SendWelcomeMailUseCase sendWelcomeMailUseCase;

    @MockitoBean
    private NotifyCommunityUseCase notifyCommunityUseCase;

    @MockitoBean
    private RevokeClaimUseCase revokeClaimUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine);
    }

    private ProcessInstance startWaitingAtConfirmation(MembershipId id, String email, String name) {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);
        membershipProcess.startProcess(new Membership(id, new Email(email), new Name(name), new Age(30)));
        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        execute(job(FlowNodes.StartEventSubmitRegistration.ELEMENT_ID, instance));
        execute(job(FlowNodes.ServiceTaskSendConfirmationMail.ELEMENT_ID, instance));
        return instance;
    }

    private void completeConfirmationTask(ProcessInstance instance) {
        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult()
                .getId();
        taskService.complete(taskId);
        execute(job(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID, instance));
        execute(job(FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, instance));
        execute(job(FlowNodes.ServiceTaskNotifyCommunity.ELEMENT_ID, instance));
    }

    @Test
    void happyPath_membershipIsActivated() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "jane@example.com", "Jane");

        // when
        completeConfirmationTask(instance);

        // then
        // The subprocess is a scope bracket: onto() steps onto it without recording it, inside() walks
        // its interior and resumes on the subprocess, so the step to the fork is checked again.
        var happyPath = PathWalk.from(FlowNodes.startEventSubmitRegistration())
                .then(next -> next.serviceTaskClaimMembership())
                .then(next -> next.gatewayHasEmptySpots())
                .onto(next -> next.subProcessConfirmMembership())
                .inside(FlowNodes.subProcessConfirmMembership(), start -> PathWalk.from(start.startEventConfirmationRequired())
                        .then(next -> next.serviceTaskSendConfirmationMail())
                        .then(next -> next.userTaskConfirmMembership())
                        .end(next -> next.endEventMembershipConfirmed()))
                .then(next -> next.gatewayNotifyFork())
                .then(next -> next.serviceTaskSendWelcomeMail())
                .then(next -> next.gatewayNotifyJoin())
                .end(next -> next.endEventMembershipActivated());

        assertThat(instance)
                .isEnded()
                // An ordered path only makes sense within one sequential branch. The path above walks the
                // welcome-mail branch; the parallel community branch is asserted unordered below.
                .hasPassedInOrder(happyPath.getIds())
                .hasPassed(
                        FlowNodes.SubProcessConfirmMembership.ELEMENT_ID,
                        FlowNodes.ServiceTaskNotifyCommunity.ELEMENT_ID)
                .hasNotPassed(
                        FlowNodes.ServiceTaskRevokeClaim.ELEMENT_ID,
                        FlowNodes.EndEventMembershipDeclined.ELEMENT_ID,
                        FlowNodes.ServiceTaskSendRejectionMail.ELEMENT_ID,
                        FlowNodes.EndEventMembershipRejected.ELEMENT_ID);

        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);
        verify(sendWelcomeMailUseCase, times(1)).sendWelcomeMail(id);
        verify(notifyCommunityUseCase, times(1)).notifyCommunity(id);
        verify(revokeClaimUseCase, never()).revokeClaim(any());
    }

    @Test
    void noCapacity_membershipIsRejected() {
        // given
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);
        MembershipId id = new MembershipId();
        membershipProcess.startProcess(new Membership(id, new Email("jack@example.com"), new Name("Jack"), new Age(40)));
        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());

        // when
        execute(job(FlowNodes.StartEventSubmitRegistration.ELEMENT_ID, instance));
        execute(job(FlowNodes.ServiceTaskSendRejectionMail.ELEMENT_ID, instance));

        // then
        var rejectionPath = PathWalk.from(FlowNodes.startEventSubmitRegistration())
                .then(next -> next.serviceTaskClaimMembership())
                .then(next -> next.gatewayHasEmptySpots())
                .then(next -> next.serviceTaskSendRejectionMail())
                .end(next -> next.endEventMembershipRejected());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(rejectionPath.getIds())
                .hasNotPassed(FlowNodes.SubProcessConfirmMembership.ELEMENT_ID, FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID);

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
        verify(sendWelcomeMailUseCase, never()).sendWelcomeMail(any());
    }

    @Test
    void abortTimer_interruptsSubprocessAndRevokesClaim() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "amy@example.com", "Amy");

        // when
        fireTimer(processEngine, FlowNodes.EventAbortAfter3HalfDays.ELEMENT_ID);
        execute(job(jobQuery().messages().activityId(FlowNodes.EventAbortAfter3HalfDays.ELEMENT_ID), instance));

        // then
        var abortPath = PathWalk.from(FlowNodes.userTaskConfirmMembership())
                .interruptedBy(FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventAbortAfter3HalfDays())
                .then(next -> next.serviceTaskRevokeClaim())
                .end(next -> next.endEventMembershipDeclined());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(abortPath.getIds())
                .hasNotPassed(FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, FlowNodes.EndEventMembershipActivated.ELEMENT_ID);

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
        verify(sendWelcomeMailUseCase, never()).sendWelcomeMail(any());
    }

    @Test
    void rejectMessage_interruptsSubprocessAndRevokesClaim() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "ben@example.com", "Ben");

        // when
        membershipProcess.rejectMembership(id);
        execute(job(FlowNodes.EventConfirmationRejected.ELEMENT_ID, instance));

        // then
        var rejectPath = PathWalk.from(FlowNodes.userTaskConfirmMembership())
                .interruptedBy(FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventConfirmationRejected())
                .then(next -> next.serviceTaskRevokeClaim())
                .end(next -> next.endEventMembershipDeclined());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(rejectPath.getIds())
                .hasNotPassed(FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, FlowNodes.EndEventMembershipActivated.ELEMENT_ID);

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void resendTimer_nonInterrupting_resendsConfirmationMailAndKeepsWaiting() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "cara@example.com", "Cara");

        // when
        fireTimer(processEngine, FlowNodes.EventResendEveryDay.ELEMENT_ID);
        execute(job(jobQuery().messages().activityId(FlowNodes.EventResendEveryDay.ELEMENT_ID), instance));
        execute(job(FlowNodes.ServiceTaskReSendConfirmationMail.ELEMENT_ID, instance));
        completeConfirmationTask(instance);

        // then
        assertThat(instance)
                .isEnded()
                .hasPassed(
                        FlowNodes.ServiceTaskReSendConfirmationMail.ELEMENT_ID,
                        FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID,
                        FlowNodes.EndEventMembershipActivated.ELEMENT_ID);
        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);
        verify(reSendConfirmationMailUseCase, times(1)).reSendConfirmationMail(id);
    }
}
