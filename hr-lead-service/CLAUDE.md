# hr-lead-service — Lead Management Service


## What this service owns
Everything about leads: creation, pipeline progression, assignment,
follow-ups, tags, notes, duplicate detection, CSV import.

This service does NOT: score leads (→ hr-ai-service),
make calls (→ hr-calling-service), send WhatsApp (→ hr-whatsapp-service).

## Package structure
com.fms.hr_crm.lead   
├── controller/      LeadController, FollowUpController, ImportController  
├── service/         LeadService, FollowUpService, DuplicateDetector, CsvImportService  
├── repository/      LeadRepository, FollowUpRepository, LeadNoteRepository  
├── model/  
│   ├── entity/      Lead, FollowUp, LeadNote, LeadTag  
│   └── dto/         LeadRequest, LeadResponse, LeadSummary, PipelineStats  
├── mapper/          LeadMapper (MapStruct)  
├── exception/       LeadNotFoundException, DuplicateLeadException  
├── config/          SecurityConfig, WebConfig  
└── scheduler/       FollowUpReminderJob  

## Lead status flow — ONLY valid transitions
NEW → CONTACTED → INTERESTED → CONVERTED
NEW → CONTACTED → NOT_INTERESTED → CLOSED
Any status → CLOSED (can always close)

## Key business rules (understand before editing)
- Duplicate = same phone OR same email within same tenant
- Auto-assign: round-robin across active recruiters if recruiter_id not provided
- A lead can have many notes, many tags, one active follow-up at a time
- CSV import is async — returns a job_id, not results directly

## Database schema (schema: lead_svc)
Tables: leads, lead_notes, lead_tags, follow_ups, import_jobs
All timestamps in UTC, column type TIMESTAMPTZ

## When you modify LeadService.java
- LeadService has 23 nit tests — run them: `./mvnw test -pl hr-lead-service -Dtest=LeadServiceTest`
- If you add a new public method, add a test before I review
- If you change status transition logic, update LeadStatusTest too

## MUST DO 
- create a theamleaf UI  for the lead
- service and add it to the parent pom.
- 