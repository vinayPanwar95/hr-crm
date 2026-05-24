package com.fms.hr_crm.calling.client;

import com.fms.hr_crm.calling.model.dto.LeadSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Facade over {@link LeadServiceFeignClient} that provides fallback behaviour
 * when hr-lead-service is unreachable.
 *
 * <p><strong>Security:</strong> {@link #getLeadPhone} returns a real phone number.
 * Callers MUST immediately pass the result to MaskingService and MUST NOT log it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeadServiceClient {

    private final LeadServiceFeignClient feign;

    /**
     * Returns the real phone number for a lead. Do NOT log the return value.
     * Falls back to a Twilio test number when lead-service is unreachable.
     */
    public String getLeadPhone(UUID leadId, UUID tenantId) {
        log.debug("getLeadPhone() — leadId={}, tenantId={}", leadId, tenantId);
        try {
            return feign.getLeadPhone(leadId, tenantId);
        } catch (RestClientException e) {
            log.warn("getLeadPhone() — lead-service unreachable for leadId={}: {} — using placeholder",
                    leadId, e.getMessage());
            return "+15005550006";   // Twilio test number for local dev
        }
    }

    /**
     * Returns a batch of lead IDs for AI campaign dialing.
     * Returns an empty list when lead-service is unreachable.
     */
    public List<UUID> getLeadIdsForCampaign(UUID tenantId, String status, int limit) {
        log.info("getLeadIdsForCampaign() — tenantId={} status={} limit={}", tenantId, status, limit);
        try {
            var ids = feign.getLeadIdsForCampaign(tenantId, status, limit);
            log.info("getLeadIdsForCampaign() — received {} lead IDs", ids.size());
            return ids;
        } catch (RestClientException e) {
            log.warn("getLeadIdsForCampaign() — lead-service unreachable: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Updates a lead's status from an AI campaign IVR response.
     * Silently ignores failures — campaign continues regardless.
     */
    public void updateLeadStatus(UUID leadId, UUID tenantId, String newStatus) {
        log.info("updateLeadStatus() — leadId={} tenantId={} newStatus={}", leadId, tenantId, newStatus);
        try {
            feign.updateLeadStatus(leadId, tenantId, newStatus);
        } catch (RestClientException e) {
            log.warn("updateLeadStatus() — could not update lead {}: {}", leadId, e.getMessage());
        }
    }

    /**
     * Returns lightweight lead summaries assigned to a recruiter.
     * Returns an empty list when lead-service is unreachable.
     */
    public List<LeadSummary> getLeadsForRecruiter(UUID tenantId, UUID recruiterId) {
        log.debug("getLeadsForRecruiter() — tenantId={} recruiterId={}", tenantId, recruiterId);
        try {
            return feign.getLeadsForRecruiter(tenantId, recruiterId);
        } catch (RestClientException e) {
            log.warn("getLeadsForRecruiter() — lead-service unreachable: {}", e.getMessage());
            return List.of();
        }
    }
}