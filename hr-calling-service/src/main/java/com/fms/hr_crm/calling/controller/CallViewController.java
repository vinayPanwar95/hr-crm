package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.client.LeadServiceClient;
import com.fms.hr_crm.calling.model.dto.InitiateCallRequest;
import com.fms.hr_crm.calling.model.dto.LeadSummary;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import com.fms.hr_crm.calling.repository.CallSessionRepository;
import com.fms.hr_crm.calling.service.CallService;
import com.fms.hr_crm.calling.service.TwilioTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Thymeleaf UI controller for call logs and click-to-call.
 */
@Controller
@RequestMapping("/calls")
@RequiredArgsConstructor
@Slf4j
public class CallViewController {

    private final CallService              callService;
    private final TwilioTokenService       tokenService;
    private final CallSessionRepository    repo;
    private final LeadServiceClient        leadClient;

    /** Hardcoded for local dev — replace with auth principal when security is wired. */
    private static final UUID DEFAULT_TENANT    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_RECRUITER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) CallStatus status,
            @RequestParam(required = false) UUID leadId,
            @RequestParam(required = false) Boolean mustChangePassword,
            Model model) {

        log.info("index() — page={}, status={}, leadId={}", page, status, leadId);

        var pageable = PageRequest.of(page, 20, Sort.by("startedAt").descending());

        var calls = (status != null)
                ? repo.findByTenantIdAndStatus(DEFAULT_TENANT, status, pageable)
                : (leadId != null)
                        ? repo.findByTenantIdAndLeadId(DEFAULT_TENANT, leadId, pageable)
                        : repo.findByTenantId(DEFAULT_TENANT, pageable);

        log.debug("index() — fetched {} calls (page {}/{})", calls.getNumberOfElements(), page, calls.getTotalPages());

        model.addAttribute("calls",           calls);
        model.addAttribute("statuses",        CallStatus.values());
        model.addAttribute("selectedStatus",  status);
        model.addAttribute("totalCalls",      repo.countByTenantIdAndStatus(DEFAULT_TENANT, CallStatus.COMPLETED)
                                             + repo.countByTenantIdAndStatus(DEFAULT_TENANT, CallStatus.FAILED));
        model.addAttribute("completedCalls",  repo.countByTenantIdAndStatus(DEFAULT_TENANT, CallStatus.COMPLETED));
        model.addAttribute("failedCalls",     repo.countByTenantIdAndStatus(DEFAULT_TENANT, CallStatus.FAILED));
        model.addAttribute("inProgressCalls", repo.countByTenantIdAndStatus(DEFAULT_TENANT, CallStatus.IN_PROGRESS));

        log.debug("index() — generating browser token for recruiter={}", DEFAULT_RECRUITER);
        model.addAttribute("browserToken",    tokenService.generateAccessToken(DEFAULT_RECRUITER, DEFAULT_TENANT).token());

        // Load leads assigned to the current recruiter for the Quick Call picker.
        // Degrade gracefully if hr-lead-service is unavailable.
        List<LeadSummary> recruiterLeads;
        try {
            recruiterLeads = leadClient.getLeadsForRecruiter(DEFAULT_TENANT, DEFAULT_RECRUITER);
            log.debug("index() — loaded {} recruiter leads", recruiterLeads.size());
        } catch (Exception e) {
            log.warn("index() — could not load recruiter leads (lead-service unavailable?): {}", e.getMessage());
            recruiterLeads = List.of();
        }
        model.addAttribute("recruiterLeads", recruiterLeads);

        if (Boolean.TRUE.equals(mustChangePassword)) {
            model.addAttribute("mustChangePasswordWarning", true);
        }

        log.info("index() — rendering calls/index");
        return "calls/index";
    }

    /**
     * JSON endpoint — returns leads assigned to the current recruiter.
     * Called by the Quick Call lead picker via fetch().
     */
    @GetMapping("/api/calls/leads")
    @ResponseBody
    public ResponseEntity<List<LeadSummary>> getRecruiterLeadsJson(
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000001") UUID tenantId,
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000001") UUID recruiterId) {
        log.debug("getRecruiterLeadsJson() — tenantId={} recruiterId={}", tenantId, recruiterId);
        return ResponseEntity.ok(leadClient.getLeadsForRecruiter(tenantId, recruiterId));
    }

    /**
     * UI page — shows all leads assigned to the current recruiter with click-to-call buttons.
     * Fetches data from hr-lead-service via internal API.
     */
    @GetMapping("/leads")
    public String leadsPage(Model model) {
        log.info("leadsPage() — fetching leads for recruiter={} tenant={}", DEFAULT_RECRUITER, DEFAULT_TENANT);
        var leads = leadClient.getLeadsForRecruiter(DEFAULT_TENANT, DEFAULT_RECRUITER);
        model.addAttribute("leads", leads);
        model.addAttribute("browserToken",
                tokenService.generateAccessToken(DEFAULT_RECRUITER, DEFAULT_TENANT).token());
        log.debug("leadsPage() — loaded {} leads", leads.size());
        return "leads/index";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        log.info("detail() — fetching call session id={}", id);
        model.addAttribute("call", callService.findById(id));
        log.info("detail() — rendering calls/detail for id={}", id);
        return "calls/detail";
    }

    @GetMapping("/initiate")
    public String initiateRedirect() {
        log.debug("initiateRedirect() — GET /calls/initiate redirecting to /calls");
        return "redirect:/calls";
    }

    @PostMapping("/{id}/cancel")
    public String cancelCall(@PathVariable UUID id, RedirectAttributes ra) {
        log.info("cancelCall() — sessionId={}", id);
        try {
            callService.cancelCall(id);
            log.info("cancelCall() — session {} canceled successfully", id);
            ra.addFlashAttribute("success", "Call canceled — session " + id);
        } catch (Exception ex) {
            log.warn("cancelCall() — failed for sessionId={}: {}", id, ex.getMessage(), ex);
            ra.addFlashAttribute("error", "Could not cancel call: " + ex.getMessage());
        }
        return "redirect:/calls";
    }

    @PostMapping("/initiate")
    public String initiateCall(
            @RequestParam UUID leadId,
            @RequestParam(required = false) String recruiterPhone,
            RedirectAttributes ra) {
        log.info("initiateCall() — leadId={}, recruiterPhone present={}", leadId, recruiterPhone != null);
        try {
            var req = new InitiateCallRequest(
                    DEFAULT_RECRUITER, leadId, DEFAULT_TENANT, recruiterPhone);
            var session = callService.initiateCall(req);
            log.info("initiateCall() — call initiated successfully, sessionId={}", session.id());
            ra.addFlashAttribute("success",
                    "Call initiated for lead " + leadId + " — session " + session.id());
        } catch (Exception ex) {
            log.warn("initiateCall() — failed for leadId={}: {}", leadId, ex.getMessage(), ex);
            ra.addFlashAttribute("error", "Call failed: " + ex.getMessage());
        }
        log.debug("initiateCall() — redirecting to /calls");
        return "redirect:/calls";
    }
}