package com.fms.hr_crm.lead.service;

import com.fms.hr_crm.lead.repository.LeadRepository;
import com.fms.hr_crm.lead.repository.RecruiterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assigns leads to recruiters using a round-robin strategy.
 *
 * <p>The recruiter pool is sourced from active recruiters in the DB for this tenant.
 * Returns {@code null} if no active recruiters are available (lead stays unassigned).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecruiterAssigner {

    private final LeadRepository leadRepository;
    private final RecruiterRepository recruiterRepository;

    public UUID nextRoundRobin(UUID tenantId) {
        // Pool = all active recruiters for this tenant from DB
        var pool = recruiterRepository.findByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(r -> r.getId())
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            log.debug("No active recruiters for tenant {} — lead will be unassigned", tenantId);
            return null;
        }

        // Pick recruiter with fewest assigned leads (round-robin by load)
        Map<UUID, Long> counts = leadRepository
                .findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .filter(l -> l.getRecruiterId() != null && pool.contains(l.getRecruiterId()))
                .collect(Collectors.groupingBy(
                        com.fms.hr_crm.lead.model.entity.Lead::getRecruiterId,
                        Collectors.counting()));

        return pool.stream()
                .min((a, b) -> Long.compare(
                        counts.getOrDefault(a, 0L),
                        counts.getOrDefault(b, 0L)))
                .orElse(null);
    }
}