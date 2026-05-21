---
name: weather
description: 查询指定地点的天气信息
when_to_use: 当用户询问天气、气温或天气预报时
allowed-tools:
  - http_request
  - shell
model: inherit
---

使用 HTTP 工具获取实时天气数据（如 `curl wttr.in/CityName?format=j1`）。

使用天气表情符号清晰展示结果（☀️⛅☁️🌧️❄️🌡️）：
1. 当前状况：温度、湿度、风力、天气描述
2. 今日预报：最高/最低温度、降水概率
3. 如有需要提供多日展望
4. 实用建议（穿衣、雨伞、户外活动）

按需转换温度单位。如有恶劣天气预警应提及。
