---
name: api_review
description: 按照 RESTful 最佳实践评审 API 设计
when_to_use: 当用户要求评审或改进 API 设计时
allowed-tools:
  - file_system
  - grep
  - shell
model: inherit
---

按照 RESTful 最佳实践评审 API 设计。

检查项：URL 设计（复数名词、浅层嵌套）、正确的 HTTP 方法、恰当的状态码、安全性（认证、输入验证、限流、CORS）、性能（分页、缓存、压缩）、文档（OpenAPI 规范）。

对每个接口，报告：当前状态 → 问题（高/中/低）→ 建议 → 修正示例。
