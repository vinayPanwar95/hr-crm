package com.fms.hr_crm.lead.repository;

import com.fms.hr_crm.lead.model.entity.Lead;
import com.fms.hr_crm.lead.model.entity.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    // Tenant-scoped finders — ALWAYS filter by tenantId
    Page<Lead> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Lead> findByTenantIdAndStatus(UUID tenantId, LeadStatus status, Pageable pageable);

    Page<Lead> findByTenantIdAndRecruiterId(UUID tenantId, UUID recruiterId, Pageable pageable);

    // Duplicate detection
    Optional<Lead> findByTenantIdAndPhone(UUID tenantId, String phone);
    Optional<Lead> findByTenantIdAndEmail(UUID tenantId, String email);

    // Pipeline stats — used for dashboard
    @Query("""
        SELECT l.status, COUNT(l)
        FROM Lead l
        WHERE l.tenantId = :tenantId
        GROUP BY l.status
        """)
    List<Object[]> countByStatusForTenant(UUID tenantId);

    long countByTenantIdAndRecruiterId(UUID tenantId, UUID recruiterId);

    /** Returns just the IDs of leads matching a status — used by AI campaign queue. */
    @Query("SELECT l.id FROM Lead l WHERE l.tenantId = :tenantId AND l.status = :status ORDER BY l.createdAt ASC")
    List<UUID> findIdsByTenantIdAndStatus(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("status") LeadStatus status,
            org.springframework.data.domain.Pageable pageable);

    // Recruiter's leads for today's work
    @Query("""
        SELECT l FROM Lead l
        WHERE l.tenantId = :tenantId
          AND l.recruiterId = :recruiterId
          AND l.status NOT IN ('CONVERTED', 'CLOSED')
        ORDER BY l.aiScore DESC NULLS LAST, l.createdAt ASC
        """)
    List<Lead> findActiveLeadsForRecruiter(UUID tenantId, UUID recruiterId);
}