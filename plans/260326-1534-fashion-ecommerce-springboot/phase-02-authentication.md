# Phase 02 — Authentication (JWT + OAuth2 + Email Reset)

## Context Links

- [plan.md](plan.md)
- [Research 01 — JWT & OAuth2 Flow](research/researcher-01-report.md)
- [Research 02 — Spring Mail Flow](research/researcher-02-report.md)
- Depends on: Phase 01 (BaseEntity, DB tables, GlobalExceptionHandler)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | HIGH |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Full auth system: local JWT register/login/refresh/logout, forgot/reset password via email token, OAuth2 social login (Google + GitHub) with auto user creation. |

---

## Key Insights

- Store `refreshToken` as hashed value in `users` table (add column via V2 migration) — not in a separate table to keep it simple (KISS).
- Access token = 15 min, refresh token = 7 days; refresh token rotation on each use.
- OAuth2 success handler generates JWT and redirects to `{frontendUrl}/oauth2/callback?token=...` — frontend stores it.
- Password reset token is UUID stored in `password_reset_tokens` table; expires in 15 min; single-use (`used=true` after consume).
- `CustomUserDetailsService` loads user by email; `UserDetails` wraps `User` entity — avoids a separate User DTO for Spring Security.

---

## Requirements

### Functional
- `POST /api/auth/register` — create local user, return tokens
- `POST /api/auth/login` — validate credentials, return tokens
- `POST /api/auth/refresh` — rotate refresh token, return new access token
- `POST /api/auth/logout` — invalidate refresh token (set null in DB)
- `POST /api/auth/forgot-password` — send reset email
- `GET /api/auth/verify-reset-token?token=` — check token validity
- `POST /api/auth/reset-password` — update password, consume token
- OAuth2 login via Google and GitHub redirect flow

### Technical
- `JwtAuthFilter` runs before every request, stateless
- Security config uses `SecurityFilterChain` bean (not `WebSecurityConfigurerAdapter`)
- BCrypt password encoding
- `@EnableMethodSecurity` for `@PreAuthorize` on controllers
- Email sent async (`@Async`) to not block HTTP response

---

## Architecture

### Package Structure
```
com.fashionshop/
├── entity/
│   ├── User.java
│   └── PasswordResetToken.java
├── repository/
│   ├── UserRepository.java
│   └── PasswordResetTokenRepository.java
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   └── ResetPasswordRequest.java
│   └── response/
│       ├── AuthResponse.java      (accessToken, refreshToken, user)
│       └── UserSummaryResponse.java
├── service/
│   ├── AuthService.java
│   ├── JwtService.java
│   └── EmailService.java
├── controller/
│   └── AuthController.java
└── security/
    ├── JwtAuthFilter.java
    ├── CustomUserDetailsService.java
    ├── SecurityConfig.java
    ├── oauth2/
    │   ├── CustomOAuth2UserService.java
    │   └── OAuth2AuthenticationSuccessHandler.java
    └── oauth2/
        └── OAuth2AuthenticationFailureHandler.java
```

### Token Flow
```
Login → JwtService.generateAccessToken(user) → 15min JWT
      → JwtService.generateRefreshToken()     → UUID stored hashed in users.refresh_token
Refresh → validate refreshToken hash → issue new accessToken + rotate refreshToken
Logout  → set users.refresh_token = NULL
```

### OAuth2 Flow
```
GET /oauth2/authorization/google
  → Spring redirects to Google
  → Callback → CustomOAuth2UserService.loadUser()
    → find user by email OR provider+providerId
    → upsert user (provider=GOOGLE, providerId=googleId)
  → OAuth2AuthenticationSuccessHandler
    → generate JWT
    → redirect to {frontendUrl}/oauth2/callback?token={jwt}&refreshToken={rt}
```

---

## Related Code Files

### Create
- `src/main/java/com/fashionshop/entity/User.java`
- `src/main/java/com/fashionshop/entity/PasswordResetToken.java`
- `src/main/java/com/fashionshop/repository/UserRepository.java`
- `src/main/java/com/fashionshop/repository/PasswordResetTokenRepository.java`
- `src/main/java/com/fashionshop/dto/request/RegisterRequest.java`
- `src/main/java/com/fashionshop/dto/request/LoginRequest.java`
- `src/main/java/com/fashionshop/dto/request/RefreshTokenRequest.java`
- `src/main/java/com/fashionshop/dto/request/ForgotPasswordRequest.java`
- `src/main/java/com/fashionshop/dto/request/ResetPasswordRequest.java`
- `src/main/java/com/fashionshop/dto/response/AuthResponse.java`
- `src/main/java/com/fashionshop/dto/response/UserSummaryResponse.java`
- `src/main/java/com/fashionshop/service/AuthService.java`
- `src/main/java/com/fashionshop/service/JwtService.java`
- `src/main/java/com/fashionshop/service/EmailService.java`
- `src/main/java/com/fashionshop/controller/AuthController.java`
- `src/main/java/com/fashionshop/security/JwtAuthFilter.java`
- `src/main/java/com/fashionshop/security/CustomUserDetailsService.java`
- `src/main/java/com/fashionshop/security/SecurityConfig.java`
- `src/main/java/com/fashionshop/security/oauth2/CustomOAuth2UserService.java`
- `src/main/java/com/fashionshop/security/oauth2/OAuth2AuthenticationSuccessHandler.java`
- `src/main/java/com/fashionshop/security/oauth2/OAuth2AuthenticationFailureHandler.java`
- `src/main/resources/db/migration/V2__add_refresh_token.sql`
- `src/main/resources/templates/email/reset-password.html` (simple HTML template)

---

## Implementation Steps

1. **Add `refreshToken` column** via `V2__add_refresh_token.sql`:
   ```sql
   ALTER TABLE users ADD COLUMN refresh_token VARCHAR(500) NULL;
   ```

2. **Create `User` entity** extending `BaseEntity`:
   - Fields: `email`, `password` (nullable for OAuth), `name`, `avatar`, `role` (enum: ADMIN/USER), `status` (enum: ACTIVE/INACTIVE), `provider` (enum: LOCAL/GOOGLE/GITHUB), `providerId`, `refreshToken`
   - `@Column(unique = true)` on email
   - `@Enumerated(EnumType.STRING)` on all enums

3. **Create `PasswordResetToken` entity** extending `BaseEntity`:
   - Fields: `userId` (FK), `token` (UUID string), `expiresAt`, `used` (boolean, default false)
   - `@ManyToOne` to User

4. **Create repositories**:
   - `UserRepository`: `findByEmail`, `findByProviderAndProviderId`, `existsByEmail`
   - `PasswordResetTokenRepository`: `findByTokenAndUsedFalse`, `deleteByUserId`

5. **Create `JwtService`**:
   - `generateAccessToken(UserDetails user)` → signed JWT with sub=email, exp=15min, role claim
   - `generateRefreshToken()` → `UUID.randomUUID().toString()`
   - `extractEmail(String token)` → parse JWT
   - `isTokenValid(String token, UserDetails user)` → check expiry + email match
   - Use `Keys.hmacShaKeyFor(secret.getBytes())` for signing key

6. **Create `CustomUserDetailsService`** implements `UserDetailsService`:
   - `loadUserByUsername(email)` → find User or throw `UsernameNotFoundException`
   - Return `org.springframework.security.core.userdetails.User` with role as granted authority

7. **Create `JwtAuthFilter`** extends `OncePerRequestFilter`:
   - Extract `Authorization: Bearer {token}` header
   - Validate token via `JwtService`
   - Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
   - Skip if token absent or invalid (let downstream handle 401)

8. **Create `SecurityConfig`** (`@Configuration @EnableWebSecurity @EnableMethodSecurity`):
   - `csrf().disable()`
   - `sessionManagement().stateless()`
   - Public: `/api/auth/**`, `/oauth2/**`, `/api/products` (GET), `/api/categories` (GET), `/swagger-ui/**`, `/api-docs/**`
   - Authenticated: everything else
   - Register `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`
   - Wire OAuth2 login with `customOAuth2UserService` + success/failure handlers

9. **Create `AuthService`**:
   - `register(RegisterRequest)` → check email unique → hash password → save user → generate tokens → return `AuthResponse`
   - `login(LoginRequest)` → load user → verify password (BCrypt) → generate tokens → save refreshToken hash → return `AuthResponse`
   - `refresh(RefreshTokenRequest)` → find user by email → compare refreshToken hash → rotate → return new `AuthResponse`
   - `logout(String email)` → set `user.refreshToken = null` → save
   - `forgotPassword(String email)` → generate UUID token → save `PasswordResetToken` (expires +15min) → call `EmailService.sendResetEmail()`
   - `verifyResetToken(String token)` → return boolean
   - `resetPassword(ResetPasswordRequest)` → find valid token → update password hash → mark token used

10. **Create `EmailService`** (`@Async`):
    - `sendResetEmail(String to, String token)` → build HTML email with reset link → `JavaMailSender.send()`
    - Reset link format: `{frontendUrl}/reset-password?token={token}`
    - Configure `application.yml`:
      ```yaml
      spring:
        mail:
          host: smtp.gmail.com
          port: 587
          username: ${MAIL_USERNAME}
          password: ${MAIL_PASSWORD}
          properties:
            mail.smtp.auth: true
            mail.smtp.starttls.enable: true
      ```
    - Enable async: `@EnableAsync` on main class

11. **Create `CustomOAuth2UserService`** extends `DefaultOAuth2UserService`:
    - `loadUser(OAuth2UserRequest)` → extract email + name + avatar from OAuth2 attributes
    - GitHub note: email may be null → call GitHub `/user/emails` API if needed (or use login as fallback name)
    - Find user by `provider+providerId` OR by email → upsert → return `DefaultOAuth2User`

12. **Create `OAuth2AuthenticationSuccessHandler`**:
    - Generate accessToken + refreshToken
    - Save refreshToken to user
    - Redirect: `{frontendUrl}/oauth2/callback?accessToken={jwt}&refreshToken={rt}`

13. **Create `OAuth2AuthenticationFailureHandler`**:
    - Redirect: `{frontendUrl}/oauth2/callback?error={message}`

14. **Create `AuthController`**:
    - Map all 7 auth endpoints
    - Return `ResponseEntity<ApiResponse<AuthResponse>>`
    - Validate request bodies with `@Valid`

15. **Configure OAuth2 in `application.yml`**:
    ```yaml
    spring:
      security:
        oauth2:
          client:
            registration:
              google:
                client-id: ${GOOGLE_CLIENT_ID}
                client-secret: ${GOOGLE_CLIENT_SECRET}
                scope: email, profile
              github:
                client-id: ${GITHUB_CLIENT_ID}
                client-secret: ${GITHUB_CLIENT_SECRET}
                scope: user:email
    ```

16. **Test all auth flows** with Postman/curl before proceeding.

---

## Todo List

- [ ] Write `V2__add_refresh_token.sql`
- [ ] Create `User` entity with all fields + enums
- [ ] Create `PasswordResetToken` entity
- [ ] Create `UserRepository` + `PasswordResetTokenRepository`
- [ ] Create all request/response DTOs for auth
- [ ] Implement `JwtService` (generate + validate)
- [ ] Implement `CustomUserDetailsService`
- [ ] Implement `JwtAuthFilter`
- [ ] Implement `SecurityConfig` (full chain)
- [ ] Implement `AuthService` (all 7 methods)
- [ ] Implement `EmailService` (async reset email)
- [ ] Implement `CustomOAuth2UserService`
- [ ] Implement `OAuth2AuthenticationSuccessHandler`
- [ ] Implement `OAuth2AuthenticationFailureHandler`
- [ ] Implement `AuthController`
- [ ] Configure OAuth2 + mail in `application.yml`
- [ ] Test register/login/refresh/logout via Postman
- [ ] Test forgot-password email flow
- [ ] Test OAuth2 Google redirect (requires deployed URL or ngrok)

---

## Success Criteria

- `POST /api/auth/register` → 201 with `AuthResponse` containing both tokens
- `POST /api/auth/login` with wrong password → 401 with error message
- `POST /api/auth/refresh` with valid refreshToken → 200 with new accessToken
- `POST /api/auth/logout` → 200; subsequent refresh with old token → 401
- `POST /api/auth/forgot-password` → email received with valid reset link
- `POST /api/auth/reset-password` → password updated; old password no longer works
- OAuth2 Google redirect → JWT returned to frontend callback URL
- Protected endpoint without token → 401 from filter (not 403)

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| GitHub OAuth2 returns null email | High | Fetch `/user/emails` API or use `login` field as display name |
| Gmail SMTP blocked (Google security) | Medium | Use App Password (not account password); enable 2FA first |
| OAuth2 redirect_uri mismatch | High | Register exact callback URL in Google/GitHub console: `{serverUrl}/login/oauth2/code/google` |
| JWT secret too short → weak signature | Low | Enforce 32+ char secret; throw on startup if too short |
| Refresh token race condition (concurrent requests) | Low | Acceptable for academic project; note in docs |

---

## Security Considerations

- Passwords hashed with BCrypt (strength 10) — never stored plain
- Refresh tokens stored as BCrypt hash in DB — stolen DB dump can't reuse them
- Password reset tokens expire in 15 min and are single-use
- `deletedAt != null` users are rejected at `CustomUserDetailsService` (soft-deleted = banned)
- OAuth2 state parameter handled by Spring Security (CSRF protection for OAuth2)
- `Authorization: Bearer` header only — no cookie-based auth (simpler, no CSRF needed)

---

## Next Steps

- Phase 3 (User Management) depends on: `User` entity, `UserRepository`, `SecurityConfig`, `@PreAuthorize` working
- Phase 4+ all depend on JWT auth being functional
