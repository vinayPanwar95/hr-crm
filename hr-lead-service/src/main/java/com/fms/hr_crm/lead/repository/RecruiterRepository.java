package com.fms.hr_crm.lead.repository;

import com.fms.hr_crm.lead.model.entity.Recruiter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecruiterRepository extends JpaRepository<Recruiter, UUID> {

    Page<Recruiter> findByTenantId(UUID tenantId, Pageable pageable);

    /** Used for the autocomplete dropdown in the Add Lead form. */
    List<Recruiter> findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(UUID tenantId, String name);

    List<Recruiter> findByTenantIdAndActiveTrue(UUID tenantId);
}