import "dotenv/config";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createSesClient } from "./config/aws.js";
import { connectDatabase } from "./config/db.js";
import { createNotificationRouter } from "./controllers/notification.controller.js";
import { EmailService } from "./services/email.service.js";
import { NotificationService } from "./services/notification.service.js";
import { SlackService } from "./services/slack.service.js";

const app = express();
const port = Number(process.env.PORT ?? 3001);
const publicDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../public");

app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({ origin: process.env.CORS_ORIGIN?.split(",") ?? "*" }));
app.use(express.json({ limit: "1mb" }));
app.use(morgan("tiny"));
app.use(express.static(publicDir));

const notificationService = new NotificationService(
  new EmailService(createSesClient()),
  new SlackService()
);

app.get("/health", (_request, response) => {
  response.json({ status: "ok", service: "blueprint-notifications" });
});

app.use(createNotificationRouter(notificationService));

app.use((error: unknown, _request: express.Request, response: express.Response) => {
  console.error(error);

  response.status(500).json({
    error: "Notification service failed"
  });
});

await connectDatabase();

app.listen(port, () => {
  console.log(`Blueprint notification service listening on http://localhost:${port}`);
});
