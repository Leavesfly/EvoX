---
name: stock_tracker
description: 追踪股票价格和市场数据
when_to_use: 当用户询问股票价格或市场数据时
allowed-tools:
  - http_request
  - shell
model: inherit
---

使用免费 API（通过 curl 访问 Yahoo Finance、Alpha Vantage）获取金融数据。

支持的操作：报价、历史、自选股、提醒、市场概览。

以带时间戳的表格格式展示数据。注意市场交易时间。始终附上免责声明：本内容不构成投资建议。
