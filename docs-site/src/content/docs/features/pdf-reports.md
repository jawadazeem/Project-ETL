---
title: PDF Reports
description: Generate branded PDF billing reports stored in S3.
sidebar:
  order: 6
---

## Overview

Blueprint generates PDF billing reports with corporate branding, stores them in S3, and provides download endpoints.

## User Flow

1. User selects a billing period and clicks **Generate PDF**
2. Frontend sends `POST /datasets/{datasetId}/reports/pdf?period=YYYY-MM`
3. Backend generates a PDF using `LocalJavaPdfRenderer`, stores it in S3
4. Frontend receives the report metadata and triggers a download via `GET /datasets/{datasetId}/reports/pdf/{reportId}`

## API

| Method | Path | Header | Response |
|--------|------|--------|----------|
| `POST` | `/datasets/{datasetId}/reports/pdf` | `X-User-Id: String` | `PdfReport` |
| `GET` | `/datasets/{datasetId}/reports/pdf/{reportId}` | — | PDF binary stream |

## Corporate Branding

Users can configure company branding for report headers:

| Method | Path | Response |
|--------|------|----------|
| `GET` | `/users/{userId}/corporate-info` | `CorporateInfo` or 404 |
| `PUT` | `/users/{userId}/corporate-info` | `CorporateInfo` |

Fields: `companyName`, `address`, `phone`, `email`, `logoUrl`.

## Key Files

| File | Role |
|------|------|
| `PdfController.java` | `POST /pdf` (generate) and `GET /pdf/{reportId}` (download) |
| `PdfReportService.java` | Orchestrates report generation and download |
| `LocalJavaPdfRenderer.java` | Pure Java PDF renderer (implements `PdfRenderer`) |
| `PdfStorageService.java` | S3 storage for generated PDFs |
| `CorporateInfoService.java` | Manages company branding for report headers |
| `CorporateInfoController.java` | `GET` and `PUT /users/{userId}/corporate-info` |
