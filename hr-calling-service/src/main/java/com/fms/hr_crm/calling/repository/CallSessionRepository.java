package com.fms.hr_crm.calling.repository;

import com.fms.hr_crm.calling.model.entity.CallSession;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {

    Page<CallSession> findByTenantId(UUID tenantId, Pageable pageable);

    Page<CallSession> findByTenantIdAndRecruiterId(UUID tenantId, UUID recruiterId, Pageable pageable);

    Page<CallSession> findByTenantIdAndLeadId(UUID tenantId, UUID leadId, Pageable pageable);

    Page<CallSession> findByTenantIdAndStatus(UUID tenantId, CallStatus status, Pageable pageable);

    Optional<CallSession> findByTwilioCallSid(String twilioCallSid);

    long countByTenantIdAndStatus(UUID tenantId, CallStatus status);

    Page<CallSession> findByCampaignId(UUID campaignId, Pageable pageable);

    long countByCampaignId(UUID campaignId);
}