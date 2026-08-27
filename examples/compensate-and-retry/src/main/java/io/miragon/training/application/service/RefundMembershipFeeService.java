package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.RefundMembershipFeeUseCase;
import io.miragon.training.application.port.outbound.PaymentLedger;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compensation handler for {@link ChargeMembershipFeeService}: refunds the fee. Because compensation
 * runs in reverse completion order, this runs BEFORE the welcome-kit reservation is cancelled.
 */
@Service
@Transactional
public class RefundMembershipFeeService implements RefundMembershipFeeUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefundMembershipFeeService.class);

    private final PaymentLedger ledger;

    public RefundMembershipFeeService(PaymentLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    public void refundMembershipFee(MembershipId membershipId) {
        ledger.refund(membershipId);
        log.info("Compensation: refunded membership fee for {}", membershipId.value());
    }
}
