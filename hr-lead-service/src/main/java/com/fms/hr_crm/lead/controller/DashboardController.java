package com.fms.hr_crm.lead.controller;

import com.fms.hr_crm.lead.security.UserPrincipal;
import com.fms.hr_crm.lead.service.LeadService;
import com.fms.hr_crm.lead.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the main landing dashboard shown after login.
 * Provides summary stats for both the Lead Pipeline and Recruiter sections.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final LeadService leadService;
    private final RecruiterService recruiterService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model,
                            @AuthenticationPrincipal UserPrincipal user) {

        var tenantId = user != null ? user.getTenantId() : java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        log.debug("Loading dashboard for tenant {}", tenantId);

        var stats = leadService.getPipelineStats(tenantId);
        var totalLeads = stats.countByStatus().values().stream().mapToLong(Long::longValue).sum();

        model.addAttribute("stats",      stats);
        model.addAttribute("totalLeads", totalLeads);
        model.addAttribute("recruiters", recruiterService.findAll(tenantId, PageRequest.of(0, 5)));
        return "dashboard";
    }
}