package com.fms.hr_crm.lead.controller;

import com.fms.hr_crm.lead.model.dto.*;
import com.fms.hr_crm.lead.model.entity.LeadStatus;
import com.fms.hr_crm.lead.security.UserPrincipal;
import com.fms.hr_crm.lead.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    // GET /api/leads?status=NEW&page=0&size=20&sort=createdAt,desc
    @GetMapping
    public Page<LeadResponse> list(
            @RequestParam(required = false) LeadStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal UserPrincipal user) {

        return leadService.findAll(user.getTenantId(), status, pageable);
    }

    // GET /api/leads/{id}
    @GetMapping("/{id}")
    public LeadResponse getOne(@PathVariable UUID id,
                               @AuthenticationPrincipal UserPrincipal user) {
        return leadService.findById(id, user.getTenantId());
    }

    // POST /api/leads
    @PostMapping
    public ResponseEntity<LeadResponse> create(
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        LeadResponse response = leadService.create(request, user.getTenantId());
        return ResponseEntity
                .created(URI.create("/api/leads/" + response.id()))
                .body(response);
    }

    // POST /api/leads/{id}/transition
    @PostMapping("/{id}/transition")
    public LeadResponse transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return leadService.updateStatus(
                id, request.newStatus(), request.note(), user.getTenantId());
    }

    // POST /api/leads/{id}/assign
    @PostMapping("/{id}/assign")
    public LeadResponse assign(
            @PathVariable UUID id,
            @RequestParam UUID recruiterId,
            @AuthenticationPrincipal UserPrincipal user) {

        return leadService.assignRecruiter(id, recruiterId, user.getTenantId());
    }

    // DELETE /api/leads/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @AuthenticationPrincipal UserPrincipal user) {
        leadService.softDelete(id, user.getTenantId());
    }

    // GET /api/leads/pipeline/stats
    @GetMapping("/pipeline/stats")
    public PipelineStats pipelineStats(@AuthenticationPrincipal UserPrincipal user) {
        return leadService.getPipelineStats(user.getTenantId());
    }

    /**
     * Internal endpoint — returns a list of lead IDs for AI campaign dialing.
     * Called ONLY by hr-calling-service.
     */
    @GetMapping("/internal/campaign-queue")
    public ResponseEntity<List<UUID>> getCampaignQueue(
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "NEW") LeadStatus status,
            @RequestParam(defaultValue = "500") int limit,
            @RequestHeader(value = "X-Internal-Service", required = false) String serviceId) {

        if (!"calling-service".equals(serviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(leadService.getLeadIdsForCampaign(tenantId, status, limit));
    }

    /**
     * Internal endpoint — updates a lead's status from an AI campaign IVR response.
     * Called ONLY by hr-calling-service after the lead presses a keypad digit.
     */
    @PostMapping("/internal/{id}/status")
    public ResponseEntity<Void> updateStatusInternal(
            @PathVariable UUID id,
            @RequestParam UUID tenantId,
            @RequestParam String newStatus,
            @RequestHeader(value = "X-Internal-Service", required = false) String serviceId) {

        if (!"calling-service".equals(serviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            var status = LeadStatus.valueOf(newStatus);
            leadService.updateStatus(id, status, "Updated by AI campaign call", tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.ok().build(); // swallow transition errors (e.g. already in that state)
        }
    }

    /**
     * Internal endpoint — returns lightweight lead info (no phone) for a recruiter's assigned leads.
     * Called ONLY by hr-calling-service to populate the Quick Call lead picker.
     */
    @GetMapping("/internal/recruiter-leads")
    public ResponseEntity<List<java.util.Map<String, Object>>> getRecruiterLeads(
            @RequestParam UUID tenantId,
            @RequestParam UUID recruiterId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestHeader(value = "X-Internal-Service", required = false) String serviceId) {

        if (!"calling-service".equals(serviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var pageable = org.springframework.data.domain.PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        var leads = leadService.findAll(tenantId, null, pageable)
                .getContent()
                .stream()
                .filter(l -> recruiterId.equals(l.recruiterId()))
                .map(l -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id",               l.id());
                    m.put("name",             l.name() != null ? l.name() : "");
                    m.put("status",           l.status() != null ? l.status().name() : "NEW");
                    m.put("company",          l.company() != null ? l.company() : "");
                    m.put("positionRequired", l.positionRequired() != null ? l.positionRequired() : "");
                    m.put("recruiterId",      l.recruiterId());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(leads);
    }

    /**
     * Internal service-to-service endpoint — returns the real phone number for a lead.
     * Called ONLY by hr-calling-service; not intended for recruiter-facing clients.
     *
     * <p>Callers must include the {@code X-Internal-Service: calling-service} header.
     * The returned phone number must be immediately encrypted by the calling service.
     */
    @GetMapping("/internal/{id}/phone")
    public ResponseEntity<String> getLeadPhoneInternal(
            @PathVariable UUID id,
            @RequestParam UUID tenantId,
            @RequestHeader(value = "X-Internal-Service", required = false) String serviceId) {

        if (!"calling-service".equals(serviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return leadService.getPhoneForInternalUse(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}