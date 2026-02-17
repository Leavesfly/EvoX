---
name: stock_tracker
description: Track stock prices and market data
when_to_use: When the user asks about stock prices or market data
allowed-tools:
  - http_request
  - shell
model: inherit
---

Fetch financial data using free APIs (Yahoo Finance via curl, Alpha Vantage).

Supported operations: quote, history, watchlist, alert, market_summary.

Present data in tabular format with timestamps. Handle market hours awareness. Always include a disclaimer that this is not financial advice.
