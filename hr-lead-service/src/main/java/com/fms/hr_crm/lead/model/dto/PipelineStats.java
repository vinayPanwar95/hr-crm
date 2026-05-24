package com.fms.hr_crm.lead.model.dto;

import java.util.Map;

public record PipelineStats(Map<String, Long> countByStatus) {}