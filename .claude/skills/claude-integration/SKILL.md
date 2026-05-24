---
name: claude-integration
description: >
  Use this skill whenever adding a new Claude API feature to hr-ai-service.
  Triggers: "add AI feature", "integrate Claude", "new AI endpoint", 
  "call Claude API", "AI-powered", "use Claude for".
---

## Environment constraints
- The ONLY class that calls Anthropic's API: `ClaudeApiClient.java`
- All new features must go through it — never create a second HTTP client
- The model string must come from config, never hardcoded in service classes
- Every API call must be logged: call `aiAuditService.log(request, response)`

## Checklist for adding a new Claude feature

### 1. Define the system prompt
Add it as a constant in `SystemPrompts.java`:
```java
public static final String YOUR_FEATURE_PROMPT = """
    You are ...
    Always respond with ...
    """;
```

### 2. Create the service class
```java
@Service
@RequiredArgsConstructor
public class YourFeatureService {
    private final ClaudeApiClient claude;
    private final AiAuditService audit;
    
    public YourOutputType process(YourInputType input) {
        // build request
        // call claude
        // parse response
        // audit log
        // return result
    }
}
```

### 3. Add controller endpoint
Map it in `AiController.java` — don't create a new controller.

### 4. Write the WireMock test
Copy from `LeadScoringServiceTest.java` as the template.

## Response parsing patterns
- JSON output → use `@JsonProperty` annotated records, parse with ObjectMapper
- Free text → return the raw string
- Streaming → return `Flux<ServerSentEvent<String>>`
- Tool use → must complete the full 2-call cycle (see `CrmToolUseService.java`)