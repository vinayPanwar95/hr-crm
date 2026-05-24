package com.fms.hr_crm.calling.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * A recruiter (or admin) who can log in to the calling service.
 * Passwords are stored as BCrypt hashes — never plain text.
 */
@Entity
@Table(
    name = "recruiter_users",
    indexes = {
        @Index(name = "idx_ru_username", columnList = "username", unique = true),
        @Index(name = "idx_ru_email",    columnList = "email",    unique = true),
        @Index(name = "idx_ru_tenant",   columnList = "tenant_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class RecruiterUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    /** BCrypt hash — never log or expose. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruiterRole role = RecruiterRole.RECRUITER;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Forces the recruiter to change their password on next login.
     * Set to {@code true} when an admin provisions the account with a temporary password.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}