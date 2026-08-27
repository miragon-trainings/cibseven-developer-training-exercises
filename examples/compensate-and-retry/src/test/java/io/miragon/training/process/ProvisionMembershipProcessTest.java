package io.miragon.training.process;

import io.miragon.training.application.port.inbound.AssignMembershipNumberUseCase;
import io.miragon.training.application.port.inbound.CancelWelcomeKitReservationUseCase;
import io.miragon.training.application.port.inbound.ChargeMembershipFeeUseCase;
import io.miragon.training.application.port.inbound.CreateMemberProfileUseCase;
import io.miragon.training.application.port.inbound.RefundMembershipFeeUseCase;
import io.miragon.training.application.port.inbound.ReserveWelcomeKitUseCase;
import io.miragon.training.application.port.inbound.VerifyProvisioningUseCase;
import io.miragon.training.application.port.outbound.ProvisioningProcess;
import io.miragon.training.domain.MembershipId;
import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.Job;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Process test for the compensate-then-retry pattern. Drives the instance to completion and asserts
 * that exactly one rollback + redo happened: the two compensable tasks run twice, each compensation
 * handler runs once, the two plain steps run once, and the instance ends "Membership active".
 */
@SpringBootTest
@ActiveProfiles("test")
class ProvisionMembershipProcessTest {

    private static final String PROCESS_KEY = "provisionMembership";

    @Autowired
    private ProvisioningProcess provisioningProcess;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ProcessEngine processEngine;

    @MockitoBean
    private CreateMemberProfileUseCase createMemberProfileUseCase;

    @MockitoBean
    private AssignMembershipNumberUseCase assignMembershipNumberUseCase;

    @MockitoBean
    private ReserveWelcomeKitUseCase reserveWelcomeKitUseCase;

    @MockitoBean
    private CancelWelcomeKitReservationUseCase cancelWelcomeKitReservationUseCase;

    @MockitoBean
    private ChargeMembershipFeeUseCase chargeMembershipFeeUseCase;

    @MockitoBean
    private RefundMembershipFeeUseCase refundMembershipFeeUseCase;

    @MockitoBean
    private VerifyProvisioningUseCase verifyProvisioningUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine);
        when(assignMembershipNumberUseCase.assignMembershipNumber(any())).thenReturn("IC-TEST0001");
        // Fail the first attempt, succeed from the second -> exactly one rollback + redo.
        when(verifyProvisioningUseCase.verify(any(), anyInt()))
                .thenAnswer(invocation -> (int) invocation.getArgument(1) >= 2);
    }

    @Test
    void failedValidation_rollsBackCommittedStepsAndRedoesThem() {
        MembershipId id = new MembershipId();
        provisioningProcess.start(id, "neo@miravelo.io", "Neo", 33);

        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(id.value().toString())
                .singleResult();

        executeAllJobs(instance.getProcessInstanceId());

        assertThat(instance)
                .isEnded()
                .hasPassed(
                        "serviceTask_createMemberProfile",
                        "serviceTask_assignMembershipNumber",
                        "serviceTask_reserveWelcomeKit",
                        "serviceTask_chargeMembershipFee",
                        "serviceTask_verifyProvisioning",
                        "serviceTask_refundMembershipFee",
                        "serviceTask_cancelWelcomeKitReservation",
                        "endEvent_membershipActive");

        // Plain steps run once and are never rolled back.
        verify(createMemberProfileUseCase, times(1)).createMemberProfile(id);
        verify(assignMembershipNumberUseCase, times(1)).assignMembershipNumber(id);
        // Compensable steps run twice (once, rolled back, then redone).
        verify(reserveWelcomeKitUseCase, times(2)).reserveWelcomeKit(id);
        verify(chargeMembershipFeeUseCase, times(2)).chargeMembershipFee(id);
        verify(verifyProvisioningUseCase, times(2)).verify(any(), anyInt());
        // Each compensation handler runs exactly once.
        verify(refundMembershipFeeUseCase, times(1)).refundMembershipFee(id);
        verify(cancelWelcomeKitReservationUseCase, times(1)).cancelWelcomeKitReservation(id);
    }

    /**
     * Executes pending async-continuation jobs of a single instance until it reaches its next wait
     * state (here: the end). The job executor is disabled in the test profile, so nothing runs on its
     * own — we push the process forward from the test thread, keeping timing under our control.
     */
    private void executeAllJobs(String processInstanceId) {
        ManagementService managementService = processEngine.getManagementService();
        for (int i = 0; i < 50; i++) {
            Job job = managementService.createJobQuery().active().messages()
                    .processInstanceId(processInstanceId)
                    .listPage(0, 1).stream().findFirst().orElse(null);
            if (job == null) {
                return;
            }
            managementService.executeJob(job.getId());
        }
    }
}
