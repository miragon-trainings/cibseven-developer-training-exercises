package io.miragon.training.application.service;

import io.miragon.training.application.port.inbound.ChargeMembershipFeeUseCase;
import io.miragon.training.application.port.outbound.PaymentLedger;
import io.miragon.training.domain.MembershipId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compensable step B: charge the membership fee. The charge is what the compensation handler
 * ({@link RefundMembershipFeeService}) refunds.
 */
@Service
@Transactional
public class ChargeMembershipFeeService implements ChargeMembershipFeeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChargeMembershipFeeService.class);

    private final PaymentLedger ledger;

    public ChargeMembershipFeeService(PaymentLedger ledger) {
        this.ledger = ledger;
    }

    @Override
    public void chargeMembershipFee(MembershipId membershipId) {
        ledger.charge(membershipId);
        log.info("Charged membership fee for {}", membershipId.value());
    }
}
