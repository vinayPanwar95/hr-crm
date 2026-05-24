package com.fms.hr_crm.lead.controller;

import com.fms.hr_crm.lead.model.dto.LeadRequest;
import com.fms.hr_crm.lead.model.entity.LeadSource;
import com.fms.hr_crm.lead.model.entity.LeadStatus;
import com.fms.hr_crm.lead.security.UserPrincipal;
import com.fms.hr_crm.lead.service.CsvImportService;
import com.fms.hr_crm.lead.service.LeadService;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/leads")
@RequiredArgsConstructor
@Slf4j
public class LeadViewController {

    private final LeadService leadService;
    private final CsvImportService csvImportService;

    /** Resolve tenant — fallback to a default UUID until auth is fully wired */
    private UUID tenantId(UserPrincipal user) {
        return user != null ? user.getTenantId() : UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            @AuthenticationPrincipal UserPrincipal user) {

        var tid      = tenantId(user);
        var pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        model.addAttribute("leads",          leadService.findAll(tid, status, pageable));
        model.addAttribute("stats",          leadService.getPipelineStats(tid));
        model.addAttribute("statuses",       LeadStatus.values());
        model.addAttribute("sources",        LeadSource.values());
        model.addAttribute("selectedStatus", status);
        return "leads/index";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model,
                         @AuthenticationPrincipal UserPrincipal user) {

        var tid = tenantId(user);
        model.addAttribute("lead",     leadService.findById(id, tid));
        model.addAttribute("statuses", LeadStatus.values());
        return "leads/detail";
    }

    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String positionRequired,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) UUID recruiterId,
            @AuthenticationPrincipal UserPrincipal user,
            RedirectAttributes ra) {
        try {
            var req = new LeadRequest(name, phone, email, company, positionRequired, source, recruiterId);
            leadService.create(req, tenantId(user));
            ra.addFlashAttribute("success", "Lead created successfully.");
        } catch (Exception ex) {
            log.warn("Failed to create lead: {}", ex.getMessage());
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leads";
    }

    @PostMapping("/{id}/transition")
    public String transition(
            @PathVariable UUID id,
            @RequestParam LeadStatus newStatus,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserPrincipal user,
            RedirectAttributes ra) {
        try {
            leadService.updateStatus(id, newStatus, note, tenantId(user));
            ra.addFlashAttribute("success", "Status updated to " + newStatus + ".");
        } catch (Exception ex) {
            log.warn("Failed to transition lead {}: {}", id, ex.getMessage());
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leads/" + id;
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            @AuthenticationPrincipal UserPrincipal user,
                            RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Please select a CSV file to upload.");
            return "redirect:/leads";
        }
        try {
            var result = csvImportService.importCsv(file, tenantId(user));
            ra.addFlashAttribute("success",
                    "Import complete — " + result.created() + " created, " +
                    result.skipped() + " skipped out of " + result.total() + " rows.");
            if (!result.errors().isEmpty()) {
                ra.addFlashAttribute("importErrors", result.errors());
            }
        } catch (Exception ex) {
            log.warn("CSV import error: {}", ex.getMessage());
            ra.addFlashAttribute("error", "Import failed: " + ex.getMessage());
        }
        return "redirect:/leads";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id,
                         @AuthenticationPrincipal UserPrincipal user,
                         RedirectAttributes ra) {
        try {
            leadService.softDelete(id, tenantId(user));
            ra.addFlashAttribute("success", "Lead deleted.");
        } catch (Exception ex) {
            log.warn("Failed to delete lead {}: {}", id, ex.getMessage());
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leads";
    }
}