# hr-ai-service -Claude AI Integration Service

## This service's job
Handles ALL interactions with the Claude API. Other services call this via REST.
Never call the Claude API from any other service — route through here.

## Claude API patterns used here
- `/api/ai/score-lead` — Basic messages + system prompt → lead scoring
- `/api/ai/summarize-call` — Streaming → real-time call summary SSE
- `/api/ai/chat` — Multi-turn conversation → recruiter assistant
- `/api/ai/automate` — Tool use → CRM automation commands
- `/api/ai/parse-resume` — Vision → resume structured extraction
- `/api/ai/batch-score` — Batch API → bulk overnight lead scoring

## Claude model to use
Always use `claude-opus-4-5` unless the task is simple (then `claude-haiku-4-5-20251001`).
For extended thinking tasks (candidate matching), use `claude-opus-4-5` with thinking

## Response handling
- All Claude responses must be logged to the `ai_request_log` table (for audit)
- If Claude returns a tool_use block, ALWAYS complete the tool cycle — never abandon mid-cycle
- Streaming responses: use Spring WebFlux Flux<ServerSentEvent>


## When you add a new AI feature
1. Add the endpoint to `AiController.java`
2. Create a dedicated service class (e.g. `ResumeParserService.java`)
3. Add the system prompt as a constant in `SystemPrompts.java`
4. Write a WireMock integration test
5. Update the README section "Available AI endpoints"