package io.miragon.training.adapter.outbound.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MembershipJpaRepository extends JpaRepository<MembershipEntity, UUID> {
}
