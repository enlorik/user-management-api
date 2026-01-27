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
- **`/login`**: 10 requests per minute
- **`/register`**: 20 requests per 10 minutes  
- **`/verify-email`**: 30 requests per minute

The rate limiter:
- Uses token bucket algorithm (Bucket4j library)
- Returns HTTP 429 with `Retry-After` header when limits are exceeded
- Supports `X-Forwarded-For` header for proxy/load balancer scenarios
- Automatically cleans up expired buckets every 30 minutes

**Implementation**: See [`RateLimitFilter.java`](src/main/java/com/empress/usermanagementapi/filter/RateLimitFilter.java) and [`RateLimitConfig.java`](src/main/java/com/empress/usermanagementapi/config/RateLimitConfig.java)

### Password Security
- All passwords are encrypted using **BCrypt** hashing before storage
- Password validation enforced during registration and reset operations
- Secure token-based password reset mechanism

**Implementation**: See [`SecurityConfig.java`](src/main/java/com/empress/usermanagementapi/config/SecurityConfig.java) for BCrypt configuration and [`PasswordResetService.java`](src/main/java/com/empress/usermanagementapi/service/PasswordResetService.java) for token management

### Session-Based Authentication
- **Form-based login** with HTTP session management via Spring Security
- Session cookies for maintaining authenticated state
- CSRF protection for web forms (disabled for REST API endpoints)
- Role-based access control (ADMIN/USER roles)
- Email verification required before login
- See [`SecurityConfig.java`](src/main/java/com/empress/usermanagementapi/config/SecurityConfig.java) for security configuration

### Logging and Sanitization
- Structured JSON logging with contextual metadata (request ID, user ID, action type)
- Automatic masking of sensitive data (passwords, emails, tokens)
- AI-powered log summarization via `/api/v1/logs/summarize` endpoint (optional OpenAI integration)
- Rule-based log analysis with automatic fallback when AI is unavailable
- See [LOGGING.md](LOGGING.md) for detailed logging documentation

### API Endpoints
The application provides both web pages and REST API endpoints:

**Web Pages**:
- `GET /login` - Login page ([`PageController.java`](src/main/java/com/empress/usermanagementapi/config/PageController.java))
- `GET /register` - Registration page
- `GET /admin` - Admin dashboard (admin only)
- `GET /user` - User dashboard (authenticated users)

**User Management REST API** (admin only):
- `GET /users` - List all users ([`UserController.java`](src/main/java/com/empress/usermanagementapi/controller/UserController.java))
- `POST /users` - Create a new user
- `PUT /users/{id}` - Update user details
- `DELETE /users/{id}` - Delete a user

**Authentication & Registration**:
- `POST /register` - Register new account with email verification ([`RegistrationController.java`](src/main/java/com/empress/usermanagementapi/controller/RegistrationController.java))
- `GET /verify-email?token=...` - Verify email address ([`EmailVerificationController.java`](src/main/java/com/empress/usermanagementapi/controller/EmailVerificationController.java))
- `POST /forgot-password` - Request password reset ([`ForgotPasswordController.java`](src/main/java/com/empress/usermanagementapi/controller/ForgotPasswordController.java))
- `POST /reset-password?token=...` - Reset password with token ([`ResetPasswordController.java`](src/main/java/com/empress/usermanagementapi/controller/ResetPasswordController.java))

**Self-Service REST API** (authenticated users):
- `GET /users/me` - Get own user information
- `PUT /users/me` - Update own email and password

**Log Analysis REST API** (admin only):
- `GET /api/v1/logs/summarize` - Get AI-powered log summaries with filtering options ([`LogController.java`](src/main/java/com/empress/usermanagementapi/controller/LogController.java))

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

### Railway Deployment

This application is optimized for deployment on Railway with PostgreSQL. The configuration handles connection pooling, database migrations, and collation version management automatically.

#### Database Configuration

**HikariCP Connection Pool** (Production)
- Connection validation before use (`SELECT 1` test query)
- Short connection lifetime (10 minutes) to prevent stale connections
- Optimized pool size (5 connections) for Railway limits
- Automatic leak detection and connection recycling

**Flyway Database Migrations**
- Automatically runs PostgreSQL-specific migrations on startup
- Handles collation version mismatches (common on Railway)
- Gracefully skips migrations in non-PostgreSQL environments
- Creates schema history table if it doesn't exist

#### Environment Configuration

The application supports three environment profiles:

1. **Production** (`application.properties`) - Default for Railway
   - PostgreSQL database with Flyway migrations enabled
   - HikariCP connection pooling optimized for Railway
   - Hibernate DDL mode: `validate` (schema managed by Flyway migrations)

2. **Local Development** (`application-local.properties`) - Activated with `--spring.profiles.active=local`
   - H2 in-memory database
   - Flyway migrations disabled
   - Hibernate DDL mode: `create-drop` (recreates schema on startup)

3. **Testing** (`test/resources/application.properties`) - Automatic for tests
   - H2 in-memory database
   - Flyway migrations disabled
   - Minimal HikariCP pooling (1 connection)
   - Hibernate DDL mode: `create-drop`

#### Railway Environment Variables

The following environment variables can be set in Railway to customize the application:

```bash
# Database Connection (automatically set by Railway PostgreSQL)
PGHOST=<railway-host>
PGPORT=<railway-port>
PGDATABASE=<railway-database>
PGUSER=<railway-user>
PGPASSWORD=<railway-password>

# HikariCP Connection Pool (optional, defaults shown)
HIKARI_MAX_POOL_SIZE=5               # Maximum connections in pool
HIKARI_MIN_IDLE=2                    # Minimum idle connections
HIKARI_CONNECTION_TIMEOUT=20000      # Connection timeout (20000ms = 20 sec)
HIKARI_IDLE_TIMEOUT=300000           # Idle timeout (300000ms = 5 min)
HIKARI_MAX_LIFETIME=600000           # Max lifetime (600000ms = 10 min)

# Email Configuration (required for email verification and password reset)
RESEND_API_KEY=<your-resend-key>     # For email functionality
RESEND_FROM=<sender-email>           # Email sender address

# OpenAI Integration (optional, for AI-powered log summarization)
OPENAI_API_KEY=<your-openai-key>     # Optional: enables AI log summarization
```

#### Troubleshooting Railway Deployment

**Problem**: "Failed to validate connection" or "connection has been closed"  
**Solution**: The HikariCP configuration automatically handles this with connection validation and recycling. If issues persist:
- Check Railway logs for connection pool exhaustion
- Verify PostgreSQL instance is running and accessible
- Consider adjusting `HIKARI_MAX_POOL_SIZE` if seeing "Connection is not available" errors

**Problem**: "database has a collation version mismatch"  
**Solution**: The Flyway migration `V1__refresh_collation_version.sql` automatically fixes this on deployment. If the migration fails:
- Check Flyway migration history: `SELECT * FROM flyway_schema_history;`
- Manually refresh collation (requires superuser): `ALTER DATABASE <dbname> REFRESH COLLATION VERSION;`
- Use Flyway repair command if needed: `mvn flyway:repair` or via Railway CLI
- As a last resort, use Flyway baseline to mark migration as completed: `mvn flyway:baseline`

**Problem**: Schema validation errors on startup  
**Solution**: The application uses Hibernate `ddl-auto=validate` with Flyway managing schema changes. If you see validation errors:
- Check that Flyway migrations have run successfully
- Verify database user has sufficient privileges
- Check Railway logs for detailed error messages

**Problem**: Tests fail locally or in CI  
**Solution**: Test environment is isolated from production configuration:
- Tests use H2 in-memory database, not PostgreSQL
- Flyway migrations are automatically disabled for tests
- HikariCP uses minimal pooling (1 connection) for tests
- No Railway-specific configuration should affect tests

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

### Railway Deployment Notes

#### Database Connection Configuration

The application is configured with optimized HikariCP settings for Railway's PostgreSQL:

- **Connection validation**: Connections are tested before use to prevent stale connection errors
- **Short connection lifetime**: 10 minutes (prevents Railway from closing idle connections)
- **Small pool size**: 5 connections maximum (respects Railway's connection limits)
- **Automatic collation updates**: Flyway migration handles PostgreSQL collation version mismatches

#### Environment Variables for Railway

The following environment variables are supported for tuning (defaults are production-ready):

```bash
HIKARI_MAX_POOL_SIZE=5           # Maximum connection pool size
HIKARI_MIN_IDLE=2                # Minimum idle connections
HIKARI_CONNECTION_TIMEOUT=20000  # Connection timeout in milliseconds
HIKARI_MAX_LIFETIME=600000       # Max connection lifetime (10 minutes)
HIKARI_IDLE_TIMEOUT=300000       # Idle timeout (5 minutes)
```

#### Common Railway Issues

**Problem**: "Failed to validate connection" or "connection has been closed"
**Solution**: The HikariCP configuration automatically handles this by validating connections before use.
- If the issue persists, check Railway logs for connection pool exhaustion
- Consider adjusting `HIKARI_MAX_POOL_SIZE` if you see "Connection is not available" errors
- Verify that your Railway PostgreSQL instance is running and accessible

**Problem**: "database has a collation version mismatch"
**Solution**: The Flyway migration `V1__refresh_collation_version.sql` automatically fixes this on deployment.
- If the migration fails, you can manually run: `ALTER DATABASE your_database_name REFRESH COLLATION VERSION;`
- Check Flyway migration history with: `SELECT * FROM flyway_schema_history;`
- To force re-run, delete the failed entry from `flyway_schema_history` and redeploy

## Session-Based Authentication

This application uses traditional **session-based authentication** with Spring Security, providing secure user authentication through HTTP sessions and cookies.

### How It Works

1. **User submits credentials** via the `/login` form
2. **Spring Security validates** credentials against the database (BCrypt password verification)
3. **Session is created** upon successful authentication
4. **Session cookie** (JSESSIONID) is sent to the browser
5. **Subsequent requests** automatically include the session cookie for authentication

### Security Features

- **BCrypt password hashing**: All passwords encrypted before storage
- **Session management**: Secure HTTP session handling via Spring Security
- **CSRF protection**: Enabled for web forms, disabled for REST API endpoints (`/users/**`, `/api/**`)
- **Role-based access control**: ADMIN and USER roles with method-level security
- **Email verification**: Users cannot login until email is verified
- **Account locking**: Unverified accounts are automatically disabled

### Role-Based Access Control

**USER Role** (Standard users):
- Access to `/user` dashboard
- `GET /users/me` - View own profile
- `PUT /users/me` - Update own profile  
- **Access denied**: Admin-only endpoints

**ADMIN Role** (Administrators):
- Access to `/admin` dashboard
- All USER permissions
- `GET /users` - List all users
- `POST /users` - Create new users
- `PUT /users/{id}` - Update any user
- `DELETE /users/{id}` - Delete users
- `GET /api/v1/logs/summarize` - View log summaries

### Authentication Flow Example

**Web Application Flow:**
```bash
# 1. Navigate to login page
curl http://localhost:8080/login

# 2. Submit credentials (browser automatically handles session cookies)
# After successful login, user is redirected to /admin or /user dashboard

# 3. Access protected resources (session cookie included automatically)
curl http://localhost:8080/users/me --cookie "JSESSIONID=<session-id>"
```

**REST API Flow:**
```bash
# 1. Authenticate and get session cookie
curl -X POST http://localhost:8080/login \
  -c cookies.txt \
  -d "username=user@example.com" \
  -d "password=YourPassword123!"

# 2. Use session cookie for API requests
curl -X GET http://localhost:8080/users/me \
  -b cookies.txt

# 3. Update profile
curl -X PUT http://localhost:8080/users/me \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"email": "newemail@example.com"}'
```

### Configuration Reference

The security configuration is defined in [`SecurityConfig.java`](src/main/java/com/empress/usermanagementapi/config/SecurityConfig.java):

- **Lines 28-68**: Security filter chain configuration
- **Lines 58-62**: Form login configuration with custom success handler
- **Lines 71-77**: Login success handler (redirects based on role)
- **Lines 80-82**: BCrypt password encoder bean
- **Lines 91-109**: User details service with email verification check

### Testing Authentication

The repository includes comprehensive authentication tests:

```bash
# Run session-based authentication tests
mvn test -Dtest=SessionBasedAuthTest

# Run user controller validation tests
mvn test -Dtest=UserControllerValidationTest

# Run CSRF protection tests
mvn test -Dtest=CsrfProtectionTest

# Run all configuration tests
mvn test -Dtest="**/config/**Test"
```

### Troubleshooting

**401 Unauthorized Errors:**
- User is not logged in or session has expired
- Navigate to `/login` to authenticate
- Verify the user's email is verified

**403 Forbidden Errors:**
- User doesn't have the required role for the endpoint
- Admin-only endpoints require ADMIN role
- Check role assignment in the database

**Session Expiration:**
- Sessions expire after period of inactivity (default: 30 minutes)
- User must log in again after session expires
- Configure session timeout in `application.properties` if needed

## Additional Documentation

- **[LOGGING.md](LOGGING.md)** - Comprehensive logging and log analysis guide
- **Swagger UI** - Interactive API documentation at `/swagger-ui.html`

## License

This project is for educational and demonstration purposes.
