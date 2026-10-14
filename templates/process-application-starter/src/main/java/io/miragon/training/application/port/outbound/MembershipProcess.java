package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;

public interface MembershipProcess {

    void startProcess(Membership membership);

    void confirm(MembershipId membershipId);
}
