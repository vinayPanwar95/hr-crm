import { Resource } from "@modelcontextprotocol/sdk/types.js";
import { db } from "../db.js";

export const pipelineResource = {
  // The resource descriptor (what Claude sees in its list)
  ...({
    uri: "hrcrm://pipeline/overview",
    name: "Lead pipeline overview",
    description: "Current snapshot of all leads grouped by pipeline stage",
    mimeType: "application/json",
  } as Resource),

  // The actual data fetch
  async read() {
    const result = await db.query(`
      SELECT
        status,
        COUNT(*) as count,
        COUNT(DISTINCT recruiter_id) as recruiters_active
      FROM leads
      GROUP BY status
      ORDER BY CASE status
        WHEN 'NEW' THEN 1
        WHEN 'CONTACTED' THEN 2
        WHEN 'INTERESTED' THEN 3
        WHEN 'CONVERTED' THEN 4
        WHEN 'CLOSED' THEN 5
        ELSE 6
      END
    `);

    return {
      contents: [{
        uri: "hrcrm://pipeline/overview",
        mimeType: "application/json",
        text: JSON.stringify(result.rows, null, 2),
      }],
    };
  },
};