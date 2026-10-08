package io.miragon.training.process;

import io.miragon.bpmn.runtime.path.PathWalk;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi.FlowNodes;
import io.miragon.training.application.port.inbound.ClaimMembershipUseCase;
import io.miragon.training.application.port.inbound.NotifyCommunityUseCase;
import io.miragon.training.application.port.inbound.ReSendConfirmationMailUseCase;
import io.miragon.training.application.port.inbound.RevokeClaimUseCase;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.miragon.training.process.util.ProcessEngineTestUtils.continueToNextWaitState;
import static io.miragon.training.process.util.ProcessEngineTestUtils.findProcessInstance;
import static io.miragon.training.process.util.ProcessEngineTestUtils.fireTimer;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the membership process at the "compensation" stage (exercise 7) — revokeClaim now
 * runs as a compensation handler (SAGA-style rollback) instead of an explicit service task.
 *
 * <p>On the happy path the flow forks after confirmation: the welcome mail and the "Notify community"
 * task both run in parallel as in-engine delegates, so the parallel join fires on its own (the
 * external-task/remote-worker variant is introduced later, in exercise 9).
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

    /**
     * Clean up any instances a test left running (e.g. one still parked at a wait state), so the
     * running-instance counts stay isolated between test methods.
     */
    @AfterEach
    void tearDown() {
        runtimeService.createProcessInstanceQuery().list()
                .forEach(pi -> runtimeService.deleteProcessInstance(pi.getId(), "test cleanup"));
    }

    private ProcessInstance startWaitingAtConfirmation(MembershipId id, String email, String name) {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);
        membershipProcess.startProcess(new Membership(id, new Email(email), new Name(name), new Age(30)));
        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());
        assertThat(instance).isWaitingAt(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);
        return instance;
    }

    private void completeConfirmationTask(ProcessInstance instance) {
        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId())
                .singleResult()
                .getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        // After confirmation the flow forks: Send Welcome Mail and Notify community both run in
        // parallel as in-engine delegates, so they execute synchronously and the parallel join fires
        // on its own — no external worker to stand in for (that comes in exercise 9).
    }

    private long runningInstanceCount() {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SubscribeNewsletterProcessApi.PROCESS_ID.getValue())
                .count();
    }

    @Test
    void happyPath_membershipActivated() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "jane@example.com", "Jane");

        completeConfirmationTask(instance);

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
                .hasNotPassed(FlowNodes.ServiceTaskRevokeClaim.ELEMENT_ID, FlowNodes.EndEventMembershipDeclined.ELEMENT_ID);

        verify(sendWelcomeMailUseCase, times(1)).sendWelcomeMail(id);
        verify(notifyCommunityUseCase, times(1)).notifyCommunity(id);
        // the instance fully completed — nothing left running
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(0L);
    }

    @Test
    void noCapacity_membershipIsRejected() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);
        MembershipId id = new MembershipId();
        membershipProcess.startProcess(new Membership(id, new Email("jack@example.com"), new Name("Jack"), new Age(40)));

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        var rejectionPath = PathWalk.from(FlowNodes.startEventSubmitRegistration())
                .then(next -> next.serviceTaskClaimMembership())
                .then(next -> next.gatewayHasEmptySpots())
                .then(next -> next.serviceTaskSendRejectionMail())
                .end(next -> next.endEventMembershipRejected());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(rejectionPath.getIds())
                .hasNotPassed(FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, FlowNodes.EndEventMembershipActivated.ELEMENT_ID);

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(0L);
    }

    @Test
    void abortTimer_interruptsSubprocessAndRevokesClaim() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "amy@example.com", "Amy");

        fireTimer(processEngine, FlowNodes.EventAbortAfter3HalfDays.ELEMENT_ID);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        // Asserted unordered: the engine orders passed activities by end time, and the compensation
        // handler finishes before the end event that throws the compensation.
        var abortPath = PathWalk.from(FlowNodes.userTaskConfirmMembership())
                .interruptedBy(FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventAbortAfter3HalfDays())
                .end(next -> next.endEventMembershipDeclined())
                .throwingCompensation(FlowNodes.eventCompensateClaim(), boundary -> boundary.serviceTaskRevokeClaim());

        assertThat(instance)
                .isEnded()
                .hasPassed(abortPath.getIds())
                .hasNotPassed(FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, FlowNodes.EndEventMembershipActivated.ELEMENT_ID);

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
        org.assertj.core.api.Assertions.assertThat(runningInstanceCount()).isEqualTo(0L);
    }

    @Test
    void rejectMessage_interruptsSubprocessAndRevokesClaim() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "ben@example.com", "Ben");

        membershipProcess.rejectMembership(id);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        var rejectPath = PathWalk.from(FlowNodes.userTaskConfirmMembership())
                .interruptedBy(FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventConfirmationRejected())
                .end(next -> next.endEventMembershipDeclined())
                .throwingCompensation(FlowNodes.eventCompensateClaim(), boundary -> boundary.serviceTaskRevokeClaim());

        assertThat(instance)
                .isEnded()
                .hasPassed(rejectPath.getIds())
                .hasNotPassed(FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, FlowNodes.EndEventMembershipActivated.ELEMENT_ID);

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void resendTimer_nonInterrupting_resendsConfirmationMailAndKeepsWaiting() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, "cara@example.com", "Cara");
        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);

        fireTimer(processEngine, FlowNodes.EventResendEveryDay.ELEMENT_ID);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance).isWaitingAt(FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);
        verify(reSendConfirmationMailUseCase, times(1)).reSendConfirmationMail(id);

        completeConfirmationTask(instance);
        assertThat(instance).isEnded().hasPassed(FlowNodes.EndEventMembershipActivated.ELEMENT_ID);
    }
}
