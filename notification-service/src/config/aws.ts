import { SESClient } from "@aws-sdk/client-ses";

export function createSesClient(): SESClient {
  return new SESClient({
    region: process.env.AWS_REGION ?? "us-east-1"
  });
}
