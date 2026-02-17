---
name: github
description: Interact with GitHub repositories and notifications
when_to_use: When the user wants to check GitHub notifications, issues, or PRs
allowed-tools:
  - http_request
  - shell
model: inherit
---

Use the GitHub REST API v3 (`api.github.com`) with `GITHUB_TOKEN` from environment.

Supported operations: notifications, issues, prs, repo_info, search, create_issue.

Check `GITHUB_TOKEN` before authenticated requests. Handle rate limits gracefully. Group notifications by repository. Show relevant metadata (labels, assignees, CI status) for issues and PRs.
