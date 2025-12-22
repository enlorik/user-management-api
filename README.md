# User Management API

A Spring Boot application that provides secure user registration, authentication, role-based access control, and self-service profile management.

Deployed on Railway:  
[user-management-api](https://user-management-api-java.up.railway.app/)

---

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA (Hibernate)
- PostgreSQL (Railway)
- Maven
- Docker
- Resend (transactional email)
- Railway (app + database hosting)

---

## Features

-authentication & security

- User registration with:
  - server-side validation
  - passwords hashed using BCrypt
  - email verification link sent via Resend
- Login via Spring Security form login (`/login`)
- Email verification:
  - unique token per user
  - verification endpoint `/verify-email?token=...`
  - users cannot log in until their email is verified
- Password reset flow:
  - `/forgot-password` form (username + email check)
  - reset token stored in the database
  - reset link via email
  - `/reset-password?token=...` page to set a new password

-user management

- Role-based access control with `ADMIN` and `USER`
- REST endpoints for managing users (admin only):
  - list users (sorted by `id`)
  - create / update / delete users
- Self-service endpoint for the current user:
  - update email (with uniqueness check)
  - update password (optional, with hashing)

-frontend pages (Thymeleaf)

- Login page
- Registration page
- Forgot password page
- Reset password page
- User dashboard (`/user`)
  - view and update own email/password
- Admin dashboard (`/admin`)
  - view all users (sorted by id)
  - create, edit and delete users through a simple UI backed by the REST API

---

## Security Overview

- Passwords are never stored in plain text (BCrypt hashing).
- Email verification is enforced in the authentication layer:
  - unverified users are treated as disabled accounts by Spring Security
- Role checks:
  - `/admin/**` is restricted to `ROLE_ADMIN`
  - `/user/**` is accessible to authenticated `ROLE_USER` or `ROLE_ADMIN`
- Public endpoints (no authentication required):
  - `/login`, `/register`
  - `/verify-email/**`
  - `/forgot-password/**`
  - `/reset-password/**`
  - static assets under `/css/**`, `/js/**`

---

## API Endpoints (high level)

-All user operations require authentication and appropriate role.

- `GET /users`
  - Admin only
  - Returns all users sorted by id ascending

- `POST /users`
  - Admin only
  - Creates a new user (username + email must be unique)

- `PUT /users/{id}`
  - Admin only
  - Updates user data (including role)

- `DELETE /users/{id}`
  - Admin only
  - Deletes a user

- `PUT /users/me`
  - Authenticated user
  - Updates own email and/or password
  - Email change is validated for uniqueness

---

## Running Locally (summary)

- Provide PostgreSQL connection settings via `application.properties` or environment variables.
- Configure Resend API key and sender (for email features).
- Build and run:

```bash
mvn clean package
java -jar target/user-management-api-*.jar
