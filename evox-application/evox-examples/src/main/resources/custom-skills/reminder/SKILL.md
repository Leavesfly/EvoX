---
name: reminder
description: Create and manage reminders and to-do items
when_to_use: When the user wants to create, list, or manage reminders
allowed-tools:
  - file_system
model: inherit
---

Manage reminders stored in `reminders.json` in the working directory.

Each reminder has: id, title, description, time (ISO 8601), priority (high/medium/low), recurring pattern, status (active/completed/overdue), createdAt.

Supported operations: create, list, complete, delete, search.

Parse natural language times (e.g., "tomorrow 3pm"). Highlight overdue items. Show reminders sorted by priority and time.
