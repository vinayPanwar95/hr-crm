package com.fms.hr_crm.lead.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP client for hr-calling-service internal endpoints.
 * Uses Spring's built-in {@code @HttpExchange} — no Spring Cloud required,
 * compatible with Spring Boot 4.x.
 */
@HttpExchange
public interface CallingServiceClient {

    @PostExchange("/api/internal/recruiters")
    void provisionRecruiter(@RequestBody ProvisionRecruiterRequest request);
}