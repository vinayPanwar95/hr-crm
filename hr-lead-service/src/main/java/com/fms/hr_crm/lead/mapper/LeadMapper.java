package com.fms.hr_crm.lead.mapper;

import com.fms.hr_crm.lead.model.dto.LeadRequest;
import com.fms.hr_crm.lead.model.dto.LeadResponse;
import com.fms.hr_crm.lead.model.entity.Lead;
import org.springframework.stereotype.Component;

@Component
public class LeadMapper {

    public Lead toEntity(LeadRequest request) {
        return Lead.builder()
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .company(request.company())
                .positionRequired(request.positionRequired())
                .source(request.source())
                .recruiterId(request.recruiterId())
                .build();
    }

    public LeadResponse toResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getTenantId(),
                lead.getName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getCompany(),
                lead.getPositionRequired(),
                lead.getStatus(),
                lead.getSource(),
                lead.getRecruiterId(),
                lead.getAiScore(),
                lead.getAiLabel(),
                lead.getLastContactedAt(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }
}