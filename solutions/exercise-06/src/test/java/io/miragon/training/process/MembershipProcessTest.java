package io.miragon.training.process;

import io.miragon.bpmn.runtime.path.PathWalk;
import io.miragon.training.adapter.process.HandleRejectionProcessApi;
import io.miragon.training.adapter.process.MembershipProcessApi;
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

import static io.miragon.training.process.util.ProcessEngineTestUtils.findProcessInstance;
import static io.miragon.training.process.util.ProcessEngineTestUtils.fireTimer;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.calledProcessInstance;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the membership process at the "call activity & DMN" stage (exercise 6).
 *
 * <p>The decline handling is extracted into the {@code handleRejection} call activity, which uses
 * the {@code categorizeApplicant} DMN to route high-value applicants (age 21–29) through a
 * "write regret mail" user task before the claim is compensated. The happy path still forks into
 * the welcome mail and the "Notify community" in-engine delegate before activation.
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

    @AfterEach
    void tearDown() {
        runtimeService.createProcessInstanceQuery().list()
                .forEach(pi -> runtimeService.deleteProcessInstance(pi.getId(), "test cleanup"));
    }

    private ProcessInstance startWaitingAtConfirmation(MembershipId id, int age) {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(true);
        membershipProcess.startProcess(new Membership(id, new Email("user@example.com"), new Name("User"), new Age(age)));
        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        execute(job(MembershipProcessApi.FlowNodes.StartEventSubmitRegistration.ELEMENT_ID, instance));
        execute(job(MembershipProcessApi.FlowNodes.ServiceTaskSendConfirmationMail.ELEMENT_ID, instance));
        return instance;
    }

    @Test
    void happyPath_membershipActivated() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 30);

        // when
        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
        taskService.complete(taskId);
        execute(job(MembershipProcessApi.FlowNodes.UserTaskConfirmMembership.ELEMENT_ID, instance));
        execute(job(MembershipProcessApi.FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, instance));
        execute(job(MembershipProcessApi.FlowNodes.ServiceTaskNotifyCommunity.ELEMENT_ID, instance));

        // then
        // The subprocess is a scope bracket: onto() steps onto it without recording it, inside() walks
        // its interior and resumes on the subprocess, so the step to the fork is checked again.
        var happyPath = PathWalk.from(MembershipProcessApi.FlowNodes.startEventSubmitRegistration())
                .then(next -> next.serviceTaskClaimMembership())
                .then(next -> next.gatewayHasEmptySpots())
                .onto(next -> next.subProcessConfirmMembership())
                .inside(MembershipProcessApi.FlowNodes.subProcessConfirmMembership(), start -> PathWalk.from(start.startEventConfirmationRequired())
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
                        MembershipProcessApi.FlowNodes.SubProcessConfirmMembership.ELEMENT_ID,
                        MembershipProcessApi.FlowNodes.ServiceTaskNotifyCommunity.ELEMENT_ID)
                .hasNotPassed(MembershipProcessApi.FlowNodes.CallActivityHandleRejection.ELEMENT_ID, MembershipProcessApi.FlowNodes.EndEventMembershipDeclined.ELEMENT_ID);

        verify(sendWelcomeMailUseCase, times(1)).sendWelcomeMail(id);
        verify(notifyCommunityUseCase, times(1)).notifyCommunity(id);
        org.assertj.core.api.Assertions.assertThat(runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(MembershipProcessApi.PROCESS_ID.getValue()).count()).isEqualTo(0L);
    }

    @Test
    void noCapacity_membershipIsRejected() {
        // given
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);
        MembershipId id = new MembershipId();
        membershipProcess.startProcess(new Membership(id, new Email("jack@example.com"), new Name("Jack"), new Age(40)));
        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());

        // when
        execute(job(MembershipProcessApi.FlowNodes.StartEventSubmitRegistration.ELEMENT_ID, instance));
        execute(job(MembershipProcessApi.FlowNodes.ServiceTaskSendRejectionMail.ELEMENT_ID, instance));

        // then
        var rejectionPath = PathWalk.from(MembershipProcessApi.FlowNodes.startEventSubmitRegistration())
                .then(next -> next.serviceTaskClaimMembership())
                .then(next -> next.gatewayHasEmptySpots())
                .then(next -> next.serviceTaskSendRejectionMail())
                .end(next -> next.endEventMembershipRejected());

        assertThat(instance)
                .isEnded()
                .hasPassedInOrder(rejectionPath.getIds())
                .hasNotPassed(MembershipProcessApi.FlowNodes.CallActivityHandleRejection.ELEMENT_ID, MembershipProcessApi.FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID);

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
    }

    @Test
    void abortTimer_lowValueApplicant_callActivityAcceptsRejectionAndCompensates() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 40); // age 40 -> DMN: not high value

        // when
        fireTimer(processEngine, MembershipProcessApi.FlowNodes.EventAbortAfter3HalfDays.ELEMENT_ID);
        execute(job(jobQuery().messages().activityId(MembershipProcessApi.FlowNodes.EventAbortAfter3HalfDays.ELEMENT_ID), instance));

        // then
        // Asserted unordered: the engine orders passed activities by end time, and the compensation
        // handler finishes before the end event that throws the compensation.
        var abortPath = PathWalk.from(MembershipProcessApi.FlowNodes.userTaskConfirmMembership())
                .interruptedBy(MembershipProcessApi.FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventAbortAfter3HalfDays())
                .then(next -> next.gatewayDeclineJoin())
                .then(next -> next.callActivityHandleRejection())
                .end(next -> next.endEventMembershipDeclined())
                .throwingCompensation(MembershipProcessApi.FlowNodes.eventCompensateClaim(), boundary -> boundary.serviceTaskRevokeClaim());

        assertThat(instance)
                .isEnded()
                .hasPassed(abortPath.getIds())
                .hasNotPassed(MembershipProcessApi.FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, MembershipProcessApi.FlowNodes.EndEventMembershipActivated.ELEMENT_ID);

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void rejectMessage_highValueApplicant_callActivityAsksForRegretMailThenCompensates() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 25); // age 25 -> DMN: high value

        // when
        membershipProcess.rejectMembership(id);
        execute(job(MembershipProcessApi.FlowNodes.EventConfirmationRejected.ELEMENT_ID, instance));

        // the called handleRejection instance now waits for the regret mail to be written
        String regretTaskId = taskService.createTaskQuery()
                .taskDefinitionKey(HandleRejectionProcessApi.FlowNodes.UserTaskWriteRegretMail.ELEMENT_ID)
                .singleResult()
                .getId();
        taskService.complete(regretTaskId);
        execute(job(HandleRejectionProcessApi.FlowNodes.UserTaskWriteRegretMail.ELEMENT_ID, calledProcessInstance(instance)));

        // then
        var rejectPath = PathWalk.from(MembershipProcessApi.FlowNodes.userTaskConfirmMembership())
                .interruptedBy(MembershipProcessApi.FlowNodes.subProcessConfirmMembership(), boundary -> boundary.eventConfirmationRejected())
                .then(next -> next.gatewayDeclineJoin())
                .then(next -> next.callActivityHandleRejection())
                .end(next -> next.endEventMembershipDeclined())
                .throwingCompensation(MembershipProcessApi.FlowNodes.eventCompensateClaim(), boundary -> boundary.serviceTaskRevokeClaim());

        assertThat(instance)
                .isEnded()
                .hasPassed(rejectPath.getIds());

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void resendTimer_nonInterrupting_resendsConfirmationMailAndKeepsWaiting() {
        // given
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 30);

        // when
        fireTimer(processEngine, MembershipProcessApi.FlowNodes.EventResendEveryDay.ELEMENT_ID);
        execute(job(jobQuery().messages().activityId(MembershipProcessApi.FlowNodes.EventResendEveryDay.ELEMENT_ID), instance));
        execute(job(MembershipProcessApi.FlowNodes.ServiceTaskReSendConfirmationMail.ELEMENT_ID, instance));
        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
        taskService.complete(taskId);
        execute(job(MembershipProcessApi.FlowNodes.UserTaskConfirmMembership.ELEMENT_ID, instance));
        execute(job(MembershipProcessApi.FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, instance));
        execute(job(MembershipProcessApi.FlowNodes.ServiceTaskNotifyCommunity.ELEMENT_ID, instance));

        // then
        assertThat(instance)
                .isEnded()
                .hasPassed(
                        MembershipProcessApi.FlowNodes.ServiceTaskReSendConfirmationMail.ELEMENT_ID,
                        MembershipProcessApi.FlowNodes.EndEventMembershipActivated.ELEMENT_ID);
        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);
        verify(reSendConfirmationMailUseCase, times(1)).reSendConfirmationMail(id);
    }
}
