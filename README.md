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
version: '3.8'
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
