---
name: write_test
description: Generate unit tests with edge case and error coverage
when_to_use: When the user asks to write, generate, or add tests
allowed-tools:
  - file_system
  - file_edit
  - grep
  - glob
model: inherit
---

Write thorough unit tests for the specified code. Follow the project's existing test framework and conventions.

Use the Arrange-Act-Assert pattern. Name tests descriptively: `should_[behavior]_when_[condition]`.

Cover:
- Happy path for each public method
- Null/empty/boundary inputs
- Error and exception handling
- Edge cases specific to the business logic

Mock external dependencies. Use specific assertions. Ensure each test is independent and idempotent.
