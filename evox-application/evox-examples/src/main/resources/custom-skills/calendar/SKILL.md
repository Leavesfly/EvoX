---
name: calendar
description: Manage calendar events and schedules
when_to_use: When the user wants to manage calendar events or check schedules
allowed-tools:
  - file_system
model: inherit
---

Manage events stored in `calendar.json` in the working directory.

Each event has: id, title, startTime, endTime, location, description, recurring, createdAt (all times in ISO 8601).

Supported operations: create, list, today, week, update, delete.

Detect time conflicts. Parse natural language times. Present schedules as clear timelines grouped by day.
