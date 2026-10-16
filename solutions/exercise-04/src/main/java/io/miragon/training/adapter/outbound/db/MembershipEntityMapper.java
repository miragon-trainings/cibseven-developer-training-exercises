package io.miragon.training.adapter.outbound.db;

import io.miragon.training.domain.Age;
import io.miragon.training.domain.Email;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import io.miragon.training.domain.Name;
import org.springframework.stereotype.Component;

@Component
public class MembershipEntityMapper {

    public MembershipEntity toEntity(Membership membership) {
        return new MembershipEntity(
                membership.id().value(),
                membership.email().value(),
                membership.name().value(),
                membership.age().value()
        );
    }

    public Membership toDomain(MembershipEntity entity) {
        return new Membership(
                new MembershipId(entity.getId()),
                new Email(entity.getEmail()),
                new Name(entity.getName()),
                new Age(entity.getAge())
        );
    }
}
