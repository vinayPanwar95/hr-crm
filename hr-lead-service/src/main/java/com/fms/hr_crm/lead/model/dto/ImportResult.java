package com.fms.hr_crm.lead.model.dto;

import java.util.List;

public record ImportResult(
        int total,
        int created,
        int skipped,
        List<String> errors
) {}