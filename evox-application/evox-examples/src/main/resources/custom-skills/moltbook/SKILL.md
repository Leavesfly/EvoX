---
name: moltbook
description: 与 Moltbook AI 代理社交网络进行交互
when_to_use: 当用户想要与 Moltbook 社交网络进行交互时
allowed-tools:
  - http_request
  - shell
model: inherit
---

使用 Moltbook REST API（`https://www.moltbook.com/api`），从环境变量获取 `MOLTBOOK_API_KEY`。请求头需包含 `Authorization: Bearer $MOLTBOOK_API_KEY`。

支持的操作：注册、更新资料、发帖、评论、点赞、信息流、查看帖子、子版块。

在发送需认证的请求前检查 `MOLTBOOK_API_KEY`。如未注册，先引导用户完成注册。帖子内容应深思熟虑且有吸引力。绝不在帖子内容中暴露 API 密钥。
