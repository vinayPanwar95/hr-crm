package com.fms.hr_crm.lead.service;

import com.fms.hr_crm.lead.exception.DuplicateLeadException;
import com.fms.hr_crm.lead.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DuplicateDetector {

    private final LeadRepository leadRepository;

    /**
     * Throws {@link DuplicateLeadException} if a non-deleted lead already exists
     * for this tenant with the same phone or email.
     */
    public void assertNoDuplicate(UUID tenantId, String phone, String email) {
        if (leadRepository.findByTenantIdAndPhone(tenantId, phone).isPresent()) {
            throw new DuplicateLeadException(phone, email);
        }
        if (email != null && leadRepository.findByTenantIdAndEmail(tenantId, email).isPresent()) {
            throw new DuplicateLeadException(phone, email);
        }
    }
}