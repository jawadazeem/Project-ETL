import { NotificationModel, type NotificationDelivery } from "../models/notification.model.js";
import type { AlarmPayload } from "../validation/notification.schema.js";
import { EmailService } from "./email.service.js";
import { SlackService } from "./slack.service.js";

export class NotificationService {
  constructor(
    private readonly emailService: EmailService,
    private readonly slackService: SlackService
  ) {}

  async notify(payload: AlarmPayload) {
    const deliveries: NotificationDelivery[] = [];

    for (const recipient of payload.recipients.email ?? []) {
      deliveries.push(await this.sendEmail(payload, recipient));
    }

    for (const webhookUrl of payload.recipients.slackWebhooks ?? []) {
      deliveries.push(await this.sendSlack(payload, webhookUrl));
    }

    return NotificationModel.create({
      alarmId: payload.alarmId,
      datasetId: payload.datasetId,
      billingPeriod: payload.billingPeriod,
      severity: payload.severity,
      title: payload.title,
      message: payload.message,
      deliveries
    });
  }

  async list(limit: number) {
    return NotificationModel.find().sort({ createdAt: -1 }).limit(limit).lean();
  }

  private async sendEmail(payload: AlarmPayload, recipient: string): Promise<NotificationDelivery> {
    try {
      const result = await this.emailService.sendAlarmEmail(payload, recipient);

      return {
        channel: "email",
        recipient,
        status: "succeeded",
        providerMessageId: result.providerMessageId,
        sentAt: new Date()
      };
    } catch (error) {
      return {
        channel: "email",
        recipient,
        status: "failed",
        error: error instanceof Error ? error.message : "Unknown email failure",
        sentAt: new Date()
      };
    }
  }

  private async sendSlack(payload: AlarmPayload, webhookUrl: string): Promise<NotificationDelivery> {
    try {
      await this.slackService.sendAlarm(payload, webhookUrl);

      return {
        channel: "slack",
        recipient: maskWebhookUrl(webhookUrl),
        status: "succeeded",
        sentAt: new Date()
      };
    } catch (error) {
      return {
        channel: "slack",
        recipient: maskWebhookUrl(webhookUrl),
        status: "failed",
        error: error instanceof Error ? error.message : "Unknown Slack failure",
        sentAt: new Date()
      };
    }
  }
}

function maskWebhookUrl(webhookUrl: string): string {
  return webhookUrl.replace(/\/services\/(.{4}).+$/, "/services/$1...");
}
