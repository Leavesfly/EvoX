---
name: calendar
description: 管理日历事件和日程安排
when_to_use: 当用户想要管理日历事件或查看日程时
allowed-tools:
  - file_system
model: inherit
---

管理存储在工作目录下 `calendar.json` 中的事件。

每个事件包含：id、title、startTime、endTime、location、description、recurring、createdAt（所有时间使用 ISO 8601 格式）。

支持的操作：创建、列表、今日、本周、更新、删除。

检测时间冲突。解析自然语言时间。以按天分组的清晰时间线形式展示日程。
