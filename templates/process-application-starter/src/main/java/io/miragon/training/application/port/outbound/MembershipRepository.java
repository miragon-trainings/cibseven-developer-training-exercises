package io.miragon.training.application.port.outbound;

import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;

public interface MembershipRepository {

    Membership find(MembershipId id);

    Membership save(Membership membership);
}
