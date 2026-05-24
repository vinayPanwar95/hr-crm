package com.fms.hr_crm.calling.service;

import com.fms.hr_crm.calling.model.entity.CallStatus;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Published by {@link CallService} whenever a call session reaches a terminal state
 * (COMPLETED, FAILED, NO_ANSWER, BUSY, CANCELED).
 *
 * <p>The {@link AiCampaignOrchestrator} listens to this event to release an agent
 * slot and schedule the next lead call.
 *
 * <p>Published inside a {@code @Transactional} method so the listener can use
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to safely read the
 * committed data.
 */
public class CallStatusUpdatedEvent extends ApplicationEvent {

    private final UUID       sessionId;
    private final UUID       campaignId;  // null when call is not part of a campaign
    private final CallStatus status;
    private final UUID       leadId;

    public CallStatusUpdatedEvent(Object source, UUID sessionId, UUID campaignId,
                                  CallStatus status, UUID leadId) {
        super(source);
        this.sessionId  = sessionId;
        this.campaignId = campaignId;
        this.status     = status;
        this.leadId     = leadId;
    }

    public UUID       sessionId()  { return sessionId; }
    public UUID       campaignId() { return campaignId; }
    public CallStatus status()     { return status; }
    public UUID       leadId()     { return leadId; }
}