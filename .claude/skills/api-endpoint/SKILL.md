---
name: api-endpoint
description: >
  Use this skill when adding a new REST API endpoint to any HR CRM service.
  Triggers: "add endpoint", "add API", "expose {action} via REST", "create a route
  for", "add a POST/GET/PUT/DELETE for", "the frontend needs to call {action}".
---

## What this skill produces
1. Controller method with proper mapping + validation
2. Service method with business logic
3. Request/Response DTOs if not existing
4. Integration test for the endpoint

## Endpoint design rules

### URL conventions
- Collections: GET /api/leads
- Single item: GET /api/leads/{id}
- Actions: POST /api/leads/{id}/convert  (verb in path for state changes)
- Nested: GET /api/leads/{id}/notes
- Bulk: POST /api/leads/import

### HTTP status codes we use
- 200 OK — successful GET, PUT
- 201 Created — successful POST that creates a resource (return Location header)
- 204 No Content — successful DELETE
- 400 Bad Request — validation failure (return field errors)
- 404 Not Found — resource doesn't exist
- 409 Conflict — duplicate detection hit
- 422 Unprocessable Entity — business rule violation

### Controller method template
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ResponseEntity<LeadResponse> createLead(
        @Valid @RequestBody LeadRequest request,
        @AuthenticationPrincipal UserPrincipal user) {
    
    LeadResponse response = leadService.create(request, user.getTenantId());
    URI location = URI.create("/api/leads/" + response.id());
    return ResponseEntity.created(location).body(response);
}
```

### Validation
Always use @Valid on @RequestBody.
Put constraints on the DTO, not the controller or service.
Return 400 with field-level errors via GlobalExceptionHandler.

## Integration test template
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class LeadControllerIT {
    
    @Test
    void createLead_validRequest_returns201() {
        // given
        var request = LeadRequest.builder()...build();
        
        // when / then
        mockMvc.perform(post("/api/leads")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("NEW"));
    }
}
```