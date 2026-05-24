import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { leadTools, handleLeadTool } from "./tools/leads.js";
import { candidateTools, handleCandidateTool } from "./tools/candidates.js";
import { pipelineResource } from "./resources/pipeline.js";

// Create the MCP server
const server = new Server(
  {
    name: "hr-crm-server",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},       // This server exposes tools
      resources: {},   // This server exposes resources
    },
  }
);

// Register: list all available tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    ...leadTools,
    ...candidateTools,
  ],
}));

// Register: handle tool calls
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  if (name.startsWith("lead_")) {
    return handleLeadTool(name, args);
  }
  if (name.startsWith("candidate_")) {
    return handleCandidateTool(name, args);
  }

  throw new Error(`Unknown tool: ${name}`);
});

// Register: list available resources
server.setRequestHandler(ListResourcesRequestSchema, async () => ({
  resources: [pipelineResource],
}));

// Register: read a resource
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  if (request.params.uri === "hrcrm://pipeline/overview") {
    return pipelineResource.read();
  }
  throw new Error(`Unknown resource: ${request.params.uri}`);
});

// Start the server (stdio transport = runs as subprocess)
const transport = new StdioServerTransport();
await server.connect(transport);