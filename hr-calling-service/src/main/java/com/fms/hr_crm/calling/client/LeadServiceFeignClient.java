package com.fms.hr_crm.calling.client;

import com.fms.hr_crm.calling.model.dto.LeadSummary;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.UUID;

/**
 * Declarative HTTP client for hr-lead-service internal endpoints.
 * Uses Spring's {@code @HttpExchange} (no Spring Cloud required).
 *
 * <p>The {@code X-Internal-Service: calling-service} header is added to every
 * request by the {@link RestClient} configured in {@link LeadServiceFeignConfig}.
 *
 * <p><strong>Security:</strong> {@code getLeadPhone} returns a real phone number.
 * Callers MUST immediately pass the result to MaskingService and MUST NOT log it.
 */
@HttpExchange
public interface LeadServiceFeignClient {

    /** Returns the real phone number for a lead. Do NOT log the return value. */
    @GetExchange("/api/leads/internal/{id}/phone")
    String getLeadPhone(
            @PathVariable("id") UUID leadId,
            @RequestParam("tenantId") UUID tenantId);

    /** Returns a batch of lead IDs for AI campaign dialing. */
    @GetExchange("/api/leads/internal/campaign-queue")
    List<UUID> getLeadIdsForCampaign(
            @RequestParam("tenantId") UUID tenantId,
            @RequestParam("status")   String status,
            @RequestParam("limit")    int limit);

    /** Updates a lead's status from an AI campaign IVR response. */
    @PostExchange("/api/leads/internal/{id}/status")
    void updateLeadStatus(
            @PathVariable("id")        UUID leadId,
            @RequestParam("tenantId")  UUID tenantId,
            @RequestParam("newStatus") String newStatus);

    /** Returns lightweight lead summaries assigned to a recruiter. */
    @GetExchange("/api/leads/internal/recruiter-leads")
    List<LeadSummary> getLeadsForRecruiter(
            @RequestParam("tenantId")    UUID tenantId,
            @RequestParam("recruiterId") UUID recruiterId);
}