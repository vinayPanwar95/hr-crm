package com.fms.hr_crm.lead.service;

import com.fms.hr_crm.lead.model.dto.ImportResult;
import com.fms.hr_crm.lead.model.dto.LeadRequest;
import com.fms.hr_crm.lead.model.entity.LeadSource;
import com.fms.hr_crm.lead.repository.RecruiterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parses a CSV/Excel-exported CSV file and bulk-creates leads for the given tenant.
 *
 * <p>Expected CSV format (header row required):
 * <pre>name,phone,email,company,positionRequired,source,recruiter_id</pre>
 * <ul>
 *   <li>{@code source} must match a {@link LeadSource} enum value or be left blank.</li>
 *   <li>{@code recruiter_id} is optional. When provided, the recruiter must exist and be
 *       active for this tenant; otherwise the lead is auto-assigned via round-robin.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final LeadService leadService;
    private final RecruiterRepository recruiterRepository;

    public ImportResult importCsv(MultipartFile file, UUID tenantId) {
        int total = 0, created = 0;
        var errors = new ArrayList<String>();

        try (var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();   // skip header row
            if (header == null) {
                return new ImportResult(0, 0, 0, List.of("File is empty"));
            }
            // Strip UTF-8 BOM that Excel adds (\uFEFF)
            if (header.startsWith("\uFEFF")) header = header.substring(1);

            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                line = line.stripTrailing().replace("\r", ""); // strip Windows CR
                if (line.isBlank()) continue;                  // skip empty lines

                // skip lines where every column is empty (e.g. ",,,,,,")
                var cols = splitCsv(line);
                if (allBlank(cols)) continue;

                total++;
                try {
                    var req = parseCols(cols, row, tenantId);
                    leadService.create(req, tenantId);
                    created++;
                } catch (Exception ex) {
                    log.warn("CSV row {} skipped: {}", row, ex.getMessage());
                    errors.add("Row " + row + ": " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("CSV import failed: {}", ex.getMessage());
            return new ImportResult(total, created, total - created,
                    List.of("Failed to read file: " + ex.getMessage()));
        }

        return new ImportResult(total, created, total - created, errors);
    }

    private LeadRequest parseCols(String[] cols, @SuppressWarnings("unused") int row, UUID tenantId) {
        if (cols.length < 2) {
            throw new IllegalArgumentException("at least name and phone columns are required");
        }

        var name         = col(cols, 0);
        var phone        = col(cols, 1);
        var email        = col(cols, 2);
        var company      = col(cols, 3);
        var position     = col(cols, 4);
        var sourceStr    = col(cols, 5);
        var recruiterStr = col(cols, 6);

        if (name == null)  throw new IllegalArgumentException("name is blank");
        if (phone == null) throw new IllegalArgumentException("phone is blank");

        LeadSource source = null;
        if (sourceStr != null) {
            try {
                source = LeadSource.valueOf(sourceStr.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown source '" + sourceStr + "'");
            }
        }

        UUID recruiterId = resolveRecruiterId(recruiterStr, tenantId, row);
        return new LeadRequest(name, phone, email, company, position, source, recruiterId);
    }

    /**
     * Validates the recruiter_id from the CSV against the recruiters table.
     * Returns the ID if the recruiter exists and is active for this tenant,
     * or null (auto-assign) if blank, invalid UUID, or not found.
     */
    private UUID resolveRecruiterId(String recruiterStr, UUID tenantId, int row) {
        if (recruiterStr == null) return null;  // blank → auto-assign

        UUID candidateId;
        try {
            candidateId = UUID.fromString(recruiterStr.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Row {}: recruiter_id '{}' is not a valid UUID — will auto-assign", row, recruiterStr);
            return null;
        }

        boolean valid = recruiterRepository.findById(candidateId)
                .filter(r -> r.getTenantId().equals(tenantId) && r.isActive())
                .isPresent();

        if (!valid) {
            log.warn("Row {}: recruiter_id {} not found or inactive for tenant {} — will auto-assign",
                    row, candidateId, tenantId);
            return null;
        }

        return candidateId;
    }

    /** Returns true when every column in the row is empty — used to skip footer/blank rows. */
    private boolean allBlank(String[] cols) {
        for (var c : cols) {
            if (c != null && !c.isBlank()) return false;
        }
        return true;
    }

    private String[] splitCsv(String line) {
        List<String> result = new ArrayList<>();
        var sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }

    private String col(String[] cols, int idx) {
        if (idx >= cols.length) return null;
        var v = cols[idx].trim();
        return v.isEmpty() ? null : v;
    }
}