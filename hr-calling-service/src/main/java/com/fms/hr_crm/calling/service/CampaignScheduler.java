package com.fms.hr_crm.calling.service;

import com.fms.hr_crm.calling.model.entity.CampaignStatus;
import com.fms.hr_crm.calling.repository.AiCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/**
 * Checks every 30 seconds for campaigns that should auto-start or auto-stop
 * based on their scheduled date and time window.
 *
 * <p>Requires {@code @EnableScheduling} on the application class or a
 * {@code @Configuration} class.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignScheduler {

    private final AiCampaignRepository   campaignRepo;
    private final AiCampaignOrchestrator orchestrator;

    /** Auto-starts SCHEDULED campaigns whose time window has begun. */
    @Scheduled(fixedDelay = 30_000)
    public void checkScheduled() {
        var now     = LocalTime.now();
        var today   = java.time.LocalDate.now();
        var pending = campaignRepo.findByStatusIn(List.of(CampaignStatus.SCHEDULED));

        for (var campaign : pending) {
            if (campaign.getScheduledDate() == null) continue;
            if (!campaign.getScheduledDate().equals(today)) continue;
            if (campaign.getWindowStart() == null) continue;
            if (!now.isBefore(campaign.getWindowStart())) {
                log.info("CampaignScheduler — auto-starting campaign id={} name='{}'",
                        campaign.getId(), campaign.getName());
                try {
                    orchestrator.start(campaign.getId());
                } catch (Exception e) {
                    log.error("CampaignScheduler — failed to auto-start campaign {}: {}",
                            campaign.getId(), e.getMessage());
                }
            }
        }
    }

    /** Auto-stops RUNNING campaigns whose time window has ended. */
    @Scheduled(fixedDelay = 30_000)
    public void checkRunning() {
        var now     = LocalTime.now();
        var running = campaignRepo.findByStatusIn(List.of(CampaignStatus.RUNNING));

        for (var campaign : running) {
            if (campaign.getWindowEnd() == null) continue;
            if (now.isAfter(campaign.getWindowEnd())) {
                log.info("CampaignScheduler — auto-stopping campaign id={} (window ended)",
                        campaign.getId());
                try {
                    orchestrator.stop(campaign.getId());
                } catch (Exception e) {
                    log.error("CampaignScheduler — failed to auto-stop campaign {}: {}",
                            campaign.getId(), e.getMessage());
                }
            }
        }
    }
}