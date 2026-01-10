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

The current implementation provides rule-based summarization with statistics and pattern recognition. The architecture is designed to be extensible for integration with:

- Custom AI/ML models
- Third-party AI services (OpenAI, Azure AI, etc.)
- On-premise AI solutions

The `LogSummarizerService` can be extended or replaced to integrate with any AI backend while maintaining the same API contract.

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
