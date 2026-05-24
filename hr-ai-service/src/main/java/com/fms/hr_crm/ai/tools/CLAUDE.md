# /hr-ai-service/src/main/java/com/hrcrm/ai/tools/CLAUDE.md

## Tool definitions in this package
This package contains Claude tool (function) definitions.
Each tool class implements the `ClaudeTool` interface.


## Rules for adding a new tool
1. Create `YourActionTool.java` implementing `ClaudeTool`
2. The `getName()` must match exactly what Claude will call
3. The `getInputSchema()` must be valid JSON Schema
4. The `execute(JsonNode input)` method must handle null/missing fields gracefully
5. Register it in `ToolRegistry.java` — it won't be picked up automatically
6. Add a unit test in `YourActionToolTest.java`

## Current tools
- `UpdateLeadStatusTool` — changes lead pipeline stage
- `ScheduleFollowUpTool` — creates calendar task
- `SearchCandidatesTool` — queries candidate DB
- `SendWhatsAppTool` — triggers WhatsApp notification