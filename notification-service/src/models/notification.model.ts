import mongoose, { Schema } from "mongoose";

export type NotificationChannel = "email" | "slack";
export type NotificationStatus = "succeeded" | "failed" | "skipped";
export type AlarmSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface NotificationDelivery {
  channel: NotificationChannel;
  recipient: string;
  status: NotificationStatus;
  providerMessageId?: string;
  error?: string;
  sentAt: Date;
}

export interface NotificationDocument extends mongoose.Document {
  alarmId: string;
  datasetId?: string;
  billingPeriod?: string;
  severity: AlarmSeverity;
  title: string;
  message: string;
  source: "blueprint";
  deliveries: NotificationDelivery[];
  createdAt: Date;
  updatedAt: Date;
}

const deliverySchema = new Schema<NotificationDelivery>(
  {
    channel: { type: String, enum: ["email", "slack"], required: true },
    recipient: { type: String, required: true },
    status: { type: String, enum: ["succeeded", "failed", "skipped"], required: true },
    providerMessageId: String,
    error: String,
    sentAt: { type: Date, required: true }
  },
  { _id: false }
);

const notificationSchema = new Schema<NotificationDocument>(
  {
    alarmId: { type: String, required: true, index: true },
    datasetId: { type: String },
    billingPeriod: { type: String },
    severity: { type: String, enum: ["LOW", "MEDIUM", "HIGH", "CRITICAL"], required: true },
    title: { type: String, required: true },
    message: { type: String, required: true },
    source: { type: String, enum: ["blueprint"], default: "blueprint", required: true },
    deliveries: { type: [deliverySchema], default: [] }
  },
  {
    timestamps: true
  }
);

export const NotificationModel = mongoose.model<NotificationDocument>(
  "Notification",
  notificationSchema
);
