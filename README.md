# User Management API

A Spring Boot application for user registration, login, role-based access control, and simple account self-service.

Deployed on Railway:  
[user-management-api](https://user-management-api-java.up.railway.app/)

Tech stack:
- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL (Railway)
- Maven
- Docker
- Resend (for sending emails)
- Railway (hosting for app and database)

What the app does:
- User registration
  - stores users in PostgreSQL
  - passwords are hashed with BCrypt
  - sends an email verification link after registration
- Login
  - form login at `/login`
  - users can only log in after verifying their email
  - redirect:
    - admin users go to `/admin`
    - normal users go to `/user`
- Email verification
  - generates a unique token per user
  - sends a link `/verify-email?token=...`
  - when the link is opened:
    - marks the token as used
    - marks the user as verified
- Password reset
  - `/forgot-password` form (username and email must match)
  - creates a password reset token and emails a link
  - `/reset-password?token=...` page to set a new password
- User management
  - roles: `ADMIN` and `USER`
  - `/admin` dashboard:
    - shows all users sorted by id
    - uses the `/users` REST API in the background
  - `/user` dashboard:
    - user can change their own email and password
    - email change checks uniqueness

Security rules (high level):
- public:
  - `/login`
  - `/register`
  - `/verify-email/**`
  - `/forgot-password/**`
  - `/reset-password/**`
  - `/css/**` and `/js/**`
- requires `ROLE_ADMIN`:
  - `/admin/**`
- requires `ROLE_USER` or `ROLE_ADMIN`:
  - `/user/**`
- all other endpoints require authentication
- unverified users are treated as disabled accounts and cannot log in

## Security Features

### CSRF Protection

This application implements CSRF (Cross-Site Request Forgery) protection to secure form-based endpoints while maintaining stateless REST API functionality.

**Configuration:**
- **Form-based endpoints** (login, register, forgot-password, reset-password) require CSRF tokens
- **REST API endpoints** (`/users/*`, `/auth/*`) are exempt from CSRF protection as they are designed to be stateless

**CSRF Token Implementation:**
- All HTML forms automatically include CSRF tokens via Thymeleaf: `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>`
- Spring Security validates these tokens on form submission
- Missing or invalid CSRF tokens result in a 403 Forbidden response

**REST API Security:**
- REST APIs (`/users/*`, `/auth/*`) use stateless authentication
- These endpoints are configured to ignore CSRF protection
- API security relies on role-based access control and authentication
- Recommended to use additional security measures like JWT tokens for production REST APIs

**Testing:**
- All form submissions in tests must include `.with(csrf())` to simulate CSRF token inclusion
- See `CsrfProtectionTest.java` for comprehensive CSRF verification tests

**Best Practices:**
- Never disable CSRF for session-based authentication endpoints
- Use HTTPS in production to prevent man-in-the-middle attacks
- For SPAs consuming REST APIs, consider implementing token-based authentication (JWT)
- Regularly update Spring Security to get the latest security patches

### Rate Limiting

This application implements IP-based rate limiting on critical public endpoints to protect against brute force attacks and spam. Rate limiting uses the Bucket4j library with a token bucket algorithm.

**Protected Endpoints:**
- `/login` and `/auth/login`: 5 requests per minute
- `/register`: 10 requests per 15 minutes
- `/verify-email`: 20 requests per minute

**How It Works:**
- Each client IP address gets its own rate limit bucket per endpoint
- When the limit is exceeded, the server returns HTTP 429 (Too Many Requests) with a `Retry-After` header
- Buckets are isolated per endpoint, so limits on `/login` don't affect `/register`
- Expired buckets are automatically cleaned up every 30 minutes

**IP Address Detection:**
- The filter prioritizes the `X-Forwarded-For` header (for proxy/load balancer scenarios)
- Falls back to `RemoteAddr` if `X-Forwarded-For` is not available
- Extracts the first IP from `X-Forwarded-For` when multiple IPs are present

**Configuration:**
Rate limits are defined in `RateLimitConfig.java`. To modify limits:
1. Adjust the bandwidth configuration in the respective `createBucketFor*` methods
2. Modify the cleanup threshold by changing `CLEANUP_THRESHOLD_MS` constant

**Production Considerations:**
- Monitor rate limit logs to detect potential attacks
- Consider adjusting limits based on your use case and legitimate user behavior
- For distributed deployments, consider using a distributed cache (Redis) for bucket storage
- Rate limit logs are at WARN level when limits are exceeded and DEBUG level for successful requests

**Testing Rate Limits:**
```bash
# Test login rate limit (5 requests per minute)
for i in {1..6}; do curl -X POST http://localhost:8080/login; done

# The 6th request will return HTTP 429 with Retry-After header
```

## Development Setup

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (for production-like setup) OR use H2 in-memory database (for quick local testing)
- Resend API account (for email functionality)

### PostgreSQL Setup

You have several options for setting up PostgreSQL:

#### Option 1: Using Docker (Recommended for Development)

Quick start with a single Docker command:
```bash
docker run --name user-mgmt-postgres \
  -e POSTGRES_DB=userdb \
  -e POSTGRES_USER=dbuser \
  -e POSTGRES_PASSWORD=dbpass \
  -p 5432:5432 \
  -d postgres:15-alpine
```

Or using Docker Compose, create a `docker-compose.yml` file:
```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: user-mgmt-postgres
    environment:
      POSTGRES_DB: userdb
      POSTGRES_USER: dbuser
      POSTGRES_PASSWORD: dbpass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Then run:
```bash
docker-compose up -d
```

#### Option 2: Local PostgreSQL Installation

Install PostgreSQL for your OS:
- **macOS**: `brew install postgresql@15`
- **Ubuntu/Debian**: `sudo apt-get install postgresql postgresql-contrib`
- **Windows**: Download from [postgresql.org](https://www.postgresql.org/download/windows/)

Create the database:
```bash
psql -U postgres
CREATE DATABASE userdb;
CREATE USER dbuser WITH PASSWORD 'dbpass';
GRANT ALL PRIVILEGES ON DATABASE userdb TO dbuser;
```

#### Option 3: Using H2 In-Memory Database (Local Testing)

For quick local testing without PostgreSQL, use the provided `application-local.properties` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

This uses H2 in-memory database and dummy email configuration.

### Environment Variables

The application requires the following environment variables when running with PostgreSQL:

#### Database Configuration
- `PGHOST`: PostgreSQL host (e.g., `localhost`)
- `PGPORT`: PostgreSQL port (default: `5432`)
- `PGDATABASE`: Database name (e.g., `userdb`)
- `PGUSER`: Database username
- `PGPASSWORD`: Database password

#### Email Service - Resend (Recommended)
- `RESEND_API_KEY`: Your Resend API key (get one at [resend.com](https://resend.com))
- `RESEND_FROM`: Email sender address (e.g., `"User Management <onboarding@resend.dev>"`)

#### Email Service - Spring Mail (Alternative)
If not using Resend, configure Spring Mail:
- `SPRING_MAIL_HOST`: SMTP server host (e.g., `smtp.gmail.com`)
- `SPRING_MAIL_PORT`: SMTP server port (e.g., `587`)
- `SPRING_MAIL_USERNAME`: SMTP username
- `SPRING_MAIL_PASSWORD`: SMTP password
- `SPRING_MAIL_FROM`: Default sender email address

**Note**: For local development with the `local` profile, these variables are pre-configured with dummy values in `application-local.properties`.

### Building and Running

#### Build the application:
```bash
mvn clean package
```

#### Run with PostgreSQL (production mode):
Set environment variables and run:
```bash
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=userdb
export PGUSER=dbuser
export PGPASSWORD=dbpass
export RESEND_API_KEY=your-resend-api-key
export RESEND_FROM="User Management <onboarding@resend.dev>"

java -jar target/user-management-api-*.jar
```

#### Run with local profile (H2 database):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Then open `http://localhost:8080` in your browser.

## Security & Authentication

### Role-Based Access Control

The application implements role-based authentication with two roles:

- **`ADMIN`**: Full access to admin dashboard (`/admin/**`), can view and manage all users
- **`USER`**: Access to user dashboard (`/user/**`), can update their own email and password

### How Authentication Works

1. **Registration**: New users register at `/register` with username, email, and password
   - Passwords are hashed using BCrypt before storage
   - An email verification link is sent to the provided email
   - Users are assigned the `USER` role by default

2. **Email Verification**: Users must verify their email before logging in
   - Verification link format: `/verify-email?token=<unique-token>`
   - Unverified accounts are disabled and cannot log in

3. **Login**: Users log in at `/login` with username and password
   - Only verified users can successfully authenticate
   - After login, users are redirected based on their role:
     - `ADMIN` → `/admin` dashboard
     - `USER` → `/user` dashboard

4. **Password Reset**: Available at `/forgot-password`
   - Requires matching username and email
   - Sends a password reset link to the user's email
   - Link format: `/reset-password?token=<unique-token>`

### Default Credentials

**Note**: There are no default admin or user credentials. You must register a new account through the `/register` endpoint. To create an admin user, you'll need to manually update the role in the database after registration:

```sql
-- Connect to your database
UPDATE users SET role = 'ADMIN' WHERE username = 'your-username';
```

## Testing

The project includes comprehensive unit and integration tests.

### Running All Tests
```bash
mvn test
```

### Running Unit Tests Only
```bash
mvn clean test -Dtest="**/controller/**Test"
```

### Running Integration Tests Only
```bash
mvn test -Dtest="**/config/**Test,**/service/**Test,**/*ApplicationTests"
```

### Test Configuration
- Tests use **H2 in-memory database** (`jdbc:h2:mem:testdb`)
- Email functionality uses dummy configuration (no actual emails sent)
- Test configuration is in `src/test/resources/application.properties`
- Database schema is created automatically (`spring.jpa.hibernate.ddl-auto=create-drop`)

### Test Coverage
- **Controller tests**: User registration, validation, API endpoints
- **Service tests**: Token cleanup, business logic
- **Configuration tests**: Swagger setup, security configuration
- **Integration tests**: Full application context loading

## API Documentation

The API is documented using **Swagger/OpenAPI 3.0**.

### Accessing API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Spec (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Available on Production

The API documentation is also available on the deployed instance:
- Production Swagger UI: [https://user-management-api-java.up.railway.app/swagger-ui.html](https://user-management-api-java.up.railway.app/swagger-ui.html)

### Using Swagger UI

1. Navigate to `/swagger-ui.html`
2. Browse available endpoints organized by controllers
3. Click on an endpoint to see details (parameters, request/response schemas)
4. Use "Try it out" to test endpoints directly from the browser
5. **Note**: Protected endpoints require authentication. Log in through the web interface first, then use Swagger with the same browser session.

### API Endpoints Overview

The REST API includes:
- `POST /api/register` - User registration
- `GET /api/users` - Get all users (admin only)
- `GET /api/users/{id}` - Get user by ID (admin only)
- `PUT /api/users/{id}` - Update user (admin only)
- `DELETE /api/users/{id}` - Delete user (admin only)

For complete endpoint documentation, schemas, and examples, refer to the Swagger UI.

## CI/CD and Test Reports

The project uses GitHub Actions for continuous integration. Every push and pull request triggers automated testing with detailed reporting.

### Accessing Test Reports

1. **From GitHub Actions:**
   - Navigate to the "Actions" tab in the repository
   - Click on any workflow run to see the execution details
   - Scroll down to the "Artifacts" section at the bottom of the page
   - Download either:
     - `unit-test-reports` - Contains unit test results
     - `integration-test-reports` - Contains integration test results

2. **Viewing HTML Reports:**
   - Extract the downloaded artifact ZIP file
   - Open `surefire-report.html` in your browser
   - The report shows:
     - Summary of all tests run
     - Individual test results (pass/fail)
     - Execution time for each test
     - Stack traces for failed tests
     - Success rate and statistics

3. **Reading Workflow Logs:**
   - Click on any workflow run in the Actions tab
   - Expand the "Annotate Unit Test Failures" or "Annotate Integration Test Failures" steps
   - View detailed test summaries including:
     - Total tests executed
     - Number of tests passed
     - Number of tests failed
     - Number of tests with errors
     - Number of tests skipped
   - Individual test results are displayed with:
     - `[PASS]` - Test passed successfully
     - `[FAIL]` - Test failed with failure message
     - `[ERROR]` - Test encountered an error
     - `[SKIP]` - Test was skipped
     - Execution time for each test
   - Failed tests are also annotated in the workflow summary with `::error` markers

### Test Categories

The CI workflow separates tests into two categories:

- **Unit Tests:** Controller layer tests (`**/controller/**Test`)
  - Fast execution
  - Focus on individual component behavior
  - No external dependencies

- **Integration Tests:** Service, config, and application tests
  - Tests interaction between components
  - May use Spring context
  - Validates end-to-end functionality

### Running Tests Locally

Generate the same HTML reports locally:
```bash
# Run tests
mvn test

# Generate HTML report
mvn surefire-report:report

# View report at target/site/surefire-report.html
```

## Docker Health Checks

The application includes a Docker `HEALTHCHECK` instruction to monitor the container's health status in production environments. This is essential for container orchestration platforms like Docker Swarm, Kubernetes, and cloud services to automatically detect and handle unhealthy containers.

### How It Works

The Docker image includes a health check that periodically verifies the application is running and responsive:

- **Health Endpoint**: The application exposes `/actuator/health` via Spring Boot Actuator
- **Check Interval**: Health check runs every 30 seconds
- **Timeout**: Each health check has a 3-second timeout
- **Start Period**: 40 seconds grace period for application startup
- **Retries**: Container is marked unhealthy after 3 consecutive failures

### Health Check Configuration

The `HEALTHCHECK` instruction in the Dockerfile:

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

**Parameters:**
- `--interval=30s`: Time between running checks (after start period)
- `--timeout=3s`: Maximum time allowed for a single check
- `--start-period=40s`: Grace period before first health check
- `--retries=3`: Consecutive failures needed to mark container as unhealthy

### Health Endpoint Details

The `/actuator/health` endpoint returns:

**When Healthy (HTTP 200):**
```json
{
  "status": "UP"
}
```

**When Unhealthy (HTTP 503):**
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": { ... }
    }
  }
}
```

**Detailed Health Information:**
- Authenticated users (admins) can see detailed component status
- Public access shows only the overall status
- Components checked: database connectivity, disk space

### Verifying Health Check Status

#### Check Health Status Manually

When the application is running:
```bash
# Direct health check
curl http://localhost:8080/actuator/health

# Expected response
{"status":"UP"}
```

#### Check Docker Container Health

After running the container:
```bash
# View container health status
docker ps

# Detailed health check logs
docker inspect --format='{{json .State.Health}}' <container-id> | jq

# View last 5 health check results
docker inspect --format='{{range .State.Health.Log}}{{.Output}}{{end}}' <container-id>
```

**Container Health States:**
- `starting`: Container is starting, within start-period grace time
- `healthy`: Health checks are passing
- `unhealthy`: Health checks have failed (after retries)

### Testing the HEALTHCHECK

#### Local Testing with Docker

1. **Build the Docker image:**
   ```bash
   docker build -t user-management-api .
   ```

2. **Run the container with health checks:**
   ```bash
   docker run -d \
     --name user-mgmt-test \
     -p 8080:8080 \
     -e PGHOST=your-db-host \
     -e PGPORT=5432 \
     -e PGDATABASE=userdb \
     -e PGUSER=dbuser \
     -e PGPASSWORD=dbpass \
     -e RESEND_API_KEY=your-api-key \
     -e RESEND_FROM="User Management <onboarding@resend.dev>" \
     user-management-api
   ```

3. **Monitor health status:**
   ```bash
   # Watch health status change from 'starting' to 'healthy'
   watch -n 2 'docker ps --filter name=user-mgmt-test'
   
   # View detailed health check logs
   docker inspect user-mgmt-test --format='{{json .State.Health}}' | jq
   ```

4. **Simulate unhealthy state:**
   ```bash
   # Stop the database to trigger health check failure
   # Wait for ~90 seconds (3 retries × 30s interval)
   # Container status will change to 'unhealthy'
   
   docker ps --filter name=user-mgmt-test
   ```

### Troubleshooting Common Issues

#### Health Check Failing Immediately

**Symptom:** Container shows as `unhealthy` shortly after starting

**Possible Causes:**
1. **Database Connection Failed**
   - Verify database environment variables are correct
   - Check database is accessible from container network
   - Review logs: `docker logs <container-id>`

2. **Application Startup Taking Too Long**
   - Increase `--start-period` if application needs more time
   - Default is 40 seconds, may need 60s+ for slow systems

3. **Port Not Accessible**
   - Verify application is listening on port 8080
   - Check no firewall blocking internal container access

**Solution:**
```bash
# Check application logs
docker logs user-mgmt-test

# Check health endpoint directly
docker exec user-mgmt-test curl -f http://localhost:8080/actuator/health

# Verify curl is installed in container
docker exec user-mgmt-test which curl
```

#### Health Check Never Becomes Healthy

**Symptom:** Container stuck in `starting` state

**Possible Causes:**
1. **Application Not Starting**
   - Check logs for startup errors
   - Verify all environment variables are set correctly

2. **Health Endpoint Not Accessible**
   - Verify Spring Boot Actuator is enabled
   - Check security configuration allows `/actuator/health`

**Solution:**
```bash
# View detailed application logs
docker logs -f user-mgmt-test

# Check if application port is listening
docker exec user-mgmt-test netstat -tlnp | grep 8080

# Test health endpoint manually inside container
docker exec user-mgmt-test curl -v http://localhost:8080/actuator/health
```

#### curl Command Not Found

**Symptom:** Health check fails with "curl: not found"

**Possible Causes:**
- curl not installed in the container image

**Solution:**
This should not occur as the Dockerfile includes:
```dockerfile
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
```

If it does occur, rebuild the image:
```bash
docker build --no-cache -t user-management-api .
```

#### Health Check Performance Impact

**Symptom:** Frequent health checks causing performance issues

**Solution:**
Adjust health check intervals in the Dockerfile:
```dockerfile
HEALTHCHECK --interval=60s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### Integration with Container Orchestration

#### Docker Compose

```yaml
services:
  app:
    build: .
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      start_period: 40s
      retries: 3
```

#### Kubernetes

Use liveness and readiness probes:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 40
  periodSeconds: 30
  timeoutSeconds: 3
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 3
```

### Best Practices

1. **Adjust Parameters for Your Environment**
   - Increase `start-period` for slower systems or complex deployments
   - Reduce `interval` for faster failure detection in critical systems
   - Tune `retries` based on acceptable downtime tolerance

2. **Monitor Health Check Logs**
   - Regularly review health check results in production
   - Set up alerts for containers becoming unhealthy
   - Use `docker events` to track health status changes

3. **Consider Database Health**
   - The health check includes database connectivity
   - Ensure database is highly available in production
   - Consider read replicas for health checks

4. **Test Before Deploying**
   - Always test health checks in staging environment
   - Simulate failure scenarios (database down, high load)
   - Verify recovery behavior after failures

5. **Use with Restart Policies**
   ```bash
   docker run -d --restart=unless-stopped \
     --name user-mgmt \
     -p 8080:8080 \
     user-management-api
   ```
   Note: Docker does not automatically restart unhealthy containers. Use orchestration tools like Docker Swarm or Kubernetes for automatic restarts based on health status.
