package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.model.dto.CampaignRequest;
import com.fms.hr_crm.calling.model.dto.CampaignResponse;
import com.fms.hr_crm.calling.model.entity.AiCampaign;
import com.fms.hr_crm.calling.model.entity.CampaignStatus;
import com.fms.hr_crm.calling.repository.AiCampaignRepository;
import com.fms.hr_crm.calling.service.AiCampaignOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Handles the AI Campaigns UI (/campaigns) and status polling API (/api/campaigns/{id}/status).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CampaignController {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001"); // fallback — replace with real auth principal
    private static final int  MAX_AGENTS = 10;

    private final AiCampaignRepository   campaignRepo;
    private final AiCampaignOrchestrator orchestrator;

    // ── UI ────────────────────────────────────────────────────────────────────

    @GetMapping("/campaigns")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        var pageable   = PageRequest.of(page, 20, Sort.by("createdAt").descending());
        var campaigns  = campaignRepo.findByTenantId(TENANT_ID, pageable).map(this::toResponse);
        model.addAttribute("campaigns", campaigns);
        return "campaigns/index";
    }

    @PostMapping("/campaigns")
    public String create(
            @RequestParam String     name,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "1") int agentCount,
            @RequestParam(defaultValue = "NEW") String targetLeadStatus,
            @RequestParam(required = false) String windowStart,
            @RequestParam(required = false) String windowEnd,
            @RequestParam(required = false) String scheduledDate,
            RedirectAttributes ra) {

        try {
            int agents = Math.min(Math.max(agentCount, 1), MAX_AGENTS);

            var campaign = new AiCampaign();
            campaign.setTenantId(TENANT_ID);
            campaign.setName(name.trim());
            campaign.setDescription(description);
            campaign.setAgentCount(agents);
            campaign.setTargetLeadStatus(targetLeadStatus);
            campaign.setWindowStart(parseTime(windowStart));
            campaign.setWindowEnd(parseTime(windowEnd));
            campaign.setScheduledDate(parseDate(scheduledDate));
            campaign.setStatus(campaign.getScheduledDate() != null
                    ? CampaignStatus.SCHEDULED : CampaignStatus.DRAFT);

            campaignRepo.save(campaign);
            ra.addFlashAttribute("success",
                    "Campaign '" + name + "' created. Click Start to begin calling.");
            log.info("Campaign created: id={} name='{}' agents={}", campaign.getId(), name, agents);
        } catch (Exception ex) {
            log.warn("Failed to create campaign: {}", ex.getMessage());
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/campaigns";
    }

    @PostMapping("/campaigns/{id}/start")
    public String start(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            orchestrator.start(id);
            ra.addFlashAttribute("success", "Campaign started — AI agents are dialing.");
        } catch (Exception ex) {
            log.warn("Failed to start campaign {}: {}", id, ex.getMessage());
            ra.addFlashAttribute("error", "Could not start campaign: " + ex.getMessage());
        }
        return "redirect:/campaigns";
    }

    @PostMapping("/campaigns/{id}/stop")
    public String stop(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            orchestrator.stop(id);
            ra.addFlashAttribute("success", "Campaign stopped.");
        } catch (Exception ex) {
            log.warn("Failed to stop campaign {}: {}", id, ex.getMessage());
            ra.addFlashAttribute("error", "Could not stop campaign: " + ex.getMessage());
        }
        return "redirect:/campaigns";
    }

    // ── JSON API — polled by UI for live progress ─────────────────────────────

    @GetMapping("/api/campaigns/{id}/status")
    @ResponseBody
    public ResponseEntity<CampaignResponse> status(@PathVariable UUID id) {
        return campaignRepo.findById(id)
                .map(c -> ResponseEntity.ok(toResponse(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/campaigns")
    @ResponseBody
    public Page<CampaignResponse> list(@RequestParam(defaultValue = "0") int page) {
        return campaignRepo.findByTenantId(TENANT_ID, PageRequest.of(page, 20,
                Sort.by("createdAt").descending())).map(this::toResponse);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CampaignResponse toResponse(AiCampaign c) {
        return new CampaignResponse(
                c.getId(), c.getName(), c.getDescription(),
                c.getAgentCount(), c.getTargetLeadStatus(),
                c.getWindowStart(), c.getWindowEnd(), c.getScheduledDate(),
                c.getStatus(), c.getTotalLeads(), c.getCalledCount(),
                c.getCompletedCount(), c.getFailedCount(),
                c.getStartedAt(), c.getStoppedAt(), c.getCreatedAt());
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalTime.parse(s); } catch (DateTimeParseException e) { return null; }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }
}