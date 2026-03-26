# Phase 01 — Project Setup & Core Infrastructure

## Context Links

- [plan.md](plan.md)
- [Research 01 — Architecture & Infrastructure](research/researcher-01-report.md)
- [Research 02 — Advanced Technologies](research/researcher-02-report.md)
- Dependencies: none (this is the foundation)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | HIGH |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Bootstrap Spring Boot 3.2 project, configure all dependencies, set up Flyway migrations for all 10 tables, define base entity, global exception handler, and API response wrapper. |

---

## Key Insights

- Use Spring Initializr with Java 21 + Maven; do NOT use Gradle (less familiar in J2EE academic context).
- Single `V1__init.sql` migration creates all tables at once — avoids partial state in free-tier Neon.tech which has connection limits.
- `BaseEntity` with `@MappedSuperclass` carrying `id`, `createdAt`, `updatedAt`, `deletedAt` eliminates duplication across all entities.
- `ApiResponse<T>` wrapper standardizes all JSON responses; frontend team depends on this contract.
- Multi-profile `application.yml` (dev/prod) is critical — Neon.tech SSL requires `?sslmode=require` in prod JDBC URL.

---

## Requirements

### Functional
- Spring Boot project builds and runs locally (`./mvnw spring-boot:run`)
- Flyway creates all 10 tables on first startup
- `/actuator/health` returns `{ status: "UP" }`
- All API responses use `ApiResponse<T>` format
- Global exception handler returns consistent error format

### Technical
- Java 21, Spring Boot 3.2.x
- Maven wrapper (`mvnw`) committed to repo
- Profiles: `dev` (H2 or local PG), `prod` (Neon.tech)
- CORS configured for local frontend + production domain
- `.env.example` documents all required env vars

---

## Architecture

### Package Structure
```
com.fashionshop/
├── config/
│   ├── CorsConfig.java
│   └── AppConfig.java
├── entity/
│   └── BaseEntity.java          (@MappedSuperclass)
├── dto/
│   └── ApiResponse.java         (generic wrapper)
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── UnauthorizedException.java
└── FashionShopApplication.java
```

### ApiResponse Shape
```json
{
  "success": true,
  "message": "OK",
  "data": { ... },
  "timestamp": "2026-03-26T15:34:00Z"
}
```

### Error Response Shape
```json
{
  "success": false,
  "message": "Resource not found",
  "errors": { "field": "error detail" },
  "timestamp": "2026-03-26T15:34:00Z"
}
```

### Patterns Used
- `@MappedSuperclass` for BaseEntity
- `@RestControllerAdvice` for GlobalExceptionHandler
- Spring profiles (`@Profile`) for env separation

---

## Related Code Files

### Create
- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `src/main/resources/db/migration/V1__init.sql`
- `src/main/java/com/fashionshop/FashionShopApplication.java`
- `src/main/java/com/fashionshop/entity/BaseEntity.java`
- `src/main/java/com/fashionshop/dto/ApiResponse.java`
- `src/main/java/com/fashionshop/exception/GlobalExceptionHandler.java`
- `src/main/java/com/fashionshop/exception/ResourceNotFoundException.java`
- `src/main/java/com/fashionshop/exception/BadRequestException.java`
- `src/main/java/com/fashionshop/exception/UnauthorizedException.java`
- `src/main/java/com/fashionshop/config/CorsConfig.java`
- `Dockerfile`
- `.env.example`
- `.gitignore`

---

## Implementation Steps

1. **Generate project via Spring Initializr**
   - Go to https://start.spring.io
   - Group: `com.fashionshop`, Artifact: `fashion-shop`
   - Java 21, Maven, Spring Boot 3.2.x
   - Dependencies: Spring Web, Spring Data JPA, Spring Security, Spring Boot Actuator, Validation, Lombok, PostgreSQL Driver, Flyway Migration

2. **Add remaining dependencies to `pom.xml`**
   ```xml
   <!-- JWT -->
   <dependency>
     <groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.3</version>
   </dependency>
   <dependency>
     <groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.3</version><scope>runtime</scope>
   </dependency>
   <dependency>
     <groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.3</version><scope>runtime</scope>
   </dependency>
   <!-- OAuth2 -->
   <dependency>
     <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-client</artifactId>
   </dependency>
   <!-- Redis -->
   <dependency>
     <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   <dependency>
     <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-cache</artifactId>
   </dependency>
   <!-- Mail -->
   <dependency>
     <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-mail</artifactId>
   </dependency>
   <!-- Cloudinary -->
   <dependency>
     <groupId>com.cloudinary</groupId><artifactId>cloudinary-http44</artifactId><version>1.38.0</version>
   </dependency>
   <!-- Gemini AI SDK -->
   <dependency>
     <groupId>com.google.ai.client.generativeai</groupId><artifactId>google-ai-java</artifactId><version>0.9.0</version>
   </dependency>
   <!-- Swagger -->
   <dependency>
     <groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.3.0</version>
   </dependency>
   <!-- MapStruct (optional, manual mapping also fine) -->
   <dependency>
     <groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId><version>1.5.5.Final</version>
   </dependency>
   ```

3. **Configure `application.yml`** (base profile)
   ```yaml
   spring:
     application:
       name: fashion-shop
     profiles:
       active: dev
     jpa:
       hibernate:
         ddl-auto: validate
       show-sql: false
       open-in-view: false
     flyway:
       enabled: true
       locations: classpath:db/migration

   server:
     port: 8080

   app:
     jwt:
       secret: ${JWT_SECRET}
       access-token-expiry: 900000    # 15 min
       refresh-token-expiry: 604800000 # 7 days
   ```

4. **Configure `application-dev.yml`**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/fashionshop
       username: postgres
       password: postgres
     jpa:
       show-sql: true
   ```

5. **Configure `application-prod.yml`**
   ```yaml
   spring:
     datasource:
       url: ${DATABASE_URL}   # Neon.tech JDBC URL with ?sslmode=require
       username: ${DATABASE_USERNAME}
       password: ${DATABASE_PASSWORD}
   ```

6. **Write `V1__init.sql`** — create all 10 tables in dependency order:
   - `users` → `categories` → `products` → `product_images`
   - `coupons` → `orders` → `order_items`
   - `reviews` → `cart_items` → `password_reset_tokens`
   - Use `VARCHAR` for enums (simpler than PG ENUM types for Flyway portability)
   - Add `deleted_at TIMESTAMP NULL` on: users, categories, products, orders, reviews, coupons
   - Add indexes: `users(email)`, `products(slug)`, `products(category_id)`, `orders(user_id)`

7. **Create `BaseEntity.java`**
   ```java
   @MappedSuperclass
   @EntityListeners(AuditingEntityListener.class)
   public abstract class BaseEntity {
     @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     @CreatedDate
     private LocalDateTime createdAt;
     @LastModifiedDate
     private LocalDateTime updatedAt;
     private LocalDateTime deletedAt;
   }
   ```
   Enable auditing: `@EnableJpaAuditing` on main app class.

8. **Create `ApiResponse.java`**
   ```java
   @Data @Builder
   public class ApiResponse<T> {
     private boolean success;
     private String message;
     private T data;
     private LocalDateTime timestamp;

     public static <T> ApiResponse<T> ok(T data) { ... }
     public static <T> ApiResponse<T> ok(String message, T data) { ... }
     public static <T> ApiResponse<T> error(String message) { ... }
   }
   ```

9. **Create custom exceptions**
   - `ResourceNotFoundException extends RuntimeException` → maps to 404
   - `BadRequestException extends RuntimeException` → maps to 400
   - `UnauthorizedException extends RuntimeException` → maps to 401

10. **Create `GlobalExceptionHandler.java`**
    - `@RestControllerAdvice`
    - Handle: `ResourceNotFoundException` → 404, `BadRequestException` → 400, `MethodArgumentNotValidException` → 400 with field errors map, `Exception` → 500

11. **Configure CORS** in `CorsConfig.java`
    - Allow origins: `http://localhost:3000`, `${APP_FRONTEND_URL}`
    - Allow all methods + headers
    - Allow credentials

12. **Create `.env.example`**
    ```
    DATABASE_URL=
    DATABASE_USERNAME=
    DATABASE_PASSWORD=
    JWT_SECRET=
    GOOGLE_CLIENT_ID=
    GOOGLE_CLIENT_SECRET=
    GITHUB_CLIENT_ID=
    GITHUB_CLIENT_SECRET=
    CLOUDINARY_CLOUD_NAME=
    CLOUDINARY_API_KEY=
    CLOUDINARY_API_SECRET=
    REDIS_HOST=
    REDIS_PORT=6379
    REDIS_PASSWORD=
    GEMINI_API_KEY=
    MAIL_USERNAME=
    MAIL_PASSWORD=
    APP_FRONTEND_URL=http://localhost:3000
    ```

13. **Create multi-stage `Dockerfile`**
    ```dockerfile
    FROM eclipse-temurin:21-jdk-alpine AS build
    WORKDIR /app
    COPY . .
    RUN ./mvnw package -DskipTests

    FROM eclipse-temurin:21-jre-alpine
    WORKDIR /app
    COPY --from=build /app/target/*.jar app.jar
    EXPOSE 8080
    ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
    ```

14. **Verify startup** — run `./mvnw spring-boot:run` and confirm:
    - Flyway migrations execute without error
    - `/actuator/health` returns `{"status":"UP"}`

---

## Todo List

- [ ] Generate project from Spring Initializr
- [ ] Add all dependencies to `pom.xml`
- [ ] Write `application.yml`, `application-dev.yml`, `application-prod.yml`
- [ ] Write `V1__init.sql` with all 10 tables + indexes
- [ ] Create `BaseEntity.java` with JPA auditing
- [ ] Create `ApiResponse.java` generic wrapper
- [ ] Create custom exception classes (3)
- [ ] Create `GlobalExceptionHandler.java`
- [ ] Create `CorsConfig.java`
- [ ] Create `.env.example` and `.gitignore`
- [ ] Create `Dockerfile` (multi-stage)
- [ ] Verify local startup + Flyway migration

---

## Success Criteria

- `./mvnw spring-boot:run` starts without errors
- All 10 tables exist in local PostgreSQL after startup
- `GET /actuator/health` → `200 { "status": "UP" }`
- A deliberate 404 (e.g., `GET /api/not-found`) returns `ApiResponse` error shape
- `docker build` completes successfully

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Neon.tech SSL connection failure | Medium | Add `?sslmode=require` to JDBC URL in prod profile |
| Flyway migration order issues (FK constraints) | Medium | Create tables in correct dependency order in V1 |
| MapStruct annotation processor not configured | Low | Add `maven-compiler-plugin` config for annotation processors |
| `jjwt` version conflict with Spring Boot managed deps | Low | Use `<dependencyManagement>` to pin version |

---

## Security Considerations

- `.env` file must be in `.gitignore` — never commit secrets
- JWT secret must be at least 256 bits (32 chars) in production
- `ddl-auto: validate` in production — Flyway owns schema, not Hibernate
- CORS: restrict `allowedOrigins` to known frontend URLs only

---

## Next Steps

- Phase 2 (Authentication) depends on this phase completing successfully
- Specifically needs: `BaseEntity`, `ApiResponse`, `GlobalExceptionHandler`, DB tables, security dependency on classpath
