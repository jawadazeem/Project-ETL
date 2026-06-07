import { z } from "zod";

export const alarmPayloadSchema = z
  .object({
    alarmId: z.string().min(1),
    datasetId: z.string().min(1).optional(),
    billingPeriod: z.string().min(1).optional(),
    severity: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]),
    title: z.string().min(1).max(160),
    message: z.string().min(1).max(4000),
    recipients: z.object({
      email: z.array(z.string().email()).default([]),
      slackWebhooks: z.array(z.string().url()).default([])
    })
  })
  .refine(
    (payload) => payload.recipients.email.length > 0 || payload.recipients.slackWebhooks.length > 0,
    {
      message: "At least one email recipient or Slack webhook is required",
      path: ["recipients"]
    }
  );

export type AlarmPayload = z.infer<typeof alarmPayloadSchema>;
