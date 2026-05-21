---
name: reminder
description: 创建和管理提醒事项与待办项
when_to_use: 当用户想要创建、查看或管理提醒事项时
allowed-tools:
  - file_system
model: inherit
---

管理存储在工作目录下 `reminders.json` 中的提醒事项。

每个提醒包含：id、title、description、time（ISO 8601）、priority（high/medium/low）、recurring pattern、status（active/completed/overdue）、createdAt。

支持的操作：创建、列表、完成、删除、搜索。

解析自然语言时间（如"明天下午3点"）。高亮显示过期项。按优先级和时间排序展示提醒。
