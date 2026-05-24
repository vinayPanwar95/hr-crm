package com.fms.hr_crm.calling.service;

import com.fms.hr_crm.calling.client.LeadServiceClient;
import com.fms.hr_crm.calling.model.entity.CallStatus;
import com.fms.hr_crm.calling.model.entity.CampaignStatus;
import com.fms.hr_crm.calling.repository.AiCampaignRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates AI campaign execution.
 *
 * <p>When a campaign starts, it fetches the target lead IDs, then runs up to
 * {@code agentCount} parallel virtual threads — each picks a lead, fires a call
 * via {@link CallService#initiateAiCampaignCall}, and waits for the next slot.
 *
 * <p>Slots are freed when a call reaches a terminal status. The status change
 * arrives via a {@link CallStatusUpdatedEvent} published by {@link CallService}
 * AFTER the DB transaction commits ({@code @TransactionalEventListener(AFTER_COMMIT)}).
 *
 * <p>All in-progress state lives in memory ({@code runtimes}). A service restart
 * clears in-progress campaigns — the admin must restart them manually.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCampaignOrchestrator {

    private static final Set<CallStatus> TERMINAL = Set.of(
            CallStatus.COMPLETED, CallStatus.FAILED,
            CallStatus.NO_ANSWER,  CallStatus.BUSY, CallStatus.CANCELED);

    private final AiCampaignRepository campaignRepo;
    private final CallService          callService;
    private final LeadServiceClient    leadClient;

    /** Cached thread pool for campaign dial tasks. */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** Live state per running campaign. */
    private final ConcurrentHashMap<UUID, CampaignRuntime> runtimes = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Starts a campaign immediately.
     * Fetches the lead queue, marks the campaign RUNNING, and fires the first wave of calls.
     *
     * @throws IllegalStateException if the campaign is not in DRAFT or SCHEDULED state
     */
    public void start(UUID campaignId) {
        var campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            log.warn("start() — campaign {} is already RUNNING", campaignId);
            return;
        }

        log.info("Starting AI campaign: id={} name='{}' agents={} target={}",
                campaignId, campaign.getName(), campaign.getAgentCount(),
                campaign.getTargetLeadStatus());

        // Fetch leads from lead-service
        var leadIds = leadClient.getLeadIdsForCampaign(
                campaign.getTenantId(), campaign.getTargetLeadStatus(), 1000);

        if (leadIds.isEmpty()) {
            log.warn("start() — no leads found for campaign {} (status={}). Marking COMPLETED.",
                    campaignId, campaign.getTargetLeadStatus());
            campaignRepo.markTerminated(campaignId, CampaignStatus.COMPLETED, Instant.now());
            return;
        }

        // Persist RUNNING state atomically
        campaignRepo.markRunning(campaignId, CampaignStatus.RUNNING, Instant.now(), leadIds.size());
        log.info("Campaign {} RUNNING — {} leads in queue", campaignId, leadIds.size());

        // Create in-memory runtime
        var runtime = new CampaignRuntime(campaignId, campaign.getTenantId(),
                campaign.getAgentCount(), leadIds);
        runtimes.put(campaignId, runtime);

        // Fire the initial wave of parallel calls
        int slots = Math.min(campaign.getAgentCount(), leadIds.size());
        for (int i = 0; i < slots; i++) {
            scheduleNextCall(campaignId);
        }
    }

    /**
     * Stops a running campaign immediately.
     * Active in-flight calls are allowed to complete naturally; no new calls are fired.
     */
    public void stop(UUID campaignId) {
        log.info("Stopping AI campaign: id={}", campaignId);
        var runtime = runtimes.remove(campaignId);
        if (runtime != null) {
            runtime.stop();
        }
        campaignRepo.markTerminated(campaignId, CampaignStatus.STOPPED, Instant.now());
        log.info("Campaign {} STOPPED", campaignId);
    }

    // ── Event listener ────────────────────────────────────────────────────────

    /**
     * Reacts to a call reaching a terminal state.
     * Fires after the DB transaction commits so the updated session is visible to all readers.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCallTerminated(CallStatusUpdatedEvent event) {
        if (event.campaignId() == null) return;

        var runtime = runtimes.get(event.campaignId());
        if (runtime == null) return; // Campaign was stopped or not managed by this instance

        log.debug("onCallTerminated() — campaign={} session={} status={}",
                event.campaignId(), event.sessionId(), event.status());

        runtime.releaseSlot(event.sessionId());

        // Update DB counters asynchronously (each modifying call opens its own transaction)
        executor.submit(() -> {
            if (TERMINAL.contains(event.status())) {
                if (event.status() == CallStatus.COMPLETED) {
                    campaignRepo.incrementCompletedCount(event.campaignId());
                } else {
                    campaignRepo.incrementFailedCount(event.campaignId());
                }
            }
        });

        if (!runtime.isRunning()) return;

        if (runtime.pendingLeads.isEmpty() && runtime.activeCount.get() == 0) {
            completeCampaign(event.campaignId());
        } else {
            scheduleNextCall(event.campaignId());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void scheduleNextCall(UUID campaignId) {
        executor.submit(() -> {
            var runtime = runtimes.get(campaignId);
            if (runtime == null || !runtime.isRunning()) return;

            var leadId = runtime.pendingLeads.poll();
            if (leadId == null) {
                // Queue is empty — if no active calls remain, complete the campaign
                if (runtime.activeCount.get() == 0) {
                    completeCampaign(campaignId);
                }
                return;
            }

            runtime.activeCount.incrementAndGet();
            try {
                campaignRepo.incrementCalledCount(campaignId);
                var session = callService.initiateAiCampaignCall(runtime.tenantId, leadId, campaignId);
                runtime.activeSessions.add(session.id());
                log.debug("scheduleNextCall() — fired call for leadId={} session={}", leadId, session.id());
            } catch (Exception ex) {
                log.warn("scheduleNextCall() — failed to initiate call leadId={}: {}", leadId, ex.getMessage());
                runtime.activeCount.decrementAndGet();
                campaignRepo.incrementFailedCount(campaignId);
                // Try next lead immediately
                scheduleNextCall(campaignId);
            }
        });
    }

    private void completeCampaign(UUID campaignId) {
        if (runtimes.remove(campaignId) != null) {
            campaignRepo.markTerminated(campaignId, CampaignStatus.COMPLETED, Instant.now());
            log.info("Campaign {} COMPLETED — all leads called", campaignId);
        }
    }

    @PreDestroy
    void shutdown() {
        log.info("AiCampaignOrchestrator shutting down — {} campaigns in flight", runtimes.size());
        runtimes.forEach((id, rt) -> rt.stop());
        executor.shutdownNow();
    }

    // ── Inner runtime state ───────────────────────────────────────────────────

    private static final class CampaignRuntime {

        final UUID                      campaignId;
        final UUID                      tenantId;
        final int                       agentCount;
        final ConcurrentLinkedQueue<UUID> pendingLeads;
        final Set<UUID>                 activeSessions = ConcurrentHashMap.newKeySet();
        final AtomicInteger             activeCount    = new AtomicInteger(0);
        volatile boolean                running        = true;

        CampaignRuntime(UUID campaignId, UUID tenantId, int agentCount, java.util.List<UUID> leadIds) {
            this.campaignId   = campaignId;
            this.tenantId     = tenantId;
            this.agentCount   = agentCount;
            this.pendingLeads = new ConcurrentLinkedQueue<>(leadIds);
        }

        boolean isRunning() { return running; }

        void stop() {
            running = false;
            pendingLeads.clear();
        }

        void releaseSlot(UUID sessionId) {
            activeSessions.remove(sessionId);
            activeCount.decrementAndGet();
        }
    }
}