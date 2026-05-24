# HR CRM Microservices - Module Structure

## Overview
All 7 modules for the HR CRM microservices project have been created with complete package structures.

## Modules Created

### 1. hr-common (Shared Library)
- **Purpose**: Shared utilities, models, and configuration
- **Port**: N/A (library)
- **Path**: `C:\Users\VinayKumar4\Projects\hr-crm\hr-common`
- **Package**: `com.fms.hr_crm.common`
- **Dependencies**: Spring Boot Core, Lombok
- **Note**: Skip executable JAR (skip=true in maven-boot-plugin)

### 2. hr-gateway (API Gateway)
- **Purpose**: Spring Cloud Gateway with JWT validation
- **Port**: 8080
- **Path**: `C:\Users\VinayKumar4\Projects\hr-crm\hr-gateway`
- **Package**: `com.fms.hr_crm.gateway`
- **Dependencies**: Spring Cloud Gateway, Security, hr-common
- **Routes Configured**:
  - `/api/leads/**` → hr-lead-service (8081)
  - `/api/ai/**` → hr-ai-service (8082)
  - `/api/calls/**` → hr-calling-service (8083)
  - `/api/whatsapp/**` → hr-whatsapp-service (8084)
  - `/api/ops/**` → hr-ops-service (8085)

### 3. hr-lead-service (Lead Management)
- **Purpose**: Lead CRUD and pipeline management
- **Port**: 8081
- **Path**: `C:\Users\VinayKumar4\Projects\hr-crm\hr-lead-service`
- **Package**: `com.fms.hr_crm.lead`
- **Database**: PostgreSQL (hr_lead_db)
- **Dependencies**: Spring Data JPA, Security, Flyway, PostgreSQL, hr-common

### 4. hr-ai-service (AI Integration)
- **Purpose**: Claude API integration for lead scoring, call summaries, resume parsing
- **Port**: 8082
- **Path**: `C:\Users\VinayKumar4\Projects\hr-crm\hr-ai-service`
- **Package**: `com.fms.hr_crm.ai`
- **Dependencies**: Spring WebFlux, Security, Anthropic Client, hr-common
- **Env Vars**: `ANTHROPIC_API_KEY`

### 5. hr-calling-service (Call Management)
- **Purpose**: Twilio integration and call masking
- **Port**: 8083
- **Path**: `C:\Users\VinayKumar4\Projects\hr-crm\hr-calling-service`
- **Package**: `com.fms.hr_crm.calling`
- **Database**: PostgreSQL (hr_calling_db)
- **Dependencies**: Spring Data JPA, Security, Twilio SDK, PostgreSQL, hr-common
- **Env Vars**: `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`

### 6. hr-whatsapp-service (WhatsApp Integration)
- **Purpose**: Meta WhatsApp API integration for messaging
- **Port**: 8084
- **Path**: `C:\Users\VinayKumar4\Projects\hr-crm\hr-whatsapp-service`
- **Package**: `com.fms.hr_crm.whatsapp`
- **Database**: PostgreSQL (hr_whatsapp_db)
- **Dependencies**: Spring Data JPA, Security, PostgreSQL, hr-common
- **Env Vars**: `WHATSAPP_TOKEN`, `WHATSAPP_BUSINESS_ACCOUNT_ID`

### 7. hr-ops-service (HR Operations)
- **Purpose**: Employee management, payslips, and leave management
- **Port**: 8085
- **Path**: `C:\Users\VinayKumar4\Projects\hr-crm\hr-ops-service`
- **Package**: `com.fms.hr_crm.ops`
- **Database**: PostgreSQL (hr_ops_db)
- **Dependencies**: Spring Data JPA, Security, Flyway, PostgreSQL, hr-common

## Directory Structure

```
hr-crm/
├── hr-common/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fms/hr_crm/common/util/
│       └── main/resources/
│
├── hr-gateway/
│   ├── pom.xml
│   ├── src/main/java/com/fms/hr_crm/gateway/GatewayApplication.java
│   ├── src/main/resources/application.yaml
│   └── src/test/java/
│
├── hr-lead-service/
│   ├── pom.xml
│   ├── src/main/java/com/fms/hr_crm/lead/LeadServiceApplication.java
│   ├── src/main/resources/application.yaml
│   └── src/test/java/
│
├── hr-ai-service/
│   ├── pom.xml
│   ├── src/main/java/com/fms/hr_crm/ai/AiServiceApplication.java
│   ├── src/main/resources/application.yaml
│   └── src/test/java/
│
├── hr-calling-service/
│   ├── pom.xml
│   ├── src/main/java/com/fms/hr_crm/calling/CallingServiceApplication.java
│   ├── src/main/resources/application.yaml
│   └── src/test/java/
│
├── hr-whatsapp-service/
│   ├── pom.xml
│   ├── src/main/java/com/fms/hr_crm/whatsapp/WhatsappServiceApplication.java
│   ├── src/main/resources/application.yaml
│   └── src/test/java/
│
├── hr-ops-service/
│   ├── pom.xml
│   ├── src/main/java/com/fms/hr_crm/ops/OpsServiceApplication.java
│   ├── src/main/resources/application.yaml
│   └── src/test/java/
│
└── pom.xml (parent)
```

## Parent POM Configuration

- **Spring Boot**: 4.0.6
- **Spring Cloud**: 2024.0.0
- **Java**: 21
- **Build Plugins**: Maven Compiler, Spring Boot Maven Plugin
- **Annotation Processing**: Lombok configured for both compile and test phases

## Environment Variables Required
```
# Common
DB_URL=jdbc:postgresql://localhost:5432/hr_crm
DB_USER=postgres
DB_PASS=postgres

# AI Service
ANTHROPIC_API_KEY=your_api_key

# Calling Service
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_FROM_NUMBER=+1234567890

# WhatsApp Service
WHATSAPP_TOKEN=your_token
WHATSAPP_BUSINESS_ACCOUNT_ID=your_account_id
```

## Next Steps

1. **Build the project**: `mvn clean install`
2. **Create databases**: Create PostgreSQL databases for each service
3. **Run migrations**: Flyway will handle schema creation
4. **Start services**: 
   - Start gateway first: `java -jar hr-gateway/target/hr-gateway-0.0.1-SNAPSHOT.jar`
   - Start dependent services on their respective ports
5. **Add integration tests**: Implement @SpringBootTest tests with WireMock for external API mocking
6. **Add controllers/repositories**: Implement service-specific logic

## Architecture Notes

- ✅ All modules inherit from parent POM (DRY principle)
- ✅ hr-common used as shared library across all services
- ✅ Each service has its own PostgreSQL database (no shared schema)
- ✅ Gateway routes requests to respective services via load balancing
- ✅ Consistent package naming: `com.fms.hr_crm.<service-name>`
- ✅ Lombok and annotation processors properly configured
- ✅ Spring Cloud BOM imported for version management

