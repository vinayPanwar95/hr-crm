package com.fms.hr_crm.calling.service;

import com.fms.hr_crm.calling.client.LeadServiceClient;
import com.fms.hr_crm.calling.exception.CallSessionNotFoundException;
import com.fms.hr_crm.calling.model.dto.CallSessionResponse;
import com.fms.hr_crm.calling.model.dto.InitiateCallRequest;
import com.fms.hr_crm.calling.model.entity.CallSession;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import com.fms.hr_crm.calling.provider.CallingProvider;
import com.fms.hr_crm.calling.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates outbound call initiation and session management.
 *
 * <p>NEVER log phone numbers. Real numbers are handled exclusively by {@link MaskingService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CallService {

    private static final Set<CallStatus> TERMINAL_STATUSES = Set.of(
            CallStatus.COMPLETED, CallStatus.FAILED,
            CallStatus.NO_ANSWER, CallStatus.BUSY, CallStatus.CANCELED);

    private final CallSessionRepository    repo;
    private final MaskingService           maskingService;
    private final LeadServiceClient        leadClient;
    private final CallingProvider          callingProvider;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Initiates a masked outbound call.
     *
     * <ol>
     *   <li>Fetches the lead's real phone from hr-lead-service (never logged)</li>
     *   <li>Encrypts and stores it via {@link MaskingService}</li>
     *   <li>Calls Twilio to dial the recruiter — when recruiter answers, Twilio
     *       fetches TwiML from our webhook which bridges to the lead</li>
     * </ol>
     */
    @Transactional
    public CallSessionResponse initiateCall(InitiateCallRequest req) {
        // 1. Create session first so we have an ID for the webhook URL
        var session = new CallSession();
        session.setTenantId(req.tenantId());
        session.setRecruiterId(req.recruiterId());
        session.setLeadId(req.leadId());
        session.setVirtualFromNumber(callingProvider.fromNumber());
        session.setRecruiterPhone(req.recruiterPhone());
        session.setStatus(CallStatus.INITIATED);
        session = repo.save(session);

        // 2. Fetch real phone from lead-service — do NOT log this value
        String realPhone = leadClient.getLeadPhone(req.leadId(), req.tenantId());

        // 3. Encrypt and store (MaskingService is the only writer of realNumberEncrypted)
        maskingService.encryptAndStore(session, realPhone);
        session = repo.save(session);

        // 4. Initiate call via the active provider (Twilio / Plivo / Telnyx)
        try {
            var sessionId    = session.getId();
            var base         = callingProvider.webhookBaseUrl();
            var voiceUrl     = base + "/webhook/call/voice/"     + sessionId;
            var statusUrl    = base + "/webhook/call/status/"    + sessionId;
            var recordingUrl = base + "/webhook/call/recording/" + sessionId;

            var toNumber = (req.recruiterPhone() != null && !req.recruiterPhone().isBlank())
                    ? req.recruiterPhone()
                    : "+15005550006"; // test number fallback for local dev

            var result = callingProvider.initiateCall(toNumber, voiceUrl, statusUrl, recordingUrl);

            session.setTwilioCallSid(result.callId());   // field name kept for backwards compat
            session.setStatus(result.initialStatus());
            log.info("Call initiated via {}: sessionId={} callId={} recruiterId={} leadId={}",
                    callingProvider.providerName(), sessionId, result.callId(),
                    req.recruiterId(), req.leadId());
        } catch (Exception e) {
            log.warn("Call initiation failed via {} (session {}): {} — session saved as FAILED",
                    callingProvider.providerName(), session.getId(), e.getMessage());
            session.setStatus(CallStatus.FAILED);
        }

        return toResponse(repo.save(session));
    }

    public CallSessionResponse findById(UUID id) {
        return toResponse(requireSession(id));
    }

    public Page<CallSessionResponse> findAll(UUID tenantId, Pageable pageable) {
        return repo.findByTenantId(tenantId, pageable).map(this::toResponse);
    }

    public Page<CallSessionResponse> findByRecruiter(UUID tenantId, UUID recruiterId, Pageable pageable) {
        return repo.findByTenantIdAndRecruiterId(tenantId, recruiterId, pageable).map(this::toResponse);
    }

    public Page<CallSessionResponse> findByLead(UUID tenantId, UUID leadId, Pageable pageable) {
        return repo.findByTenantIdAndLeadId(tenantId, leadId, pageable).map(this::toResponse);
    }

    /** Updates session from a Twilio status callback. */
    @Transactional
    public void updateStatus(UUID sessionId, CallStatus newStatus, Integer durationSeconds) {
        var session = requireSession(sessionId);
        session.setStatus(newStatus);

        if (durationSeconds != null) {
            session.setDurationSeconds(durationSeconds);
        }
        if (newStatus == CallStatus.COMPLETED || newStatus == CallStatus.FAILED
                || newStatus == CallStatus.NO_ANSWER || newStatus == CallStatus.BUSY) {
            session.setEndedAt(Instant.now());
            session.setCallTag(autoTag(session));
        }

        repo.save(session);
        log.info("Call session {} status updated to {}", sessionId, newStatus);

        // Notify campaign orchestrator after transaction commits
        if (session.getCampaignId() != null && TERMINAL_STATUSES.contains(newStatus)) {
            eventPublisher.publishEvent(new CallStatusUpdatedEvent(
                    this, sessionId, session.getCampaignId(), newStatus, session.getLeadId()));
        }
    }

    /** Updates session from a Twilio status callback using callSid. */
    @Transactional
    public void updateStatusByCallSid(String callSid, CallStatus newStatus, Integer durationSeconds) {
        repo.findByTwilioCallSid(callSid).ifPresent(session -> {
            session.setStatus(newStatus);
            if (durationSeconds != null) session.setDurationSeconds(durationSeconds);
            if (TERMINAL_STATUSES.contains(newStatus)) {
                session.setEndedAt(Instant.now());
                session.setCallTag(autoTag(session));
            }
            repo.save(session);
            log.info("Call {} status updated to {} via callSid", callSid, newStatus);

            if (session.getCampaignId() != null && TERMINAL_STATUSES.contains(newStatus)) {
                eventPublisher.publishEvent(new CallStatusUpdatedEvent(
                        this, session.getId(), session.getCampaignId(), newStatus, session.getLeadId()));
            }
        });
    }

    /**
     * Initiates an AI campaign call directly to a lead's phone.
     *
     * <p>Unlike {@link #initiateCall} (which dials the recruiter first), AI campaign
     * calls dial the lead directly. The voice webhook returns an AI script with IVR.
     *
     * <p>SECURITY: {@code decryptForCampaignDial} is the ONLY place in this method
     * where the real phone is exposed. It flows directly to the provider API — never logged.
     */
    @Transactional
    public CallSessionResponse initiateAiCampaignCall(UUID tenantId, UUID leadId, UUID campaignId) {
        var session = new CallSession();
        session.setTenantId(tenantId);
        session.setRecruiterId(UUID.fromString("00000000-0000-0000-0000-000000000000")); // zero UUID = AI agent marker
        session.setLeadId(leadId);
        session.setCampaignId(campaignId);
        session.setVirtualFromNumber(callingProvider.fromNumber());
        session.setStatus(CallStatus.INITIATED);
        session = repo.save(session);

        // Fetch and encrypt the lead's real phone — do NOT log
        var realPhone = leadClient.getLeadPhone(leadId, tenantId);
        maskingService.encryptAndStore(session, realPhone);
        session = repo.save(session);

        try {
            var base      = callingProvider.webhookBaseUrl();
            var voiceUrl  = base + "/webhook/campaign/" + campaignId + "/voice/"  + session.getId();
            var statusUrl = base + "/webhook/campaign/" + campaignId + "/status/" + session.getId();

            // Decrypt to dial the lead directly — intentional for AI outbound
            var dialPhone = maskingService.decryptForCampaignDial(session);
            var result    = callingProvider.initiateCall(dialPhone, voiceUrl, statusUrl, null);

            session.setTwilioCallSid(result.callId());
            session.setStatus(result.initialStatus());
            log.info("AI campaign call initiated: campaignId={} sessionId={} leadId={}",
                    campaignId, session.getId(), leadId);
        } catch (Exception e) {
            log.warn("AI campaign call failed to initiate: campaignId={} leadId={}: {}",
                    campaignId, leadId, e.getMessage());
            session.setStatus(CallStatus.FAILED);
            // Publish immediately so orchestrator can release the slot
            eventPublisher.publishEvent(new CallStatusUpdatedEvent(
                    this, session.getId(), campaignId, CallStatus.FAILED, leadId));
        }

        return toResponse(repo.save(session));
    }

    /**
     * Cancels an active call. Calls Twilio to terminate the call if a callSid exists,
     * then marks the session as CANCELED regardless of whether Twilio succeeded.
     *
     * <p>Works for INITIATED and RINGING states (use COMPLETED for in-progress).
     */
    @Transactional
    public CallSessionResponse cancelCall(UUID sessionId) {
        var session = requireSession(sessionId);
        log.info("cancelCall() — sessionId={} callSid={} currentStatus={}",
                sessionId, session.getTwilioCallSid(), session.getStatus());

        if (session.getTwilioCallSid() != null) {
            // cancelCall() implementations are required to swallow provider errors internally
            callingProvider.cancelCall(session.getTwilioCallSid());
        }

        session.setStatus(CallStatus.CANCELED);
        session.setEndedAt(Instant.now());
        session.setCallTag("NO_RESPONSE");
        return toResponse(repo.save(session));
    }

    /** Saves a recording URL on the call session. */
    @Transactional
    public void saveRecording(UUID sessionId, String recordingUrl) {
        var session = requireSession(sessionId);
        session.setRecordingUrl(recordingUrl);
        repo.save(session);
        log.info("Recording saved for session {}", sessionId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Auto-tags the call based on outcome and duration.
     * Simple rule: short calls = CALL_BACK, long completed = HOT_LEAD, failed = NO_RESPONSE.
     */
    private String autoTag(CallSession session) {
        if (session.getStatus() == CallStatus.NO_ANSWER || session.getStatus() == CallStatus.BUSY) {
            return "CALL_BACK";
        }
        if (session.getStatus() == CallStatus.FAILED) {
            return "NO_RESPONSE";
        }
        var duration = session.getDurationSeconds();
        if (duration == null || duration < 30) {
            return "BRIEF_CALL";
        }
        if (duration >= 120) {
            return "HOT_LEAD";
        }
        return "CONTACTED";
    }

    private CallSession requireSession(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new CallSessionNotFoundException(id));
    }

    private CallSessionResponse toResponse(CallSession s) {
        return new CallSessionResponse(
                s.getId(),
                s.getRecruiterId(),
                s.getLeadId(),
                s.getTenantId(),
                s.getStatus(),
                s.getDurationSeconds(),
                s.getCallTag(),
                s.getRecordingUrl(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.getUpdatedAt(),
                s.getCampaignId()
        );
    }
}