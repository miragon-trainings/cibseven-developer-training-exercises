package io.miragon.training.adapter.outbound.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "membership")
public class MembershipEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "age", nullable = false)
    private int age;

    protected MembershipEntity() {
    }

    public MembershipEntity(UUID id, String email, String name, int age) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.age = age;
    }

    public UUID getId() { return id; }

    public String getEmail() { return email; }

    public String getName() { return name; }

    public int getAge() { return age; }
}
