package com.fms.hr_crm.calling.repository;

import com.fms.hr_crm.calling.model.entity.RecruiterUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecruiterUserRepository extends JpaRepository<RecruiterUser, UUID> {

    Optional<RecruiterUser> findByUsername(String username);

    Optional<RecruiterUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}