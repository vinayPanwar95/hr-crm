package com.fms.hr_crm.lead.exception;

import java.util.UUID;

public class LeadNotFoundException extends RuntimeException {

    public LeadNotFoundException(UUID id) {
        super("Lead not found with id: " + id);
    }
}