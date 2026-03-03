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
    Forgot --> UserService
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

## Arrow Validation

Every arrow in the diagram above is backed by direct source code evidence. The sections below trace each relationship to the relevant file and line(s).

### Infrastructure → Security Layer

| Arrow | Source Evidence |
|-------|----------------|
| `Client → RateLimitFilter` | `filter/RateLimitFilter.java` – `@Component @Order(1) public class RateLimitFilter implements Filter` registers the filter as the first interceptor in the servlet chain; its `doFilter()` method examines every incoming HTTP request and either rejects it (HTTP 429) or calls `chain.doFilter()` to pass it forward. |
| `RateLimitFilter → SecurityFilterChain` | `filter/RateLimitFilter.java` line 88 – `chain.doFilter(request, response)` forwards allowed requests to the next filter. `config/SecurityConfig.java` – `@Bean public SecurityFilterChain securityFilterChain(HttpSecurity http)` defines the Spring Security filter that authenticates sessions and enforces RBAC on every request that passes rate limiting. |

### SecurityFilterChain → Controllers

| Arrow | Source Evidence |
|-------|----------------|
| `Security → PageController` | `config/PageController.java` – `@GetMapping("/login")`, `@GetMapping("/register")`, `@GetMapping("/admin")`, `@GetMapping("/user")`. `config/SecurityConfig.java` – `/login` and `/register` are `.permitAll()`; `/admin/**` requires `ROLE_ADMIN`; `/user/**` requires `ROLE_USER` or `ROLE_ADMIN`. |
| `Security → RegistrationController` | `controller/RegistrationController.java` – `@PostMapping("/register")`. `config/SecurityConfig.java` – `.requestMatchers("/login", "/register").permitAll()` allows unauthenticated access to this endpoint. |
| `Security → EmailVerificationController` | `controller/EmailVerificationController.java` – `@GetMapping("/verify-email")`. `config/SecurityConfig.java` – `.requestMatchers("/verify-email", "/verify-email/**").permitAll()` allows unauthenticated access so users can follow the link from their email. |
| `Security → ForgotPasswordController` | `controller/ForgotPasswordController.java` – `@GetMapping("/forgot-password")` and `@PostMapping("/forgot-password")`. `config/SecurityConfig.java` – `.requestMatchers("/forgot-password", "/forgot-password/**", "/reset-password", "/reset-password/**").permitAll()`. |
| `Security → ResetPasswordController` | `controller/ResetPasswordController.java` – `@GetMapping("/reset-password")` and `@PostMapping("/reset-password")`. `config/SecurityConfig.java` – same `.permitAll()` matcher listed above. |
| `Security → UserController` | `controller/UserController.java` – `@RestController @RequestMapping("/users")`. `config/SecurityConfig.java` – `anyRequest().authenticated()` requires a valid session; individual methods additionally use `@PreAuthorize("hasRole('ADMIN')")` for admin-only operations. |
| `Security → LogController` | `controller/LogController.java` – `@RestController @RequestMapping("/api/v1/logs")`. `config/SecurityConfig.java` – `anyRequest().authenticated()` requires authentication; the `/summarize` endpoint adds `@PreAuthorize("hasRole('ADMIN')")`. |

### Controller → Service Dependencies

| Arrow | Source Evidence |
|-------|----------------|
| `RegistrationController → UserService` | `controller/RegistrationController.java` – `private final UserService userService` (constructor injection); calls `userService.usernameExists()`, `userService.emailExists()`, and `userService.create()`. |
| `RegistrationController → EmailVerificationService` | `controller/RegistrationController.java` – `private final EmailVerificationService emailVerificationService` (constructor injection); calls `emailVerificationService.createTokenForUser(created)` after a successful registration. |
| `RegistrationController → EmailService` | `controller/RegistrationController.java` – `private final EmailService emailService` (constructor injection); calls `emailService.sendVerificationEmail(created.getEmail(), verifyLink)`. |
| `EmailVerificationController → EmailVerificationService` | `controller/EmailVerificationController.java` – `private final EmailVerificationService emailVerificationService` (constructor injection); calls `emailVerificationService.verifyToken(token)`. |
| `ForgotPasswordController → UserService` | `controller/ForgotPasswordController.java` – `private final UserService userService` (constructor injection); calls `userService.findByUsernameAndEmail(trimmedUsername, trimmedEmail)` to confirm the account exists before issuing a reset token. |
| `ForgotPasswordController → PasswordResetService` | `controller/ForgotPasswordController.java` – `private final PasswordResetService passwordResetService` (constructor injection); calls `passwordResetService.createPasswordResetTokenForEmail(trimmedEmail)`. |
| `ForgotPasswordController → EmailService` | `controller/ForgotPasswordController.java` – `private final EmailService emailService` (constructor injection); calls `emailService.sendPasswordResetEmail(trimmedEmail, resetLink)`. |
| `ResetPasswordController → PasswordResetService` | `controller/ResetPasswordController.java` – `private final PasswordResetService passwordResetService` (constructor injection); calls `passwordResetService.validatePasswordResetToken(token)` on GET and `passwordResetService.resetPassword(token, password)` on POST. |
| `UserController → UserService` | `controller/UserController.java` – `private final UserService userService` (constructor injection); calls `userService.findAll()`, `userService.create()`, `userService.findById()`, `userService.updateWithPassword()`, `userService.existsById()`, `userService.deleteById()`, and `userService.findByUsername()`. |
| `LogController → LogReaderService` | `controller/LogController.java` – `private final LogReaderService logReaderService` (constructor injection); calls `logReaderService.readLogs(start, end)`. |
| `LogController → LogSanitizerService` | `controller/LogController.java` – `private final LogSanitizerService logSanitizerService` (constructor injection); calls `logSanitizerService.sanitizeLogs(logEntries)`. |
| `LogController → LogSummarizerService` | `controller/LogController.java` – `private final LogSummarizerService logSummarizerService` (constructor injection); calls `logSummarizerService.summarizeLogs(sanitizedLogs, start, end)`. |

### Service → Repository Dependencies

| Arrow | Source Evidence |
|-------|----------------|
| `UserService → UserRepository` | `service/UserService.java` – `private final UserRepository userRepo` (constructor injection); calls `userRepo.save()`, `userRepo.findAll(Sort)`, `userRepo.findById()`, `userRepo.findByUsername()`, `userRepo.findByUsernameAndEmail()`, `userRepo.existsById()`, `userRepo.existsByEmail()`, `userRepo.existsByUsername()`, and `userRepo.deleteById()`. |
| `EmailVerificationService → EmailVerificationTokenRepository` | `service/EmailVerificationService.java` – `private final EmailVerificationTokenRepository tokenRepo` (constructor injection); calls `tokenRepo.findByUser()`, `tokenRepo.save()`, and `tokenRepo.findByToken()`. |
| `EmailVerificationService → UserRepository` | `service/EmailVerificationService.java` – `private final UserRepository userRepo` (constructor injection); calls `userRepo.save(user)` to persist `user.setVerified(true)` after successful token validation. |
| `PasswordResetService → PasswordResetTokenRepository` | `service/PasswordResetService.java` – `private final PasswordResetTokenRepository tokenRepo` (constructor injection); calls `tokenRepo.findByUser()`, `tokenRepo.save()`, and `tokenRepo.findByToken()`. |
| `PasswordResetService → UserRepository` | `service/PasswordResetService.java` – `private final UserRepository userRepo` (constructor injection); calls `userRepo.findByEmail()` to locate the account and `userRepo.save(user)` to persist the new encoded password. |

### Repository → Database

| Arrow | Source Evidence |
|-------|----------------|
| `UserRepository → DB` | `repository/UserRepository.java` – `public interface UserRepository extends JpaRepository<User, Long>`. Spring Data JPA generates SQL against PostgreSQL in production (configured via `PGHOST`/`PGDATABASE` environment variables) or H2 in local dev (`spring.profiles.active=local`). |
| `EmailVerificationTokenRepository → DB` | `repository/EmailVerificationTokenRepository.java` – `public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long>`. The `@Entity` annotation on `entity/EmailVerificationToken.java` maps it to a database table managed by JPA. |
| `PasswordResetTokenRepository → DB` | `repository/PasswordResetTokenRepository.java` – `public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>`. The `@Entity` annotation on `entity/PasswordResetToken.java` maps it to a database table managed by JPA. |

### Scheduled Service → Repositories

| Arrow | Source Evidence |
|-------|----------------|
| `TokenCleanupService → EmailVerificationTokenRepository` | `service/TokenCleanupService.java` – `private final EmailVerificationTokenRepository emailVerificationTokenRepository` (constructor injection); the `@Scheduled(cron = "0 0 2 * * ?")` method `cleanupExpiredTokens()` calls `emailVerificationTokenRepository.deleteByExpiryDateBefore(now)`. |
| `TokenCleanupService → PasswordResetTokenRepository` | `service/TokenCleanupService.java` – `private final PasswordResetTokenRepository passwordResetTokenRepository` (constructor injection); the same `@Scheduled` method calls `passwordResetTokenRepository.deleteByExpiryDateBefore(now)`. |

### Optional External Integration

| Arrow | Source Evidence |
|-------|----------------|
| `LogSummarizerService -.optional.-> OpenAI API` | `service/LogSummarizerService.java` – `@Autowired(required = false) OpenAIClient openAIClient` in the constructor makes the dependency optional; an explicit `if (openAIClient != null)` guard before the API call ensures the service falls back to rule-based summarization when no OpenAI API key is configured. |
