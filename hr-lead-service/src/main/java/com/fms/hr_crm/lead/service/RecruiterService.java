package com.fms.hr_crm.lead.service;

import com.fms.hr_crm.lead.client.CallingServiceClient;
import com.fms.hr_crm.lead.client.ProvisionRecruiterRequest;
import com.fms.hr_crm.lead.model.dto.RecruiterRequest;
import com.fms.hr_crm.lead.model.dto.RecruiterResponse;
import com.fms.hr_crm.lead.model.entity.Recruiter;
import com.fms.hr_crm.lead.repository.LeadRepository;
import com.fms.hr_crm.lead.repository.RecruiterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for recruiter onboarding and retrieval.
 * Recruiters are the users who handle leads in the pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final LeadRepository       leadRepository;
    private final CallingServiceClient callingServiceClient;

    /**
     * Onboards a new recruiter under the given tenant.
     */
    @Transactional
    public RecruiterResponse create(RecruiterRequest request, UUID tenantId) {
        log.info("Onboarding recruiter '{}' for tenant {}", request.name(), tenantId);
        var recruiter = Recruiter.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .active(true)
                .build();
        recruiter.setTenantId(tenantId);
        var savedRecruiter = recruiterRepository.save(recruiter);
        log.debug("Recruiter created with id={}", savedRecruiter.getId());

        // Provision login account in hr-calling-service with the one-time password
        try {
            var username = deriveUsername(request.email(), request.name());
            var provision = new ProvisionRecruiterRequest(
                    savedRecruiter.getId(), username, request.email(),
                    request.name(), request.loginPassword(), tenantId);
            log.info("Provisioning calling-service login for recruiterId={} username={}", savedRecruiter.getId(), username);
            callingServiceClient.provisionRecruiter(provision);
            log.info("Calling-service login provisioned for recruiterId={} username={}", savedRecruiter.getId(), username);
        } catch (Exception e) {
            // Non-fatal: recruiter is created in lead-service; admin can retry provisioning
            log.warn("Failed to provision calling-service login for recruiterId={}: {}", savedRecruiter.getId(), e.getMessage());
        }

        return toResponse(savedRecruiter, tenantId);
    }

    /**
     * Derives a calling-service username from email (before @) or from name.
     * Examples: "rahul@company.com" → "rahul", "Rahul Mehta" → "rahul.mehta"
     */
    private String deriveUsername(String email, String name) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@')).toLowerCase();
        }
        return name.toLowerCase().replaceAll("[^a-z0-9]", ".").replaceAll("\\.{2,}", ".");
    }

    /**
     * Returns paginated list of all recruiters for the tenant.
     */
    public Page<RecruiterResponse> findAll(UUID tenantId, Pageable pageable) {
        log.debug("Fetching recruiters for tenant {}", tenantId);
        return recruiterRepository.findByTenantId(tenantId, pageable)
                .map(r -> toResponse(r, tenantId));
    }

    /**
     * Searches active recruiters by name fragment — used by the Add Lead autocomplete.
     */
    public List<RecruiterResponse> searchByName(UUID tenantId, String name) {
        log.debug("Autocomplete search name='{}' tenant={}", name, tenantId);
        return recruiterRepository
                .findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(tenantId, name)
                .stream()
                .map(r -> toResponse(r, tenantId))
                .toList();
    }

    /**
     * Deactivates a recruiter so they no longer appear in new-lead assignment.
     */
    @Transactional
    public void deactivate(UUID id, UUID tenantId) {
        log.info("Deactivating recruiter id={} tenant={}", id, tenantId);
        recruiterRepository.findById(id).ifPresent(r -> {
            if (r.getTenantId().equals(tenantId)) {
                r.setActive(false);
                recruiterRepository.save(r);
            }
        });
    }

    private RecruiterResponse toResponse(Recruiter r, UUID tenantId) {
        var leadsAssigned = leadRepository.countByTenantIdAndRecruiterId(tenantId, r.getId());
        return new RecruiterResponse(
                r.getId(), r.getName(), r.getEmail(), r.getPhone(), r.isActive(), leadsAssigned);
    }
}