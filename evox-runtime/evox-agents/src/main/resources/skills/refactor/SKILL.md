---
name: refactor
description: Improve code structure, readability, and maintainability
when_to_use: When the user asks to refactor, clean up, or restructure code
allowed-tools:
  - file_system
  - file_edit
  - grep
  - glob
model: inherit
---

Refactor the specified code to improve its design. Preserve existing behavior — refactoring must not change functionality.

Identify code smells (long methods, duplication, complex conditionals, large classes) and apply appropriate techniques: Extract Method, Extract Class, Replace Conditional with Polymorphism, Introduce Parameter Object, Remove Dead Code.

Follow SOLID principles. Make changes incrementally. Explain WHY each change improves the code.

Output: Code Smells → Refactoring Plan → Refactored Code → Impact Analysis.
