package com.fms.hr_crm.calling.exception;

import java.util.UUID;

public class CallSessionNotFoundException extends RuntimeException {
    public CallSessionNotFoundException(UUID id) {
        super("Call session not found: " + id);
    }
}