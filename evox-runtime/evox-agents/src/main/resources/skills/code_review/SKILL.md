---
name: code_review
description: Review code for bugs, security, and style issues
when_to_use: When the user asks to review, check, or audit code
allowed-tools:
  - file_system
  - grep
  - glob
model: inherit
---

Review the provided code thoroughly. For each issue found, cite the file and line, classify severity (🔴 Critical / 🟡 Warning / 🔵 Info), and provide a concrete fix.

Check for:
- Bugs, logic errors, and unhandled edge cases
- Security vulnerabilities (injection, XSS, CSRF, auth issues)
- Performance problems and resource leaks
- Readability, naming, and missing error handling
- Thread safety in concurrent code

Output a structured review: Summary → Issues (by severity) → Suggested Fixes.
