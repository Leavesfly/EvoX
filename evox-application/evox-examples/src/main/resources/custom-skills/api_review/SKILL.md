---
name: api_review
description: Review API designs for RESTful best practices
when_to_use: When the user asks to review or improve an API design
allowed-tools:
  - file_system
  - grep
  - shell
model: inherit
---

Review the API design against RESTful best practices.

Check: URL design (plural nouns, shallow nesting), correct HTTP methods, appropriate status codes, security (auth, input validation, rate limiting, CORS), performance (pagination, caching, compression), and documentation (OpenAPI spec).

For each endpoint, report: Current state → Issues (HIGH/MEDIUM/LOW) → Recommendation → Corrected example.
