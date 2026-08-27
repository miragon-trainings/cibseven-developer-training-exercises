package io.miragon.training.application.port.inbound;

import io.miragon.training.domain.MembershipId;

public interface RefundMembershipFeeUseCase {

    void refundMembershipFee(MembershipId membershipId);
}
