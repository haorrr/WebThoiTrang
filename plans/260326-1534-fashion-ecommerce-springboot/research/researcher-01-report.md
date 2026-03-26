# Research Report 01 — Spring Boot Architecture & Infrastructure

## 1. Spring Boot 3.x Project Structure (REST API E-Commerce)

```
src/main/java/com/fashionshop/
├── config/          # SecurityConfig, SwaggerConfig, RedisConfig, CorsConfig
├── controller/      # REST controllers
├── service/         # Business logic (interfaces + impl)
├── repository/      # JPA repositories
├── entity/          # JPA entities
├── dto/             # Request/Response DTOs
│   ├── request/
│   └── response/
├── mapper/          # MapStruct mappers
├── exception/       # GlobalExceptionHandler, custom exceptions
├── security/        # JWT filter, OAuth2 handlers, UserDetailsService
├── util/            # Helper classes
└── FashionShopApplication.java
```

**Key Maven parent:** `spring-boot-starter-parent:3.2.x`

---

## 2. Spring Security — JWT + OAuth2 (Spring Boot 3.x)

### Dependencies:
```xml
spring-boot-starter-security
spring-boot-starter-oauth2-client
io.jsonwebtoken:jjwt-api:0.12.3
io.jsonwebtoken:jjwt-impl:0.12.3
io.jsonwebtoken:jjwt-jackson:0.12.3
```

### JWT Flow:
1. POST /api/auth/login → validate credentials → return `accessToken` (15min) + `refreshToken` (7d)
2. Client sends `Authorization: Bearer <token>` on each request
3. `JwtAuthFilter` extends `OncePerRequestFilter` → validates token → sets `SecurityContextHolder`
4. POST /api/auth/refresh → validate refreshToken → return new accessToken

### OAuth2 Flow (Google + GitHub):
- `spring.security.oauth2.client.registration.google` config in `application.yml`
- `spring.security.oauth2.client.registration.github` config in `application.yml`
- `OAuth2AuthenticationSuccessHandler` → generate JWT after OAuth2 login
- `CustomOAuth2UserService` → fetch user info, upsert user in DB with `provider=GOOGLE/GITHUB`

### Roles: `ROLE_ADMIN`, `ROLE_USER` via `@PreAuthorize("hasRole('ADMIN')")`

---

## 3. Database Hosting — Free/Cheap Options

| Option | DB | Free Tier | Latency | Spring Boot Compat |
|--------|-----|-----------|---------|-------------------|
| **Supabase** | PostgreSQL | 500MB, 2 projects | Low | ✅ Excellent |
| **Neon** | PostgreSQL | 512MB, branching | Low | ✅ Excellent |
| **Railway** | PostgreSQL/MySQL | $5 credit/mo | Low | ✅ Excellent |
| **PlanetScale** | MySQL | Free tier ended 2024 | — | ⚠️ Paid only |
| **Aiven** | PostgreSQL | 1 free service | Medium | ✅ Good |

**Recommendation: Neon (PostgreSQL)** — generous free tier, serverless scaling, branching for dev/prod, native JDBC URL.
**Spring dependency:** `spring-boot-starter-data-jpa` + `org.postgresql:postgresql`

---

## 4. Flyway vs Liquibase

| | Flyway | Liquibase |
|---|---|---|
| Format | SQL scripts | SQL/XML/YAML/JSON |
| Learning curve | Low | Medium |
| Spring Boot support | Native auto-config | Native auto-config |
| **Recommendation** | ✅ **Flyway** for J2EE project | — |

**Flyway dependency:** `org.flywaydb:flyway-core`
**Migration files:** `src/main/resources/db/migration/V1__init.sql`, `V2__add_coupons.sql`

---

## 5. Spring Boot Deployment — Render.com / Railway.app

### Render.com (Recommended for Spring Boot):
- Free tier: 512MB RAM, spins down after 15min inactivity (cold start ~30s)
- Deploy via GitHub auto-deploy or Dockerfile
- `Dockerfile` approach recommended for Spring Boot 3.x + GraalVM native optional
- Set env vars: `DATABASE_URL`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, etc.
- Custom domain: free `.onrender.com` subdomain or attach custom domain

### Railway.app:
- $5 free credit/month (enough for dev/testing)
- One-click PostgreSQL + Spring Boot deploy
- No cold start (always-on within credit limit)

### Dockerfile for Spring Boot:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Domain:** Free subdomain from Render/Railway, or buy `.id`/`.site` (~$1-2/yr) on Namecheap.
