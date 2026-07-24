---
title: Frontend
description: Static frontend architecture, state management, and UI patterns.
sidebar:
  order: 2
---

## Overview

The frontend is a static HTML/CSS/JS application served directly by Spring Boot from `src/main/resources/static`. It has no build step, package manager, client-side router, or component framework.

## Pages

| Route | File | Purpose |
|-------|------|---------|
| `/` | `index.html` | Dashboard |
| `/login.html` | `login.html` | Demo login / guest entry |
| `/info.html` | `info.html` | About page with tech stack |
| `/error/404.html` | `error/404.html` | Static 404 page |

## JavaScript Files

| File | Purpose |
|------|---------|
| `js/main.js` | Shared navigation helpers on `window.Blueprint` |
| `js/dashboard.js` | Dashboard state, API calls, charts, modals, upload, Trace chat, toast system, skeleton loaders, welcome overlay, dataset switcher, notifications, archive/restore/delete, predictions |
| `js/login.js` | Login and guest redirect behavior |
| `js/info.js` | Open dashboard button |
| `js/error.js` | Go home button |

## State Management

State is module-global in `dashboard.js`:

```js
const GUEST_USER_ID = "00000000-0000-0000-0000-000000000001";

let providerChartInstance = null;
let alarmsChartInstance = null;
let currentPeriod = null;
let currentDatasetId = null;
let currentPageAllRecords = 0;
let currentPageFilterByProvider = 0;
let viewingArchived = false;
const pageSize = 20;
```

## UI Patterns

### Toolbar Buttons

All action buttons use the `.btn-modern` system with consistent sizing (8px 16px padding, 14px font, 8px border-radius):

- `.btn-modern` — default subtle background
- `.btn-modern-primary` — accent-colored (blue)
- `.btn-modern-danger` — red tint for destructive actions (Delete Dataset)

### Skeleton Loaders

Each async-loaded card area has a `skeleton-block` element with an animated shimmer during fetches, hidden on completion or error.

### Toast Notifications

`showToast(message, type)` creates a dismissable toast in `#toast-container` (fixed top-right):
- `error` — red left border
- `info` — blue left border
- Auto-dismiss after 5 seconds

### Welcome Overlay

Displayed on first visit when no dataset is active. Shows "Upload CSV" and "Load Demo Data" buttons. On return visits, `tryLoadExistingDatasets()` silently finds the first `READY` dataset.

### Dataset Switcher

A `<select>` in the navbar, populated with each dataset's filename, upload date, and status. Changing the selection resets pagination and reloads all dashboard sections.

## Dependencies

- Bootstrap 5.3.2 (CDN)
- Chart.js (CDN)
- Google Fonts Montserrat (CDN)
