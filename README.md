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

Rate Limiting:
- Protects critical endpoints from abuse (brute force attacks, spam, etc.)
- Rate limits are enforced per IP address
- Endpoints with rate limiting:
  - `/login` and `/auth/login`: 5 requests per minute (strict - prevents brute force)
  - `/register`: 10 requests per 15 minutes (moderate - prevents spam registrations)
  - `/verify-email`: 20 requests per minute (lenient but protected)
- When rate limit is exceeded:
  - Returns HTTP 429 (Too Many Requests)
  - Includes `Retry-After` header with wait time in seconds
  - Includes `X-Rate-Limit-Retry-After-Seconds` header
  - Returns JSON error message with retry information
- All successful requests include `X-Rate-Limit-Remaining` header
- Rate limits are tracked separately for each IP address
- Uses Bucket4j library for efficient token bucket rate limiting

Running locally (summary):
- requires a PostgreSQL database
- requires Resend API key and sender address for email features
- build and run:
  - run `mvn clean package`
  - then run `java -jar target/user-management-api-*.jar`
- then open `http://localhost:8080` in the browser
