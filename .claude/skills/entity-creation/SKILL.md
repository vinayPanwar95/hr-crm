---
name: entity-creation
description: >
  Use this skill when creating a new JPA entity, database table, or domain model
  in the HR CRM project. Triggers: "add entity", "create table", "new model",
  "add a {noun} to the database", "I need to store {noun}", "add {noun} to leads".
  Also triggers when a new business concept needs persistence.
---

## What this skill produces
1. A JPA entity class in model/entity/
2. A Flyway migration SQL file
3. A Spring Data repository interface
4. A DTO (Request + Response) in model/dto/
5. MapStruct mapper entry

## Step-by-step checklist

### Step 1 — Design the table first
Write the SQL CREATE TABLE before any Java code.
Ask yourself:
- What's the primary key? (always BIGSERIAL, never UUID for performance)
- What's nullable vs not null?
- What foreign keys exist?
- What indexes are needed? (all FK columns + frequently filtered columns)

### Step 2 — Create the Flyway migration
File: `src/main/resources/db/migration/V{next}__add_{tablename}.sql`
Get the next version number by looking at existing files in that directory.

Template:
```sql
CREATE TABLE lead_svc.{tablename} (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    -- your columns here
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_{tablename}_tenant ON lead_svc.{tablename}(tenant_id);
CREATE INDEX idx_{tablename}_{fk_col} ON lead_svc.{tablename}({fk_col});
```

### Step 3 — Create the JPA entity
Must extend BaseEntity. Use the entity conventions from the entity package CLAUDE.md.

### Step 4 — Create the repository
Extend JpaRepository<Entity, Long>.
Add query methods for the most common access patterns now.

### Step 5 — Create DTOs
- {Entity}Request — what the client sends (input validation with @NotNull etc.)
- {Entity}Response — what we return to the client (never the entity itself)

### Step 6 — Verify
Run: `./mvnw compile -pl hr-lead-service`
If it fails, fix before moving on.

## Common mistakes to avoid
- Forgetting to increment Flyway version number → checksum mismatch on startup
- Using cascade = ALL → silent deletes
- Missing tenant_id → multi-tenancy breaks
- Returning entity from controller → lazy loading exceptions, over-exposure