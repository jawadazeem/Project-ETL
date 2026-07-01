import mongoose from "mongoose";

export async function connectDatabase(): Promise<void> {
  const mongoUri = process.env.MONGODB_URI;

  if (!mongoUri) {
    throw new Error("MONGODB_URI is required");
  }

  await mongoose.connect(mongoUri, {
    dbName: process.env.MONGODB_DB_NAME ?? "blueprint_notifications",
    serverSelectionTimeoutMS: 5000
  });
}

export async function disconnectDatabase(): Promise<void> {
  await mongoose.disconnect();
}
