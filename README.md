# user-management-api

A Spring Boot REST API for managing user accounts with authentication, registration, profile updates, and log processing capabilities.

## About

This API provides comprehensive user management functionality including:
- User authentication and authorization with role-based access control
- User registration with email verification
- Password reset functionality
- User profile management
- Structured logging with AI-powered log summarization

## Features

### Rate Limiting
Critical public endpoints are protected with IP-based rate limiting to prevent brute force attacks:
- **`/login`** and **`/auth/login`**: 10 requests per minute
- **`/register`**: 20 requests per 10 minutes  
- **`/verify-email`**: 30 requests per minute

The rate limiter:
- Uses token bucket algorithm (Bucket4j library)
- Returns HTTP 429 with `Retry-After` header when limits are exceeded
- Supports `X-Forwarded-For` header for proxy/load balancer scenarios
- Automatically cleans up expired buckets every 30 minutes

### Password Security
- All passwords are encrypted using **BCrypt** hashing before storage
- Password validation enforced during registration and reset operations
- Secure token-based password reset mechanism

### Logging and Sanitization
- Structured JSON logging with contextual metadata (request ID, user ID, action type)
- Automatic masking of sensitive data (passwords, emails, tokens)
- AI-powered log summarization via `/api/v1/logs/summarize` endpoint (optional OpenAI integration)
- Rule-based log analysis with automatic fallback when AI is unavailable
- See [LOGGING.md](LOGGING.md) for detailed logging documentation

### API Endpoints
The REST API provides the following core endpoints:

**User Management** (admin only):
- `GET /users` - List all users
- `POST /users` - Create a new user
- `PUT /users/{id}` - Update user details
- `DELETE /users/{id}` - Delete a user

**Authentication**:
- `POST /register` - Register new account with email verification
- `POST /login` - Authenticate user
- `GET /verify-email?token=...` - Verify email address
- `POST /forgot-password` - Request password reset
- `POST /reset-password?token=...` - Reset password with token

**Self-Service**:
- `PUT /users/me` - Update own email and password

**Log Analysis** (admin only):
- `GET /api/v1/logs/summarize` - Get AI-powered log summaries with filtering options

For detailed API documentation, see the Swagger UI at `/swagger-ui.html` when running the application.

## Requirements

- **Java 17** or higher
- **Spring Boot 3.4.5**
- **Maven 3.6+**
- **PostgreSQL 12+** (or H2 in-memory database for local development)

### Dependencies
Key Spring Boot dependencies:
- `spring-boot-starter-web` - RESTful web services
- `spring-boot-starter-security` - Authentication and authorization
- `spring-boot-starter-data-jpa` - Database persistence
- `spring-boot-starter-mail` - Email functionality
- `spring-boot-starter-actuator` - Health checks and monitoring
- `bucket4j-core` - Rate limiting
- `springdoc-openapi-starter-webmvc-ui` - API documentation

## Getting Started

### Quick Start with Maven

**Using H2 in-memory database (no setup required):**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Then open `http://localhost:8080` in your browser.

### Running with PostgreSQL

1. **Start PostgreSQL** (using Docker):
   ```bash
   docker run --name user-mgmt-postgres \
     -e POSTGRES_DB=userdb \
     -e POSTGRES_USER=dbuser \
     -e POSTGRES_PASSWORD=dbpass \
     -p 5432:5432 \
     -d postgres:15-alpine
   ```

2. **Set environment variables**:
   ```bash
   export PGHOST=localhost
   export PGPORT=5432
   export PGDATABASE=userdb
   export PGUSER=dbuser
   export PGPASSWORD=dbpass
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

### Running with Docker

1. **Build the image**:
   ```bash
   docker build -t user-management-api .
   ```

2. **Run the container**:
   ```bash
   docker run -d -p 8080:8080 \
     -e PGHOST=your-db-host \
     -e PGPORT=5432 \
     -e PGDATABASE=userdb \
     -e PGUSER=dbuser \
     -e PGPASSWORD=dbpass \
     user-management-api
   ```

### Running from JAR

1. **Build the application**:
   ```bash
   mvn clean package
   ```

2. **Run the JAR**:
   ```bash
   java -jar target/user-management-api-0.0.1-SNAPSHOT.jar
   ```

### Testing

Run all tests:
```bash
mvn test
```

Run specific test categories:
```bash
# Unit tests only
mvn test -Dtest="**/controller/**Test"

# Integration tests only
mvn test -Dtest="**/config/**Test,**/service/**Test,**/*ApplicationTests"
```

## Additional Documentation

- **[LOGGING.md](LOGGING.md)** - Comprehensive logging and log analysis guide
- **Swagger UI** - Interactive API documentation at `/swagger-ui.html`

## License

This project is for educational and demonstration purposes.
