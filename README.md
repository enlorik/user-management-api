# User Management API

A Spring Boot application for user registration, login, role-based access control, and account self-service.

Deployed on Railway:  
[https://user-management-api-java.up.railway.app/]

---

Tech stack

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL (Railway)
- Maven
- Docker
- Resend (for sending emails)
- Railway (hosting for app + database)

---

What the app can do

- User registration
  - stores users in PostgreSQL
  - passwords are hashed with BCrypt (not stored in plain text)
  - sends an email verification link after registration
- Login
  - form login at `/login`
  - users can only log in after verifying their email
  - redirects:
    - admin users → `/admin`
    - normal users → `/user`
- Email verification
  - generates a unique token per user
  - sends a link `/verify-email?token=...`
  - when the link is opened:
    - marks the token as used
    - marks the user as verified
- Password reset
  - `/forgot-password` form (username + email must match)
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

---

Security rules (high level)

- `/login`, `/register`, `/verify-email/**`, `/forgot-password/**`, `/reset-password/**` and `/css/**`, `/js/**` are public
- `/admin/**` requires `ROLE_ADMIN`
- `/user/**` requires `ROLE_USER` or `ROLE_ADMIN`
- all other endpoints require authentication
- unverified users are treated as disabled accounts and cannot log in

---

Running locally (summary)

You need:

- a PostgreSQL database
- Resend API key and sender address (for the email features)

Then:

```bash
mvn clean package
java -jar target/user-management-api-*.jar
