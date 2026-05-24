package com.fms.hr_crm.lead.service;


import com.fms.hr_crm.lead.exception.DuplicateLeadException;
import com.fms.hr_crm.lead.exception.LeadNotFoundException;
import com.fms.hr_crm.lead.mapper.LeadMapper;
import com.fms.hr_crm.lead.model.dto.LeadRequest;
import com.fms.hr_crm.lead.model.dto.LeadResponse;
import com.fms.hr_crm.lead.model.dto.PipelineStats;
import com.fms.hr_crm.lead.model.entity.Lead;
import com.fms.hr_crm.lead.model.entity.LeadStatus;
import com.fms.hr_crm.lead.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)     // default: read-only; write methods override
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;
    private final DuplicateDetector duplicateDetector;
    private final RecruiterAssigner recruiterAssigner;

    @Transactional
    public LeadResponse create(LeadRequest request, UUID tenantId) {
        // 1. Duplicate check before any DB write
        duplicateDetector.assertNoDuplicate(tenantId, request.phone(), request.email());

        // 2. Build entity
        Lead lead = leadMapper.toEntity(request);
        lead.setTenantId(tenantId);
        lead.setStatus(LeadStatus.NEW);

        // 3. Auto-assign recruiter if not specified
        if (lead.getRecruiterId() == null) {
            UUID assignedRecruiter = recruiterAssigner.nextRoundRobin(tenantId);
            lead.setRecruiterId(assignedRecruiter);
        }

        Lead saved = leadRepository.save(lead);
        log.info("Lead created: id={} tenant={}", saved.getId(), tenantId);
        return leadMapper.toResponse(saved);
    }

    public LeadResponse findById(UUID id, UUID tenantId) {
        return leadRepository.findById(id)
                .filter(l -> l.getTenantId().equals(tenantId))  // tenant isolation
                .map(leadMapper::toResponse)
                .orElseThrow(() -> new LeadNotFoundException(id));
    }

    public Page<LeadResponse> findAll(UUID tenantId, LeadStatus status, Pageable pageable) {
        Page<Lead> page = (status != null)
                ? leadRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : leadRepository.findByTenantId(tenantId, pageable);

        return page.map(leadMapper::toResponse);
    }

    @Transactional
    public LeadResponse updateStatus(UUID id, LeadStatus newStatus,
                                     String note, UUID tenantId) {
        Lead lead = requireLead(id, tenantId);
        lead.transitionTo(newStatus);   // business rule enforced in entity

        if (newStatus == LeadStatus.CONTACTED) {
            lead.setLastContactedAt(java.time.Instant.now());
        }

        if (note != null && !note.isBlank()) {
            // add note via LeadNoteService — single responsibility
        }

        return leadMapper.toResponse(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponse assignRecruiter(UUID id, UUID recruiterId, UUID tenantId) {
        Lead lead = requireLead(id, tenantId);
        lead.setRecruiterId(recruiterId);
        return leadMapper.toResponse(leadRepository.save(lead));
    }

    @Transactional
    public void softDelete(UUID id, UUID tenantId) {
        Lead lead = requireLead(id, tenantId);
        lead.setDeletedAt(java.time.Instant.now());
        leadRepository.save(lead);
    }

    public PipelineStats getPipelineStats(UUID tenantId) {
        List<Object[]> rows = leadRepository.countByStatusForTenant(tenantId);
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.toMap(
                        r -> ((LeadStatus) r[0]).name(),
                        r -> (Long) r[1]
                ));
        return new PipelineStats(counts);
    }

    /**
     * Returns a batch of lead IDs in the given status for AI campaign dialing.
     * Called internally by hr-calling-service — not exposed to recruiters.
     */
    public List<UUID> getLeadIdsForCampaign(UUID tenantId, LeadStatus status, int limit) {
        log.debug("getLeadIdsForCampaign() — tenant={} status={} limit={}", tenantId, status, limit);
        return leadRepository.findIdsByTenantIdAndStatus(
                tenantId, status,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    /**
     * Internal use only — returns the real phone number for hr-calling-service.
     * Do NOT call this from any recruiter-facing endpoint.
     * Do NOT log the returned value.
     */
    public Optional<String> getPhoneForInternalUse(UUID id, UUID tenantId) {
        return leadRepository.findById(id)
                .filter(l -> l.getTenantId().equals(tenantId))
                .map(Lead::getPhone);
    }

    // Private guard — ensures tenant isolation on every write
    private Lead requireLead(UUID id, UUID tenantId) {
        return leadRepository.findById(id)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElseThrow(() -> new LeadNotFoundException(id));
    }
}