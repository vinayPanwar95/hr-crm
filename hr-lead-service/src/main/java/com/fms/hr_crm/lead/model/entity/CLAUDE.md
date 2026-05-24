# JPA Entities — rules for this package

## Entity conventions (read before adding/editing any entity)
- All entities extend BaseEntity (has id, createdAt, updatedAt, tenantId)
- Use @Column(nullable = false) explicitly — no implicit nullability
- Enums stored as VARCHAR, not ordinal: @Enumerated(EnumType.STRING)
- No bidirectional relationships — always unidirectional, parent owns the FK
- Soft delete only: use @Where(clause = "deleted_at IS NULL"), never hard delete
- No cascade ALL — be explicit: cascade = {PERSIST, MERGE} only

## Why these rules exist
- Ordinal enums break when you reorder the enum
- Bidirectional causes infinite loops in JSON serialization
- Hard deletes break audit trails required for GDPR compliance
- Cascade ALL causes accidental deletes through parent

## When adding a new entity
1. Extend BaseEntity
2. Add Flyway migration V{next}__add_{tablename}.sql
3. Add the repository interface
4. Add to LeadMapper if it needs DTO mapping