package com.fms.hr_crm.lead.controller;

import com.fms.hr_crm.lead.model.dto.RecruiterRequest;
import com.fms.hr_crm.lead.model.dto.RecruiterResponse;
import com.fms.hr_crm.lead.security.UserPrincipal;
import com.fms.hr_crm.lead.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Handles the Recruiter Dashboard UI (/recruiters) and the
 * autocomplete JSON API used by the Add Lead form (/api/recruiters/search).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RecruiterViewController {

    private final RecruiterService recruiterService;

    private UUID tenantId(UserPrincipal user) {
        return user != null ? user.getTenantId() : UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    @GetMapping("/recruiters")
    public String index(
            @RequestParam(defaultValue = "0") int page,
            Model model,
            @AuthenticationPrincipal UserPrincipal user) {

        var pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());
        model.addAttribute("recruiters", recruiterService.findAll(tenantId(user), pageable));
        return "recruiters/index";
    }

    @PostMapping("/recruiters")
    public String create(
            @RequestParam String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam String loginPassword,
            @AuthenticationPrincipal UserPrincipal user,
            RedirectAttributes ra) {
        try {
            recruiterService.create(new RecruiterRequest(name, email, phone, loginPassword), tenantId(user));
            ra.addFlashAttribute("success", "Recruiter '" + name + "' onboarded successfully.");
        } catch (Exception ex) {
            log.warn("Failed to create recruiter: {}", ex.getMessage());
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/recruiters";
    }

    @PostMapping("/recruiters/{id}/deactivate")
    public String deactivate(@PathVariable UUID id,
                             @AuthenticationPrincipal UserPrincipal user,
                             RedirectAttributes ra) {
        try {
            recruiterService.deactivate(id, tenantId(user));
            ra.addFlashAttribute("success", "Recruiter deactivated.");
        } catch (Exception ex) {
            log.warn("Failed to deactivate recruiter {}: {}", id, ex.getMessage());
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/recruiters";
    }

    // ── JSON API (used by Add Lead autocomplete) ──────────────────────────────

    @GetMapping("/api/recruiters/search")
    @ResponseBody
    public ResponseEntity<List<RecruiterResponse>> search(
            @RequestParam(defaultValue = "") String name,
            @AuthenticationPrincipal UserPrincipal user) {

        if (name.length() < 1) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(recruiterService.searchByName(tenantId(user), name));
    }
}