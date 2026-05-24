package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.model.dto.CallSessionResponse;
import com.fms.hr_crm.calling.model.dto.InitiateCallRequest;
import com.fms.hr_crm.calling.model.dto.TwilioTokenResponse;
import com.fms.hr_crm.calling.service.CallService;
import com.fms.hr_crm.calling.service.TwilioTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * Recruiter-facing REST API for call management.
 *
 * <p>SECURITY: All responses go through {@link CallSessionResponse} which
 * MUST NOT include any phone numbers — real or virtual.
 */
@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService        callService;
    private final TwilioTokenService tokenService;

    /** Initiates a masked outbound call. */
    @PostMapping
    public ResponseEntity<CallSessionResponse> initiateCall(
            @Valid @RequestBody InitiateCallRequest req) {

        var response = callService.initiateCall(req);
        return ResponseEntity
                .created(URI.create("/api/calls/" + response.id()))
                .body(response);
    }

    /** Gets a single call session by ID. */
    @GetMapping("/{id}")
    public CallSessionResponse getOne(@PathVariable UUID id) {
        return callService.findById(id);
    }

    /** Lists call sessions for a tenant, newest first. */
    @GetMapping
    public Page<CallSessionResponse> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID recruiterId,
            @RequestParam(required = false) UUID leadId,
            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {

        if (recruiterId != null) {
            return callService.findByRecruiter(tenantId, recruiterId, pageable);
        }
        if (leadId != null) {
            return callService.findByLead(tenantId, leadId, pageable);
        }
        return callService.findAll(tenantId, pageable);
    }

    /**
     * Generates a Twilio Access Token for browser-based (WebRTC) calling.
     * The token grants permission to make outgoing calls via the configured TwiML App.
     */
    @GetMapping("/token")
    public TwilioTokenResponse getBrowserToken(
            @RequestParam UUID recruiterId,
            @RequestParam(defaultValue = "00000000-0000-0000-0000-000000000001") UUID tenantId) {

        return tokenService.generateAccessToken(recruiterId, tenantId);
    }
}