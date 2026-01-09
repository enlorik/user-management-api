# Logging Integration

This document describes the logging functionality integrated into the user-management API.

## Features

### 1. Structured JSON Logging
The application uses Logback with the Logstash encoder to produce structured JSON logs that can be easily consumed by log aggregation tools and AI summarizers.

### 2. Sensitive Data Masking
Passwords and other sensitive information are automatically masked in logs to prevent security issues. The following fields are masked:
- `password`
- `passwordHash`
- `rawPassword`
- `token`
- `apiKey`
- `authorization`

### 3. Log Metadata
Each log entry includes:
- `timestamp`: ISO 8601 formatted timestamp
- `level`: Log level (DEBUG, INFO, WARN, ERROR)
- `logger`: Fully qualified class name
- `message`: Log message
- `thread`: Thread name
- `caller_class_name`: Class that generated the log
- `caller_method_name`: Method that generated the log
- `caller_file_name`: Source file name
- `caller_line_number`: Line number in source file
- `application`: Application name

### 4. Environment-Specific Configuration

#### Development (local profile)
- Human-readable console logs
- DEBUG level for application code
- DEBUG level for Spring Web

#### Production (default profile)
- JSON formatted logs to console and file
- INFO level for application code
- WARN level for Spring Web and Hibernate
- Logs rotate daily and keep 30 days of history

## Example Log Entries

### User Creation (with masked password)
```json
{
  "timestamp": "2026-01-09T14:54:30.508234987Z",
  "@version": "1",
  "message": "Creating new user - username: user1291129327, email: 123@example.com, role: USER",
  "logger": "com.empress.usermanagementapi.service.UserService",
  "thread": "main",
  "level": "INFO",
  "caller_class_name": "com.empress.usermanagementapi.service.UserService",
  "caller_method_name": "create",
  "caller_file_name": "UserService.java",
  "caller_line_number": 46,
  "application": "user-management-api"
}
```

### HTTP Request
```json
{
  "timestamp": "2026-01-09T14:54:30.492684224Z",
  "@version": "1",
  "message": "Incoming HTTP request - method: POST, uri: /users, remoteAddr: 127.0.0.1",
  "logger": "com.empress.usermanagementapi.config.LoggingInterceptor",
  "thread": "main",
  "level": "INFO",
  "caller_class_name": "com.empress.usermanagementapi.config.LoggingInterceptor",
  "caller_method_name": "preHandle",
  "caller_file_name": "LoggingInterceptor.java",
  "caller_line_number": 25,
  "application": "user-management-api"
}
```

### Validation Error
```json
{
  "timestamp": "2026-01-09T14:54:30.494690746Z",
  "@version": "1",
  "message": "Validation failed - field count: 1",
  "logger": "com.empress.usermanagementapi.exception.GlobalExceptionHandler",
  "thread": "main",
  "level": "WARN",
  "caller_class_name": "com.empress.usermanagementapi.exception.GlobalExceptionHandler",
  "caller_method_name": "handleValidationException",
  "caller_file_name": "GlobalExceptionHandler.java",
  "caller_line_number": 56,
  "application": "user-management-api"
}
```

## Logging Locations

### Service Layer (UserService)
- User creation with username, email, and role (password is never logged)
- User creation success with userId and username

### Controller Layer (UserController)
- Incoming requests with username (for create)
- Duplicate username/email warnings
- User creation success
- User update and delete operations

### HTTP Layer (LoggingInterceptor)
- All incoming HTTP requests with method, URI, and remote address
- All completed HTTP requests with status code and duration
- Request headers (excluding sensitive headers like Authorization and Cookie)

### Exception Handler (GlobalExceptionHandler)
- Validation failures with field count
- Individual field validation errors (at DEBUG level)
- Final validation error count

## Configuration

The logging configuration is in `src/main/resources/logback-spring.xml`.

To use development logging (human-readable), set the Spring profile to `local`:
```
spring.profiles.active=local
```

For production, use the default profile (or any profile other than `local`).

## Testing

Tests for the logging functionality are located in:
- `src/test/java/com/empress/usermanagementapi/config/LoggingInterceptorTest.java`

All existing tests continue to pass with logging enabled.
