---
name: java-service
description: >
  Use this skill when creating a new Spring Boot microservice in the HR CRM project.
  Triggers: "add a new service", "create a microservice", "new module", 
  "scaffold a service". Also use when the user references a service that doesn't
  exist yet in the hr-crm/ directory.
---

## What this skill does
Creates a fully wired Spring Boot microservice following HR CRM conventions.

## Steps to follow — in this exact order

1. Create the Maven module directory: `hr-crm/hr-{name}-service/`

2. Create `pom.xml` inheriting from parent:
```xml

        com.fms
        hr-crm-parent
        0.0.1-SNAPSHOT
   
   hr-{name}-service
```

3. Add the module to `/hr-crm/pom.xml` modules list

4. Create the standard package structure:
   src/main/java/com/hrcrm/{name}/
   ├── {Name}ServiceApplication.java
   ├── controller/
   ├── service/
   ├── repository/
   ├── model/entity/
   ├── model/dto/
   └── config/
   src/main/resources/
   ├── application.yml
   └── db/migration/V1__init.sql
   src/test/java/com/hrcrm/{name}/
   └── integration/
5. Set server.port in application.yml — check existing services for next available port

6. Register the service in the API Gateway routes

7. Run `./mvnw compile -pl hr-{name}-service` to verify it builds

## What NOT to do
- Do not use `spring-boot-starter-web` AND `spring-boot-starter-webflux` together
- Do not create a service without a health endpoint (`/actuator/health`)
- Do not forget to add the new module to the parent POM

## Output to confirm when done
Show the directory tree of the new service and the gateway route you added.