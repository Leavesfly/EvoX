---
name: weather
description: Query weather information for a location
when_to_use: When the user asks about weather, temperature, or forecasts
allowed-tools:
  - http_request
  - shell
model: inherit
---

Fetch real-time weather data using HTTP tools (e.g., `curl wttr.in/CityName?format=j1`).

Present results clearly with weather emojis (☀️⛅☁️🌧️❄️🌡️):
1. Current conditions: temperature, humidity, wind, description
2. Today's forecast: high/low, precipitation probability
3. Multi-day outlook if requested
4. Practical recommendations (clothing, umbrella, outdoor activities)

Convert temperature units as requested. Mention severe weather alerts if applicable.
