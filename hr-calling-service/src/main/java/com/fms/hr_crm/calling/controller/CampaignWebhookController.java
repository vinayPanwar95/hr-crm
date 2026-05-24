package com.fms.hr_crm.calling.controller;

import com.fms.hr_crm.calling.client.LeadServiceClient;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import com.fms.hr_crm.calling.repository.CallSessionRepository;
import com.fms.hr_crm.calling.service.CallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles Twilio webhooks for AI campaign calls.
 *
 * <p>Campaign calls use separate webhook paths ({@code /webhook/campaign/...}) so the
 * AI-specific TwiML (IVR script) is cleanly separated from the recruiter-call voice flow.
 *
 * <p>Security note: these endpoints are public (Twilio must reach them).
 * In production, add Twilio signature validation matching {@link WebhookController}.
 */
@RestController
@RequestMapping("/webhook/campaign")
@RequiredArgsConstructor
@Slf4j
public class CampaignWebhookController {

    private final CallService           callService;
    private final CallSessionRepository sessionRepo;
    private final LeadServiceClient     leadClient;

    /**
     * Voice webhook — Twilio calls this when the lead picks up the AI campaign call.
     * Returns TwiML that plays the AI recruiter script and presents an IVR menu.
     */
    @PostMapping(value = "/{campaignId}/voice/{sessionId}",
                 produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> aiVoice(
            @PathVariable UUID campaignId,
            @PathVariable UUID sessionId) {

        log.info("aiVoice() — campaignId={} sessionId={}", campaignId, sessionId);

        var gatherUrl = "/webhook/campaign/" + campaignId + "/gather/" + sessionId;
        var twiml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Pause length="1"/>
                    <Say voice="alice" language="en-IN">
                        Hello! This is an automated call from our recruitment team.
                        We have an exciting job opportunity that matches your profile
                        and would love to share more details with you.
                    </Say>
                    <Gather numDigits="1" timeout="8" action="%s" method="POST">
                        <Say voice="alice" language="en-IN">
                            Press 1 if you are interested in hearing more about this opportunity.
                            Press 2 if you would like to be removed from our calling list.
                        </Say>
                    </Gather>
                    <Say voice="alice" language="en-IN">
                        We did not receive your response.
                        Our team will reach out to you again soon. Thank you. Goodbye!
                    </Say>
                </Response>
                """.formatted(gatherUrl);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
    }

    /**
     * Gather webhook — called by Twilio with the lead's keypad response.
     * Updates the lead status in hr-lead-service based on the response.
     *
     * <ul>
     *   <li>1 → INTERESTED — recruiter will follow up</li>
     *   <li>2 → NOT_INTERESTED — removed from active calling</li>
     * </ul>
     */
    @PostMapping(value = "/{campaignId}/gather/{sessionId}",
                 produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> aiGather(
            @PathVariable UUID campaignId,
            @PathVariable UUID sessionId,
            @RequestParam(value = "Digits", defaultValue = "") String digits) {

        log.info("aiGather() — campaignId={} sessionId={} digits='{}'",
                campaignId, sessionId, digits);

        String twiml;

        if ("1".equals(digits)) {
            // Lead is interested — update status and thank them
            updateLeadStatus(sessionId, "INTERESTED");
            twiml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                        <Say voice="alice" language="en-IN">
                            Thank you for your interest!
                            One of our recruitment specialists will contact you very soon
                            with complete details about this exciting opportunity.
                            Have a wonderful day!
                        </Say>
                        <Hangup/>
                    </Response>
                    """;
        } else if ("2".equals(digits)) {
            // Lead wants to opt out
            updateLeadStatus(sessionId, "NOT_INTERESTED");
            twiml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                        <Say voice="alice" language="en-IN">
                            You have been removed from our calling list.
                            We apologize for any inconvenience and wish you all the best.
                            Goodbye!
                        </Say>
                        <Hangup/>
                    </Response>
                    """;
        } else {
            twiml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Response>
                        <Say voice="alice" language="en-IN">
                            We did not receive a valid response.
                            Our team will follow up with you shortly. Goodbye!
                        </Say>
                        <Hangup/>
                    </Response>
                    """;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
    }

    /**
     * Status callback — Twilio posts call lifecycle events here.
     * Delegates to {@link CallService#updateStatus} which triggers the campaign slot release.
     */
    @PostMapping("/{campaignId}/status/{sessionId}")
    public ResponseEntity<Void> campaignStatus(
            @PathVariable UUID campaignId,
            @PathVariable UUID sessionId,
            @RequestParam(value = "CallStatus",   defaultValue = "") String twilioStatus,
            @RequestParam(value = "CallDuration", required = false) Integer duration) {

        log.info("campaignStatus() — campaignId={} sessionId={} status={}",
                campaignId, sessionId, twilioStatus);

        var status = CallStatus.fromTwilio(twilioStatus);
        callService.updateStatus(sessionId, status, duration);
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void updateLeadStatus(UUID sessionId, String newStatus) {
        sessionRepo.findById(sessionId).ifPresent(session -> {
            log.info("Updating lead {} status to {} (AI campaign response)",
                    session.getLeadId(), newStatus);
            leadClient.updateLeadStatus(session.getLeadId(), session.getTenantId(), newStatus);
        });
    }
}