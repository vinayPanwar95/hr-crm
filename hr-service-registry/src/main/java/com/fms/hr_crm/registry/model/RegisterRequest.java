package com.fms.hr_crm.registry.model;

/**
 * Payload sent by a service when it registers or re-registers.
 *
 * @param name logical service name, must match {@code spring.application.name}
 * @param url  reachable base URL for this instance
 */
public record RegisterRequest(String name, String url) {}