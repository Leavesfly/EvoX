---
name: github
description: 与 GitHub 仓库和通知进行交互
when_to_use: 当用户想要查看 GitHub 通知、Issue 或 PR 时
allowed-tools:
  - http_request
  - shell
model: inherit
---

使用 GitHub REST API v3（`api.github.com`），从环境变量获取 `GITHUB_TOKEN`。

支持的操作：通知、Issue、PR、仓库信息、搜索、创建 Issue。

在发送需认证的请求前检查 `GITHUB_TOKEN`。优雅处理速率限制。按仓库分组通知。为 Issue 和 PR 展示相关元数据（标签、负责人、CI 状态）。
