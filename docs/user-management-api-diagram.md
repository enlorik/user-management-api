# User Management API Diagram

```mermaid
flowchart TD
    Client[Browser / API Client] --> RateLimit[RateLimitFilter\n(IP-based throttle)]
    RateLimit --> Security[SecurityFilterChain\nSession auth + RBAC]

    Security --> Pages[PageController\n/login /register /admin /user]
    Security --> Register[RegistrationController\nPOST /register]
    Security --> Verify[EmailVerificationController\nGET /verify-email]
    Security --> Forgot[ForgotPasswordController\nGET/POST /forgot-password]
    Security --> Reset[ResetPasswordController\nGET/POST /reset-password]
    Security --> Users[UserController\n/users + /users/me]
    Security --> Logs[LogController\n/api/v1/logs/*]

    Register --> UserService
    Register --> EmailVerificationService
    Register --> EmailService

    Verify --> EmailVerificationService
    Forgot --> PasswordResetService
    Forgot --> EmailService
    Reset --> PasswordResetService

    Users --> UserService
    Logs --> LogReaderService
    Logs --> LogSanitizerService
    Logs --> LogSummarizerService

    LogSummarizerService -.optional.-> OpenAI[OpenAI API]

    UserService --> UserRepo[(UserRepository)]
    EmailVerificationService --> EmailTokenRepo[(EmailVerificationTokenRepository)]
    EmailVerificationService --> UserRepo
    PasswordResetService --> PasswordTokenRepo[(PasswordResetTokenRepository)]
    PasswordResetService --> UserRepo

    UserRepo --> DB[(PostgreSQL / H2)]
    EmailTokenRepo --> DB
    PasswordTokenRepo --> DB

    TokenCleanup[TokenCleanupService\n@Scheduled cleanup] --> EmailTokenRepo
    TokenCleanup --> PasswordTokenRepo
```
