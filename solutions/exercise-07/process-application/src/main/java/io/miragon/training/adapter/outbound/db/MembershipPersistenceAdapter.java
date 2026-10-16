package io.miragon.training.adapter.outbound.db;

import io.miragon.training.application.port.outbound.MembershipRepository;
import io.miragon.training.domain.Membership;
import io.miragon.training.domain.MembershipId;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
public class MembershipPersistenceAdapter implements MembershipRepository {

    private final MembershipJpaRepository jpaRepository;
    private final MembershipEntityMapper mapper;

    public MembershipPersistenceAdapter(MembershipJpaRepository jpaRepository, MembershipEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Membership find(MembershipId id) {
        var entity = jpaRepository.findById(id.value())
                .orElseThrow(() -> new NoSuchElementException("Membership not found: " + id.value()));
        return mapper.toDomain(entity);
    }

    @Override
    public Membership save(Membership membership) {
        jpaRepository.save(mapper.toEntity(membership));
        return membership;
    }
}
