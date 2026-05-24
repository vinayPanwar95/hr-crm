package com.fms.hr_crm.lead.exception;

public class DuplicateLeadException extends RuntimeException {

    public DuplicateLeadException(String phone, String email) {
        super("A lead already exists with phone: " + phone + " or email: " + email);
    }
}