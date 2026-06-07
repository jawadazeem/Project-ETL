import { Router } from "express";
import { ZodError } from "zod";
import { NotificationService } from "../services/notification.service.js";
import { alarmPayloadSchema } from "../validation/notification.schema.js";

export function createNotificationRouter(notificationService: NotificationService): Router {
  const router = Router();

  router.post("/notify", async (request, response, next) => {
    try {
      const payload = alarmPayloadSchema.parse(request.body);
      const notification = await notificationService.notify(payload);

      response.status(202).json({
        id: notification.id,
        alarmId: notification.alarmId,
        deliveries: notification.deliveries
      });
    } catch (error) {
      if (error instanceof ZodError) {
        response.status(400).json({
          error: "Invalid notification payload",
          details: error.flatten()
        });
        return;
      }

      next(error);
    }
  });

  router.get("/notifications", async (request, response, next) => {
    try {
      const rawLimit = Number(request.query.limit ?? 50);
      const limit = Number.isFinite(rawLimit) ? Math.min(Math.max(rawLimit, 1), 100) : 50;

      response.json(await notificationService.list(limit));
    } catch (error) {
      next(error);
    }
  });

  return router;
}
