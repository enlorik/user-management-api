# Log Summarization API

## Overview

The Log Summarization API provides AI-powered insights from system logs with comprehensive privacy and security measures. This endpoint is designed for administrators to quickly analyze log data and identify patterns, issues, and trends.

## Endpoint

```
GET /api/v1/logs/summarize
```

**Authentication Required**: Admin role (`ROLE_ADMIN`)

## Query Parameters

All parameters are optional and can be combined for precise filtering:

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `startTime` | ISO8601 | Start time for log filtering | `2026-01-10T00:00:00Z` |
| `endTime` | ISO8601 | End time for log filtering | `2026-01-10T23:59:59Z` |
| `logLevel` | String | Filter by log level | `INFO`, `WARN`, `ERROR`, `DEBUG` |
| `actionType` | String | Filter by action type | `USER_LOGIN`, `USER_REGISTRATION`, `USER_CREATE`, etc. |
| `userId` | String | Filter by user ID | `123` |

## Response Format

```json
{
  "summary": "Log Analysis Summary for the last 1 hour:\n\nTotal Events: 12\n...",
  "totalLogs": 12,
  "startTime": "2026-01-10T00:00:00Z",
  "endTime": "2026-01-10T01:00:00Z",
  "logLevelStats": {
    "INFO": 8,
    "ERROR": 3,
    "WARN": 1
  },
  "actionTypeStats": {
    "USER_LOGIN": 5,
    "USER_CREATE": 2,
    "USER_REGISTRATION": 3
  },
  "topIssues": [
    "Login failure (occurred 3 times)",
    "Validation failure (occurred 1 times)"
  ]
}
```

## Example Requests

### Basic Request (All Logs)

```bash
curl -X GET "http://localhost:8080/api/v1/logs/summarize" \
  -u admin:password \
  -H "Content-Type: application/json"
```

### Filter by Time Range

```bash
curl -X GET "http://localhost:8080/api/v1/logs/summarize?startTime=2026-01-10T00:00:00Z&endTime=2026-01-10T23:59:59Z" \
  -u admin:password \
  -H "Content-Type: application/json"
```

### Filter by Log Level

```bash
curl -X GET "http://localhost:8080/api/v1/logs/summarize?logLevel=ERROR" \
  -u admin:password \
  -H "Content-Type: application/json"
```

### Filter by Action Type

```bash
curl -X GET "http://localhost:8080/api/v1/logs/summarize?actionType=USER_LOGIN" \
  -u admin:password \
  -H "Content-Type: application/json"
```

### Combined Filters

```bash
curl -X GET "http://localhost:8080/api/v1/logs/summarize?startTime=2026-01-10T00:00:00Z&logLevel=ERROR&actionType=USER_LOGIN" \
  -u admin:password \
  -H "Content-Type: application/json"
```

## Privacy & Security

### Data Sanitization

Before processing, all logs are sanitized to remove sensitive information:

- **Email addresses**: Masked (e.g., `j***e@e***.com`)
- **Phone numbers**: Replaced with `***-***-****`
- **Tokens/API keys**: Replaced with `****MASKED_TOKEN****`
- **SSN**: Replaced with `***-**-****`

### Security Features

1. **Admin-Only Access**: Requires `ROLE_ADMIN` authorization
2. **CSRF Protection**: Disabled for stateless REST API
3. **Method-Level Security**: Uses `@PreAuthorize` annotations
4. **Input Validation**: All parameters are validated for format and range

## Error Handling

The API returns appropriate HTTP status codes and error messages:

### 400 Bad Request

Invalid parameters or time ranges:

```json
{
  "error": "Invalid startTime format. Expected ISO8601 format (e.g., 2026-01-10T00:00:00Z)"
}
```

```json
{
  "error": "Invalid time range: startTime must be before endTime"
}
```

### 403 Forbidden

Non-admin user attempting access:

```
HTTP 403 Forbidden
```

### 302 Found (Redirect)

Unauthenticated user - redirects to login page.

## Use Cases

1. **Security Monitoring**: Identify failed login attempts and unauthorized access
2. **Performance Analysis**: Track error rates and response times
3. **User Activity**: Monitor user registration and login patterns
4. **Debugging**: Quickly identify and analyze error conditions
5. **Compliance**: Generate audit reports for specific time periods

## Integration with AI

This endpoint now includes **OpenAI-powered log summarization** for intelligent insights and recommendations.

### How AI Integration Works

The application uses OpenAI's GPT-3.5-turbo model to generate intelligent summaries when configured:

1. **Configuration**: Set the `OPENAI_API_KEY` environment variable
2. **Smart Fallback**: If the API key is not set or OpenAI is unavailable, the system automatically falls back to rule-based summarization
3. **Secure**: API keys are never exposed in code or logs
4. **Cost-Effective**: Uses GPT-3.5-turbo for optimal cost/performance balance

### Setup Instructions

```bash
# Get your API key from https://platform.openai.com/api-keys
export OPENAI_API_KEY=sk-proj-...

# Start the application
mvn spring-boot:run
```

### Example AI-Generated Summary

**With OpenAI (when `OPENAI_API_KEY` is configured):**

```json
{
  "summary": "System Health Assessment:\n\nThe system shows a moderate error rate of 15% with 12 total events analyzed. Critical issues require immediate attention:\n\n1. Pattern Analysis: Multiple failed login attempts detected, suggesting potential brute force activity or user credential issues\n2. Key Trends: User registration is active with 3 new signups, but login success rate is concerning\n3. Critical Issues:\n   - 2 login failures require investigation\n   - No database connectivity issues detected\n   - System response times within normal parameters\n\nRecommendations:\n1. Review and strengthen authentication mechanisms\n2. Implement rate limiting if not already in place\n3. Monitor login patterns for the affected accounts\n4. Consider implementing multi-factor authentication for enhanced security",
  "totalLogs": 12,
  "startTime": "2026-01-10T00:00:00Z",
  "endTime": "2026-01-10T01:00:00Z",
  "logLevelStats": {
    "INFO": 8,
    "ERROR": 3,
    "WARN": 1
  },
  "actionTypeStats": {
    "USER_LOGIN": 5,
    "USER_CREATE": 2,
    "USER_REGISTRATION": 3
  },
  "topIssues": [
    "Login failure (occurred 3 times)",
    "Validation failure (occurred 1 times)"
  ]
}
```

**Without OpenAI (fallback to rule-based):**

```json
{
  "summary": "Log Analysis Summary for the last 1 hour:\n\nTotal Events: 12\n\nLog Level Distribution:\n- 3 errors detected\n- 1 warnings\n- 8 informational events\n\nUser Activity:\n- 5 login attempts\n- 3 registration attempts\n- 2 users created\n\nTop Issues:\n- Login failure (occurred 3 times)\n- Validation failure (occurred 1 times)\n\nInsights:\n- Low error rate (25.0%)\n- Multiple login attempts detected, review for potential security concerns\n- User growth: 5 new users",
  "totalLogs": 12,
  ...
}
```

### Benefits of AI-Powered Summaries

- **Contextual Understanding**: GPT-3.5-turbo understands the relationships between different log events
- **Actionable Insights**: Provides specific recommendations based on the log data
- **Natural Language**: Summaries are written in clear, professional language
- **Pattern Recognition**: Identifies complex patterns that rule-based systems might miss
- **Risk Assessment**: Evaluates system health and highlights critical issues

### Architecture

The `LogSummarizerService` implements a hybrid approach:

```
┌─────────────────────────────────────┐
│   LogController                      │
│   /api/v1/logs/summarize            │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   LogSummarizerService              │
├─────────────────────────────────────┤
│  1. Try OpenAI API (if configured)  │
│  2. Fallback to rule-based          │
│  3. Return comprehensive summary    │
└─────────────────────────────────────┘
```

### Error Handling

The integration includes robust error handling:

- **API Unavailable**: Automatically falls back to rule-based summarization
- **Network Issues**: Timeouts and connection errors are gracefully handled
- **Invalid API Key**: Logs warning and uses fallback method
- **Rate Limiting**: Respects OpenAI's rate limits and retries appropriately

### Cost Considerations

- **GPT-3.5-turbo pricing**: ~$0.002 per 1K tokens
- **Typical request**: 200-500 tokens (prompt) + 200-300 tokens (response) = ~$0.001-0.002 per summary
- **Optimization**: Summaries are limited to 500 tokens to control costs
- **Free tier**: Not available for API usage; requires paid account

For cost estimation:
- 1,000 log summaries ≈ $1-2
- 10,000 log summaries ≈ $10-20

The `LogSummarizerService` can be extended or replaced to integrate with other AI providers (Azure OpenAI, Anthropic Claude, etc.) while maintaining the same API contract.

## Logging

The endpoint itself logs its operations:

```json
{
  "timestamp": "2026-01-10T01:00:00Z",
  "level": "INFO",
  "actionType": "LOG_SUMMARIZATION",
  "message": "Received log summarization request - startTime: ..., endTime: ...",
  "requestId": "abc-123"
}
```

## Performance Considerations

- Log files are read sequentially from disk
- Filtering is applied in-memory after reading
- Large log files may take longer to process
- Consider implementing pagination for very large log sets in production

## Future Enhancements

1. Real-time log streaming support
2. Export summaries to PDF or CSV
3. Scheduled automatic summaries
4. Custom alert thresholds
5. Integration with monitoring tools (Grafana, Prometheus)
