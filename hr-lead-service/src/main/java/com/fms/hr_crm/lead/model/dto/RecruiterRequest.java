package com.fms.hr_crm.lead.model.dto;

/**
 * Payload for creating or updating a recruiter.
 * {@code loginPassword} is a mandatory one-time password for the recruiter's calling-service login.
 * It is NOT stored in hr-lead-service — it is forwarded to hr-calling-service only.
 */
public record RecruiterRequest(String name, String email, String phone, String loginPassword) {}