# Blueprint Notifications Microservice

A Node.js, Express, TypeScript, and MongoDB service that receives Blueprint alarm payloads and delivers them to humans through AWS SES email and optional Slack webhooks.

## API

### `POST /notify`

Blueprint calls this after `AlarmDetectionService` runs.

```json
{
  "alarmId": "alarm-123",
  "datasetId": "dataset-456",
  "billingPeriod": "2026-05",
  "severity": "HIGH",
  "title": "Department total crossed threshold",
  "message": "Engineering exceeded its May spend threshold.",
  "recipients": {
    "email": ["ops@example.com"],
    "slackWebhooks": ["https://hooks.slack.com/services/..."]
  }
}
```

### `GET /notifications`

Returns recent delivery history for the tiny frontend and API consumers.

### `GET /health`

Returns service health.

## Local Development

```bash
npm install
cp .env.example .env
docker compose up -d
npm run dev
```

The frontend is served from `/`.
Mongo Express is available at `http://localhost:8081`.

Try a notification locally:

```bash
curl -X POST http://localhost:3001/notify \
  -H "content-type: application/json" \
  -d '{
    "alarmId": "alarm-123",
    "datasetId": "dataset-456",
    "billingPeriod": "2026-05",
    "severity": "HIGH",
    "title": "Department total crossed threshold",
    "message": "Engineering exceeded its May spend threshold.",
    "recipients": {
      "email": ["ops@example.com"]
    }
  }'
```

## Environment

```bash
PORT=3001
MONGODB_URI=mongodb://127.0.0.1:27017
MONGODB_DB_NAME=blueprint_notifications
AWS_REGION=us-east-1
SES_SOURCE_EMAIL=alerts@example.com
CORS_ORIGIN=http://localhost:8080
```

Learn more about the main application:

- [Blueprint GitHub Repository](https://github.com/jawadazeem/blueprint)
- [Blueprint Website](https://blueprint.jawadazeem.com)
