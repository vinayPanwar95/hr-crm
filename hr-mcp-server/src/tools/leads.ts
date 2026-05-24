import { Tool } from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";
import { db } from "../db.js";

// Tool DEFINITIONS — what Claude sees
export const leadTools: Tool[] = [
  {
    name: "lead_search",
    description: `Search leads in the HR CRM by name, company, status, or recruiter.
    Use this when asked about leads, pipeline status, or who owns a lead.`,
    inputSchema: {
      type: "object",
      properties: {
        query: {
          type: "string",
          description: "Name, email, or company to search for",
        },
        status: {
          type: "string",
          enum: ["NEW", "CONTACTED", "INTERESTED", "CONVERTED", "CLOSED"],
          description: "Filter by pipeline status",
        },
        recruiter_id: {
          type: "number",
          description: "Filter by assigned recruiter ID",
        },
        limit: {
          type: "number",
          description: "Max results to return (default 10)",
        },
      },
    },
  },
  {
    name: "lead_update_status",
    description: `Update a lead's pipeline status.
    Use when asked to move a lead, mark as converted, close a lead, etc.`,
    inputSchema: {
      type: "object",
      required: ["lead_id", "new_status"],
      properties: {
        lead_id: {
          type: "number",
          description: "The lead's database ID",
        },
        new_status: {
          type: "string",
          enum: ["NEW", "CONTACTED", "INTERESTED", "NOT_INTERESTED",
                 "CONVERTED", "CLOSED"],
        },
        note: {
          type: "string",
          description: "Optional note to attach to the status change",
        },
      },
    },
  },
  {
    name: "lead_create",
    description: "Create a new lead in the CRM",
    inputSchema: {
      type: "object",
      required: ["name", "phone"],
      properties: {
        name:        { type: "string" },
        phone:       { type: "string" },
        email:       { type: "string" },
        company:     { type: "string" },
        position:    { type: "string", description: "Role they're hiring for" },
        source:      { type: "string", description: "How they found us" },
        recruiter_id:{ type: "number" },
      },
    },
  },
];

// Tool HANDLERS — the actual logic
export async function handleLeadTool(name: string, args: any) {
  switch (name) {
    case "lead_search": {
      const { query, status, recruiter_id, limit = 10 } = args;

      let sql = `
        SELECT l.id, l.name, l.company, l.status, l.phone,
               u.name as recruiter_name, l.created_at
        FROM leads l
        LEFT JOIN users u ON l.recruiter_id = u.id
        WHERE 1=1
      `;
      const params: any[] = [];

      if (query) {
        params.push(`%${query}%`);
        sql += ` AND (l.name ILIKE $${params.length}
                   OR l.company ILIKE $${params.length}
                   OR l.email ILIKE $${params.length})`;
      }
      if (status) {
        params.push(status);
        sql += ` AND l.status = $${params.length}`;
      }
      if (recruiter_id) {
        params.push(recruiter_id);
        sql += ` AND l.recruiter_id = $${params.length}`;
      }

      params.push(limit);
      sql += ` ORDER BY l.created_at DESC LIMIT $${params.length}`;

      const result = await db.query(sql, params);

      return {
        content: [{
          type: "text",
          text: JSON.stringify(result.rows, null, 2),
        }],
      };
    }

    case "lead_update_status": {
      const { lead_id, new_status, note } = args;

      await db.query(
        `UPDATE leads SET status = $1, updated_at = NOW() WHERE id = $2`,
        [new_status, lead_id]
      );

      if (note) {
        await db.query(
          `INSERT INTO lead_notes (lead_id, content, created_at)
           VALUES ($1, $2, NOW())`,
          [lead_id, note]
        );
      }

      return {
        content: [{
          type: "text",
          text: `Lead ${lead_id} updated to ${new_status}${note ? ' with note' : ''}.`,
        }],
      };
    }

    case "lead_create": {
      const result = await db.query(
        `INSERT INTO leads (name, phone, email, company, position_required,
          source, recruiter_id, status, created_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,'NEW',NOW())
         RETURNING id`,
        [args.name, args.phone, args.email, args.company,
         args.position, args.source, args.recruiter_id]
      );

      return {
        content: [{
          type: "text",
          text: `Created lead with ID ${result.rows[0].id}`,
        }],
      };
    }

    default:
      throw new Error(`Unknown lead tool: ${name}`);
  }
}