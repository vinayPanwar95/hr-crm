package com.fms.hr_crm.calling.repository;

import com.fms.hr_crm.calling.model.entity.AiCampaign;
import com.fms.hr_crm.calling.model.entity.CampaignStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiCampaignRepository extends JpaRepository<AiCampaign, UUID> {

    Page<AiCampaign> findByTenantId(UUID tenantId, Pageable pageable);

    /** Used by scheduler to find campaigns that should auto-start. */
    List<AiCampaign> findByStatusIn(List<CampaignStatus> statuses);

    // ── Atomic counter increments — safe for concurrent virtual threads ───────

    @Modifying
    @Transactional
    @Query("UPDATE AiCampaign c SET c.calledCount = c.calledCount + 1 WHERE c.id = :id")
    void incrementCalledCount(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE AiCampaign c SET c.completedCount = c.completedCount + 1 WHERE c.id = :id")
    void incrementCompletedCount(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE AiCampaign c SET c.failedCount = c.failedCount + 1 WHERE c.id = :id")
    void incrementFailedCount(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE AiCampaign c SET c.status = :status, c.startedAt = :startedAt, c.totalLeads = :total WHERE c.id = :id")
    void markRunning(@Param("id") UUID id,
                     @Param("status") CampaignStatus status,
                     @Param("startedAt") Instant startedAt,
                     @Param("total") int total);

    @Modifying
    @Transactional
    @Query("UPDATE AiCampaign c SET c.status = :status, c.stoppedAt = :stoppedAt WHERE c.id = :id")
    void markTerminated(@Param("id") UUID id,
                        @Param("status") CampaignStatus status,
                        @Param("stoppedAt") Instant stoppedAt);
}