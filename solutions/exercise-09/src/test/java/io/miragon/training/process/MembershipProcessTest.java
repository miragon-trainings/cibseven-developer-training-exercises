package io.miragon.training.process;

import io.miragon.training.adapter.process.HandleRejectionProcessApi;
import io.miragon.training.adapter.process.SubscribeNewsletterProcessApi;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the membership process at the "call activity & DMN" stage (exercise 8).
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
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());
        assertThat(instance).isWaitingAt(SubscribeNewsletterProcessApi.FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);
        return instance;
    }

    @Test
    void happyPath_membershipActivated() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 30);

        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        // After confirmation the flow forks: Send Welcome Mail and Notify community both run in
        // parallel as in-engine delegates, so the parallel join fires on its own (the external-task
        // /remote-worker variant is introduced later, in exercise 9).

        assertThat(instance)
                .isEnded()
                .hasPassed(SubscribeNewsletterProcessApi.FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.ServiceTaskNotifyCommunity.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.EndEventMembershipActivated.ELEMENT_ID)
                .hasNotPassed(SubscribeNewsletterProcessApi.FlowNodes.CallActivityHandleRejection.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.EndEventMembershipDeclined.ELEMENT_ID);

        verify(sendWelcomeMailUseCase, times(1)).sendWelcomeMail(id);
        verify(notifyCommunityUseCase, times(1)).notifyCommunity(id);
        org.assertj.core.api.Assertions.assertThat(runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SubscribeNewsletterProcessApi.PROCESS_ID.getValue()).count()).isEqualTo(0L);
    }

    @Test
    void noCapacity_membershipIsRejected() {
        when(claimMembershipUseCase.claimMembership(any())).thenReturn(false);
        MembershipId id = new MembershipId();
        membershipProcess.startProcess(new Membership(id, new Email("jack@example.com"), new Name("Jack"), new Age(40)));

        ProcessInstance instance = findProcessInstance(runtimeService, id.value().toString());
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance)
                .isEnded()
                .hasPassed(SubscribeNewsletterProcessApi.FlowNodes.ServiceTaskSendRejectionMail.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.EndEventMembershipRejected.ELEMENT_ID)
                .hasNotPassed(SubscribeNewsletterProcessApi.FlowNodes.CallActivityHandleRejection.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID);

        verify(sendRejectionMailUseCase).sendRejectionMail(id);
    }

    @Test
    void abortTimer_lowValueApplicant_callActivityAcceptsRejectionAndCompensates() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 40); // age 40 -> DMN: not high value

        fireTimer(processEngine, SubscribeNewsletterProcessApi.FlowNodes.EventAbortAfter3HalfDays.ELEMENT_ID);
        continueToNextWaitState(processEngine);

        assertThat(instance)
                .isEnded()
                .hasPassed(SubscribeNewsletterProcessApi.FlowNodes.CallActivityHandleRejection.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.ServiceTaskRevokeClaim.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.EndEventMembershipDeclined.ELEMENT_ID)
                .hasNotPassed(SubscribeNewsletterProcessApi.FlowNodes.ServiceTaskSendWelcomeMail.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.EndEventMembershipActivated.ELEMENT_ID);

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void rejectMessage_highValueApplicant_callActivityAsksForRegretMailThenCompensates() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 25); // age 25 -> DMN: high value

        membershipProcess.rejectMembership(id);
        continueToNextWaitState(processEngine);

        // the called handleRejection instance now waits for the regret mail to be written
        String regretTaskId = taskService.createTaskQuery()
                .taskDefinitionKey(HandleRejectionProcessApi.FlowNodes.UserTaskWriteRegretMail.ELEMENT_ID)
                .singleResult()
                .getId();
        taskService.complete(regretTaskId);
        continueToNextWaitState(processEngine);

        assertThat(instance)
                .isEnded()
                .hasPassed(SubscribeNewsletterProcessApi.FlowNodes.CallActivityHandleRejection.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.ServiceTaskRevokeClaim.ELEMENT_ID, SubscribeNewsletterProcessApi.FlowNodes.EndEventMembershipDeclined.ELEMENT_ID);

        verify(revokeClaimUseCase, times(1)).revokeClaim(id);
    }

    @Test
    void resendTimer_nonInterrupting_resendsConfirmationMailAndKeepsWaiting() {
        MembershipId id = new MembershipId();
        ProcessInstance instance = startWaitingAtConfirmation(id, 30);
        verify(sendConfirmationMailUseCase, times(1)).sendConfirmationMail(id);

        fireTimer(processEngine, SubscribeNewsletterProcessApi.FlowNodes.EventResendEveryDay.ELEMENT_ID);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());

        assertThat(instance).isWaitingAt(SubscribeNewsletterProcessApi.FlowNodes.UserTaskConfirmMembership.ELEMENT_ID);
        verify(reSendConfirmationMailUseCase, times(1)).reSendConfirmationMail(id);

        String taskId = taskService.createTaskQuery()
                .processInstanceId(instance.getProcessInstanceId()).singleResult().getId();
        taskService.complete(taskId);
        continueToNextWaitState(processEngine, instance.getProcessInstanceId());
        assertThat(instance).isEnded().hasPassed(SubscribeNewsletterProcessApi.FlowNodes.EndEventMembershipActivated.ELEMENT_ID);
    }
}
