package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.model.dto.ProvisionRecruiterRequest;
import com.fms.hr_crm.calling.service.RecruiterAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API consumed by hr-lead-service to provision recruiter login accounts.
 * Protected by the {@code X-Internal-Service} header check in SecurityConfig.
 */
@RestController
@RequestMapping("/api/internal/recruiters")
@RequiredArgsConstructor
@Slf4j
public class InternalRecruiterController {

    private final RecruiterAuthService authService;

    /**
     * Provisions (or re-provisions) a recruiter login account with a one-time password.
     * Called by hr-lead-service immediately after a recruiter is onboarded.
     */
    @PostMapping
    public ResponseEntity<Void> provision(@RequestBody ProvisionRecruiterRequest request) {
        log.info("provision() — recruiterId={} username={} tenantId={}",
                request.recruiterId(), request.username(), request.tenantId());
        try {
            authService.provisionRecruiter(
                    request.recruiterId(),
                    request.username(),
                    request.email(),
                    request.fullName(),
                    request.tempPassword(),
                    request.tenantId()
            );
        } catch (DataIntegrityViolationException e) {
            // Race condition: a concurrent request already provisioned this user.
            // The account exists and is set up — treat as idempotent success.
            log.warn("provision() — username={} already provisioned by concurrent request, skipping",
                    request.username());
        }
        return ResponseEntity.ok().build();
    }
}