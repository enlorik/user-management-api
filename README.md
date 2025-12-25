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

Running locally (summary):
- requires a PostgreSQL database
- requires Resend API key and sender address for email features
- build and run:
  - run `mvn clean package`
  - then run `java -jar target/user-management-api-*.jar`
- then open `http://localhost:8080` in the browser

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
   - View test summaries with emoji indicators:
     - 📊 Total tests executed
     - ✅ Number of tests passed
     - ❌ Number of tests failed
     - ⚠️ Number of tests with errors
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
