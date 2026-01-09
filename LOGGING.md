# Logging Integration

This document describes the comprehensive structured logging functionality integrated into the user-management API, designed for AI summarization readiness and centralized log analysis.

## Features

### 1. Structured JSON Logging
The application uses Logback with the Logstash encoder to produce structured JSON logs that can be easily consumed by log aggregation tools, centralized logging systems, and AI summarizers. All logs include standardized metadata fields for enhanced analysis and correlation.

### 2. Contextual Metadata
Each log entry automatically includes contextual metadata using SLF4J's Mapped Diagnostic Context (MDC):
- `requestId`: Unique identifier for tracing requests across the system
- `userId`: User identifier for user-specific actions (when available)
- `actionType`: Type of action being performed (e.g., USER_LOGIN, USER_REGISTRATION, USER_CREATE, PASSWORD_RESET)
- `httpStatus`: HTTP status code for the response
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

### 3. Sensitive Data Masking
Passwords and other sensitive information are automatically masked in logs to prevent security issues and comply with privacy best practices. The following fields are masked:
- `password`, `passwordHash`, `rawPassword`
- `token`, `apiKey`, `authorization`
- `email` (partially masked in structured fields)
- `ssn`, `socialSecurityNumber`
- `creditCard`, `cardNumber`
- `phone`, `phoneNumber`

Email addresses in log messages are automatically masked using the `LoggingUtil.maskEmail()` method (e.g., `john.doe@example.com` → `j***e@e***.com`).

### 4. Comprehensive Logging Coverage

#### HTTP Layer (LoggingInterceptor)
- All incoming HTTP requests with method, URI, remote address, and unique requestId
- All completed HTTP requests with status code, duration, and metadata
- Request headers (excluding sensitive headers like Authorization and Cookie)
- HTTP status code automatically added to MDC for correlation

#### Controller Layer
**UserController:**
- User creation requests with username and masked email
- Duplicate username/email warnings
- User creation success with userId
- User update and delete operations with userId and actionType metadata

**LoginController:**
- Login attempts with username
- Login success/failure with detailed error messages
- Authentication failures (bad credentials, unexpected errors)

**RegistrationController:**
- Registration attempts with username and masked email
- Validation failures with error counts
- Duplicate username/email detection
- Email verification token creation and sending
- User registration success with userId

#### Service Layer
**UserService:**
- User creation with username, masked email, and role
- User creation success with userId and username

**EmailVerificationService:**
- Email verification token creation with userId
- Token refresh operations
- Email verification attempts
- Token validation (invalid, expired, already used)
- Successful email verification with userId

**PasswordResetService:**
- Password reset token requests with masked email
- Token creation success with userId
- Password reset attempts
- Token validation (invalid, expired, already used)
- Successful password reset with userId

**Exception Handler (GlobalExceptionHandler):**
- Validation failures with field count
- Individual field validation errors (at DEBUG level)
- Final validation error count

### 5. Environment-Specific Configuration

#### Development (`local` profile)
- Human-readable console logs
- DEBUG level for application code (`com.empress.usermanagementapi`)
- DEBUG level for Spring Web and Spring Security
- Plain text format for easy reading

#### Testing (`test` profile)
- JSON formatted logs to console
- DEBUG level for application code
- INFO level for Spring Web and Spring Security
- Comprehensive logging for test analysis

#### Production (default profile)
- JSON formatted logs to console and file
- INFO level for application code
- WARN level for Spring Web and Hibernate
- Logs rotate daily and keep 30 days of history
- Reduced noise for production operations

### 6. Custom Logging Utilities
The application provides a `LoggingUtil` class for consistent metadata management:
- `generateRequestId()`: Generate unique request IDs
- `setUserId(userId)`: Set user context for logs
- `setActionType(action)`: Set action type for log categorization
- `setHttpStatus(status)`: Set HTTP status code
- `maskEmail(email)`: Mask email addresses for privacy
- `maskSensitiveData(data)`: Mask generic sensitive data
- `clearMdc()`: Clear MDC context to prevent data leakage

## Example Log Entries

### User Creation (with metadata and masked fields)
```json
{
  "timestamp": "2026-01-09T22:21:28.631769207Z",
  "@version": "1",
  "message": "Received request to create user - username: testuser, email: t**t@e***m",
  "logger": "com.empress.usermanagementapi.controller.UserController",
  "thread": "main",
  "level": "INFO",
  "actionType": "USER_CREATE",
  "requestId": "2c11470e-584d-4e5d-b2d7-888321c9d2ce",
  "caller_class_name": "com.empress.usermanagementapi.controller.UserController",
  "caller_method_name": "createUser",
  "caller_file_name": "UserController.java",
  "caller_line_number": 72,
  "application": "user-management-api"
}
```

### User Registration Success (with userId metadata)
```json
{
  "timestamp": "2026-01-09T22:21:28.632968936Z",
  "@version": "1",
  "message": "User created successfully - userId: 1, username: testuser",
  "logger": "com.empress.usermanagementapi.controller.UserController",
  "thread": "main",
  "level": "INFO",
  "actionType": "USER_CREATE",
  "userId": "1",
  "requestId": "2c11470e-584d-4e5d-b2d7-888321c9d2ce",
  "caller_class_name": "com.empress.usermanagementapi.controller.UserController",
  "caller_method_name": "createUser",
  "caller_file_name": "UserController.java",
  "caller_line_number": 105,
  "application": "user-management-api"
}
```

### HTTP Request with Metadata
```json
{
  "timestamp": "2026-01-09T22:21:28.381359359Z",
  "@version": "1",
  "message": "Incoming HTTP request - method: POST, uri: /register, remoteAddr: 127.0.0.1",
  "logger": "com.empress.usermanagementapi.config.LoggingInterceptor",
  "thread": "main",
  "level": "INFO",
  "requestId": "49bc48ed-b685-4e26-b777-ccec6e3b776b",
  "caller_class_name": "com.empress.usermanagementapi.config.LoggingInterceptor",
  "caller_method_name": "preHandle",
  "caller_file_name": "LoggingInterceptor.java",
  "caller_line_number": 30,
  "application": "user-management-api"
}
```

### HTTP Response with Status and Duration
```json
{
  "timestamp": "2026-01-09T22:21:28.63502668Z",
  "@version": "1",
  "message": "HTTP request completed - method: POST, uri: /users, status: 201, duration: 24ms",
  "logger": "com.empress.usermanagementapi.config.LoggingInterceptor",
  "thread": "main",
  "level": "INFO",
  "requestId": "2c11470e-584d-4e5d-b2d7-888321c9d2ce",
  "httpStatus": "201",
  "caller_class_name": "com.empress.usermanagementapi.config.LoggingInterceptor",
  "caller_method_name": "afterCompletion",
  "caller_file_name": "LoggingInterceptor.java",
  "caller_line_number": 83,
  "application": "user-management-api"
}
```

### Validation Error with Action Type
```json
{
  "timestamp": "2026-01-09T22:21:55.714442120Z",
  "@version": "1",
  "message": "Registration validation failed - username: validuser, errorCount: 1",
  "logger": "com.empress.usermanagementapi.controller.RegistrationController",
  "thread": "main",
  "level": "WARN",
  "actionType": "USER_REGISTRATION",
  "requestId": "5fae455f-c66a-485d-93d6-05e23b33933d",
  "caller_class_name": "com.empress.usermanagementapi.controller.RegistrationController",
  "caller_method_name": "registerSubmit",
  "caller_file_name": "RegistrationController.java",
  "caller_line_number": 80,
  "application": "user-management-api"
}
```

### Password Reset with User Context
```json
{
  "timestamp": "2026-01-09T14:54:30.508234987Z",
  "@version": "1",
  "message": "Password reset successful - userId: 5, username: john_doe",
  "logger": "com.empress.usermanagementapi.service.PasswordResetService",
  "thread": "main",
  "level": "INFO",
  "actionType": "PASSWORD_RESET",
  "userId": "5",
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "caller_class_name": "com.empress.usermanagementapi.service.PasswordResetService",
  "caller_method_name": "resetPassword",
  "caller_file_name": "PasswordResetService.java",
  "caller_line_number": 125,
  "application": "user-management-api"
}
```

## Logging Locations

### HTTP Layer (LoggingInterceptor)
- All incoming HTTP requests with method, URI, remote address, and unique requestId
- All completed HTTP requests with status code, duration, and httpStatus in MDC
- Request headers (excluding sensitive headers like Authorization and Cookie)

### Service Layer

**UserService:**
- User creation with username, email, and role (password is never logged)
- User creation success with userId and username

**EmailVerificationService:**
- Email verification token creation with userId and action type
- Token refresh operations
- Email verification attempts
- Token validation failures (invalid, expired, already used)
- Successful email verification with userId

**PasswordResetService:**
- Password reset token requests with masked email
- Token creation success with userId
- Password reset attempts
- Token validation failures (invalid, expired, already used)
- Successful password reset with userId

### Controller Layer

**UserController:**
- Incoming requests with username and masked email (for create)
- Duplicate username/email warnings
- User creation success with userId
- User update and delete operations with userId and actionType

**LoginController:**
- Login attempts with username
- Login success/failure
- Authentication failures with error details

**RegistrationController:**
- Registration attempts with username and masked email
- Validation failures with error counts
- Duplicate username/email detection
- User registration success with userId
- Email verification token creation and sending

### Exception Handler (GlobalExceptionHandler)
- Validation failures with field count
- Individual field validation errors (at DEBUG level)
- Final validation error count

## Action Types

The following action types are used for log categorization:
- `USER_LOGIN`: User login attempts
- `USER_REGISTRATION`: User registration process
- `USER_CREATE`: User creation via API
- `USER_UPDATE`: User profile updates
- `USER_DELETE`: User deletion
- `EMAIL_VERIFICATION`: Email verification process
- `EMAIL_VERIFICATION_TOKEN_CREATE`: Email verification token creation
- `PASSWORD_RESET`: Password reset process
- `PASSWORD_RESET_TOKEN_CREATE`: Password reset token creation

## Privacy and Security

### Sensitive Data Masking
- All password fields are masked in JSON output
- Email addresses are partially masked in log messages (e.g., `j***e@e***.com`)
- Personal identifiable information (PII) is masked:
  - Social Security Numbers (SSN)
  - Credit card numbers
  - Phone numbers
- API keys and authorization tokens are masked
- Sensitive headers (Authorization, Cookie) are excluded from request logs

### MDC Context Cleanup
The MDC context is automatically cleared at the end of each request in the LoggingInterceptor to prevent data leakage between requests in thread-pooled environments.

## Configuration

The logging configuration is in `src/main/resources/logback-spring.xml`.

### Switching Profiles

**Development (human-readable logs):**
```bash
spring.profiles.active=local
```

**Testing (JSON logs with DEBUG level):**
```bash
spring.profiles.active=test
```

**Production (JSON logs with INFO level):**
```bash
# No profile or any profile other than 'local' or 'test'
spring.profiles.active=production
```

### Custom Configuration

You can customize logging behavior in `application.properties` or `application-{profile}.properties`:

```properties
# Set specific logger levels
logging.level.com.empress.usermanagementapi=DEBUG
logging.level.org.springframework.web=INFO

# Email service logging
logging.level.com.empress.usermanagementapi.service.EmailService=INFO
```

## Best Practices

1. **Always use metadata:** Set actionType and userId when available for better log correlation
2. **Mask sensitive data:** Use `LoggingUtil.maskEmail()` and `LoggingUtil.maskSensitiveData()` for PII
3. **Clear MDC context:** Always clear MDC after completing contextual operations
4. **Use appropriate log levels:**
   - `DEBUG`: Detailed information for debugging
   - `INFO`: Important business events and successful operations
   - `WARN`: Warning conditions and validation failures
   - `ERROR`: Error conditions and exceptions
5. **Include context in messages:** Provide enough information to understand the log without looking at other logs

## AI Summarization Readiness

The enhanced structured logging is designed to be AI-ready:
- **Consistent metadata fields** enable AI models to understand context
- **Action types** provide semantic meaning for log categorization
- **Request IDs** enable tracing and correlation across distributed systems
- **User IDs** enable user-specific analysis and behavior tracking
- **Structured JSON** is easily parseable by AI models and analytics tools
- **Masked sensitive data** ensures privacy compliance in AI training datasets

## Testing

Tests for the logging functionality are located in:
- `src/test/java/com/empress/usermanagementapi/config/LoggingInterceptorTest.java`

All existing tests continue to pass with enhanced logging enabled.
