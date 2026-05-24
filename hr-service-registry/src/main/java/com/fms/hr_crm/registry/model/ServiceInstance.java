package com.fms.hr_crm.registry.model;

import java.time.Instant;

/**
 * Represents a registered service instance in the registry.
 *
 * @param name         logical service name (e.g. "hr-lead-service")
 * @param url          base URL of this instance (e.g. "http://localhost:8081")
 * @param status       UP or DOWN
 * @param registeredAt when the service first registered in this registry lifecycle
 * @param lastSeen     timestamp of the most recent register or heartbeat call
 */
public record ServiceInstance(
        String name,
        String url,
        String status,
        Instant registeredAt,
        Instant lastSeen
) {}