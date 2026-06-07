import type { AlarmPayload } from "../validation/notification.schema.js";

export class SlackService {
  async sendAlarm(payload: AlarmPayload, webhookUrl: string): Promise<void> {
    const response = await fetch(webhookUrl, {
      method: "POST",
      headers: {
        "content-type": "application/json"
      },
      body: JSON.stringify({
        text: `Blueprint ${payload.severity} alarm: ${payload.title}`,
        blocks: [
          {
            type: "header",
            text: {
              type: "plain_text",
              text: `Blueprint ${payload.severity} alarm`
            }
          },
          {
            type: "section",
            text: {
              type: "mrkdwn",
              text: `*${payload.title}*\n${payload.message}`
            }
          },
          {
            type: "context",
            elements: [
              {
                type: "mrkdwn",
                text: [
                  `Alarm ID: \`${payload.alarmId}\``,
                  payload.datasetId ? `Dataset: \`${payload.datasetId}\`` : undefined,
                  payload.billingPeriod ? `Period: \`${payload.billingPeriod}\`` : undefined
                ]
                  .filter(Boolean)
                  .join(" | ")
              }
            ]
          }
        ]
      })
    });

    if (!response.ok) {
      throw new Error(`Slack webhook failed with ${response.status}`);
    }
  }
}
