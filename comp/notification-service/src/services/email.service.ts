import { SendEmailCommand, SESClient } from "@aws-sdk/client-ses";
import type { AlarmPayload } from "../validation/notification.schema.js";

export interface EmailResult {
  providerMessageId?: string;
}

export class EmailService {
  constructor(private readonly sesClient: SESClient) {}

  async sendAlarmEmail(payload: AlarmPayload, recipient: string): Promise<EmailResult> {
    const sourceEmail = process.env.SES_SOURCE_EMAIL;

    if (!sourceEmail) {
      throw new Error("SES_SOURCE_EMAIL is required when email notifications are enabled");
    }

    const response = await this.sesClient.send(
      new SendEmailCommand({
        Source: sourceEmail,
        Destination: {
          ToAddresses: [recipient]
        },
        Message: {
          Subject: {
            Data: `[Blueprint ${payload.severity}] ${payload.title}`
          },
          Body: {
            Text: {
              Data: renderTextEmail(payload)
            },
            Html: {
              Data: renderHtmlEmail(payload)
            }
          }
        }
      })
    );

    return { providerMessageId: response.MessageId };
  }
}

function renderTextEmail(payload: AlarmPayload): string {
  return [
    payload.message,
    "",
    `Alarm ID: ${payload.alarmId}`,
    payload.datasetId ? `Dataset: ${payload.datasetId}` : undefined,
    payload.billingPeriod ? `Billing period: ${payload.billingPeriod}` : undefined,
    `Severity: ${payload.severity}`
  ]
    .filter(Boolean)
    .join("\n");
}

function renderHtmlEmail(payload: AlarmPayload): string {
  const fields = [
    ["Alarm ID", payload.alarmId],
    ["Dataset", payload.datasetId],
    ["Billing period", payload.billingPeriod],
    ["Severity", payload.severity]
  ]
    .filter(([, value]) => Boolean(value))
    .map(([label, value]) => `<li><strong>${escapeHtml(label)}:</strong> ${escapeHtml(value)}</li>`)
    .join("");

  return `
    <h1>${escapeHtml(payload.title)}</h1>
    <p>${escapeHtml(payload.message)}</p>
    <ul>${fields}</ul>
  `;
}

function escapeHtml(value: unknown): string {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
