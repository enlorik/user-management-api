# user-management-api

A streamlined Spring Boot REST API for managing user accounts with authentication, registration, and profile updates. 

## About
- Authentication & Authorization
- Registration with Email Verification
- Password Reset Functionality
- User Profile Management
- AI-powered Log Summarization

## Features Overview
### Rate Limiting
- Protects endpoints with IP-based limits.
- Token bucket algorithm (Bucket4j).
- E.g., `/login`: 10 req/min.

### Secure Password Handling
- **BCrypt** password hashing.
- Secure token-based reset mechanism.

### Simplified Session-Based Authentication
- HTTP-based sessions with role-based access (ADMIN/USER).
- Email verification required for login.

### Structured Logging
- JSON-based logs with sensitive data masking.
- Optional AI log summaries (`/api/v1/logs/summarize`).

## REST API: Key Endpoints
**Public**:
- `/register`: New user registration.
- `/verify-email`: Verify account.

**Authenticated Users**:
- `GET /users/me`: View profile.
- `PUT /users/me`: Update profile.

**Admin Only**:
- `GET /users`: List users.
- `DELETE /users/{id}`: Remove user.

## Requirements
- **Java 17+**, **Spring Boot 3.4.5**
- **Maven 3.6+**
- Database: PostgreSQL or H2 (local dev).

## Quick Setup
### Local Run (H2 Database):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
### Docker (PostgreSQL Required):
```bash
docker run -d -p 8080:8080 -e DB_SETTINGS ... user-management-api
```

## License
For demonstration purposes only.