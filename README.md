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

### JWT Authentication
- **Stateless authentication** using JSON Web Tokens (JWT)
- Token-based API authentication for RESTful operations
- Configurable token expiration (default: 24 hours)
- HS256 algorithm for token signing with secure secret key
- Role-based access control with JWT tokens
- See [JWT Authentication Guide](#jwt-authentication) for usage details

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
- `POST /auth/login` - Authenticate and receive JWT token
- `POST /register` - Register new account with email verification
- `POST /login` - Web form login
- `GET /verify-email?token=...` - Verify email address
- `POST /forgot-password` - Request password reset
- `POST /reset-password?token=...` - Reset password with token

**Self-Service** (authenticated users):
- `GET /users/me` - Get own user information
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

The following environment variables can be set in Railway to customize database behavior:

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

# Other required environment variables
JWT_SECRET=<your-secret-key>         # Generate with: openssl rand -base64 64
RESEND_API_KEY=<your-resend-key>     # For email functionality
RESEND_FROM=<sender-email>           # Email sender address
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

## JWT Authentication

This API uses JWT (JSON Web Tokens) for stateless authentication. JWT tokens are issued on successful login and must be included in subsequent API requests.

### Configuration

JWT authentication requires two environment variables:

```bash
# JWT secret key for signing tokens (minimum 256 bits / 32 bytes)
# Generate a secure key using: openssl rand -base64 64
export JWT_SECRET="your-secure-secret-key-here"

# JWT token expiration time in milliseconds (default: 24 hours)
export JWT_EXPIRATION=86400000
```

**Important**: Always use a strong, randomly generated secret in production. Never commit secrets to version control.

### Authentication Flow

#### 1. Login and Obtain JWT Token

**Request:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@example.com",
    "password": "YourPassword123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjc5...",
  "username": "user@example.com",
  "role": "USER"
}
```

#### 2. Use JWT Token for API Requests

Include the token in the `Authorization` header with `Bearer` prefix:

```bash
# Get own user information
curl -X GET http://localhost:8080/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Update own information
curl -X PUT http://localhost:8080/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newemail@example.com"
  }'
```

### Role-Based Access Control

The API enforces role-based access control using JWT tokens:

**USER Role** (Standard users):
- `GET /users/me` - View own profile
- `PUT /users/me` - Update own profile  
- **Access denied**: Admin-only endpoints (users CRUD)

**ADMIN Role** (Administrators):
- All USER permissions
- `GET /users` - List all users
- `POST /users` - Create new users
- `PUT /users/{id}` - Update any user
- `DELETE /users/{id}` - Delete users
- `GET /api/v1/logs/summarize` - View log summaries

### Security Features

- **Stateless authentication**: No server-side session storage required
- **Token expiration**: Tokens automatically expire after configured time
- **Secure signing**: HS256 algorithm with minimum 256-bit secret key
- **Role validation**: Automatic role extraction and validation from tokens
- **Email verification**: Unverified accounts cannot obtain tokens

### Testing JWT Authentication

The repository includes comprehensive JWT authentication tests:

```bash
# Run JWT authentication tests
mvn test -Dtest=JwtAuthenticationTest

# Run all security-related tests
mvn test -Dtest="**/controller/JwtAuthenticationTest,**/config/SecurityConfigTest"
```

### Troubleshooting

**401 Unauthorized Errors:**
- Check that the token is included in the `Authorization` header
- Verify the token hasn't expired (default: 24 hours)
- Ensure the token uses the `Bearer ` prefix
- Confirm the user's email is verified

**403 Forbidden Errors:**
- The user doesn't have the required role for the endpoint
- Admin-only endpoints require ADMIN role

**500 Internal Server Error:**
- Check that `JWT_SECRET` is configured and at least 32 bytes long
- Verify `JWT_EXPIRATION` is a valid number (milliseconds)

## Additional Documentation

- **[LOGGING.md](LOGGING.md)** - Comprehensive logging and log analysis guide
- **Swagger UI** - Interactive API documentation at `/swagger-ui.html`

## License

This project is for educational and demonstration purposes.
