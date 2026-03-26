# Phase 08 — Swagger, Testing & Deployment

## Context Links

- [plan.md](plan.md)
- [Research 01 — Render.com Deploy](research/researcher-01-report.md)
- [Research 02 — Springdoc OpenAPI](research/researcher-02-report.md)
- Depends on: All prior phases (all controllers must exist before Swagger annotations are added)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | HIGH |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Configure Springdoc OpenAPI/Swagger UI, write smoke tests for critical flows, finalize Docker multi-stage build, and deploy to Render.com with all environment variables. |

---

## Key Insights

- Springdoc auto-scans all `@RestController` classes — minimal manual annotation needed. Only add `@Tag`, `@Operation`, and `@SecurityRequirement` to controllers for better docs.
- Swagger UI must be accessible without auth in `SecurityConfig` (already listed in Phase 1 public paths).
- Render.com free tier spins down after 15 min inactivity — cold start ~30s. Document this in README. Use `/actuator/health` as the health check URL in `render.yaml`.
- Write integration tests only for the critical happy-path flows (register, login, create product, create order) — full unit test coverage is out of scope for academic project (YAGNI).
- `render.yaml` (Infrastructure as Code) makes the deploy reproducible and reviewable.
- The Docker image must NOT copy `.env` file — all secrets are injected by Render as environment variables.

---

## Requirements

### Functional
- Swagger UI at `/swagger-ui.html` — all endpoints documented with auth scheme
- All controllers annotated with `@Tag` and key endpoints with `@Operation`
- Bearer token auth testable directly from Swagger UI
- `GET /actuator/health` → `{"status":"UP"}` (health check for Render)
- Docker image builds and runs correctly locally
- App deploys to Render.com and all endpoints are reachable

### Technical
- Multi-stage Docker build (JDK for build, JRE for runtime) — final image ~200MB
- `render.yaml` defines web service + environment variable references
- `application-prod.yml` uses all env vars (no hardcoded values)
- `.dockerignore` excludes `.env`, `target/`, `.git/`
- README documents all env vars + API base URL + Swagger URL

---

## Architecture

### Files Modified/Created
```
project root:
├── Dockerfile              (finalize from Phase 1 skeleton)
├── .dockerignore
├── render.yaml
└── README.md (update)

src/main/java/com/fashionshop/config/
└── SwaggerConfig.java

src/main/java/com/fashionshop/controller/
└── (all controllers — add @Tag, @Operation annotations)

src/test/java/com/fashionshop/
├── AuthIntegrationTest.java
├── ProductIntegrationTest.java
└── OrderIntegrationTest.java
```

### Swagger Security Config
```java
@SecurityScheme(
  name = "bearerAuth",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "JWT"
)
@OpenAPIDefinition(
  info = @Info(title = "Fashion Shop API", version = "1.0", description = "J2EE Academic Project"),
  security = @SecurityRequirement(name = "bearerAuth")
)
```

---

## Related Code Files

### Create
- `src/main/java/com/fashionshop/config/SwaggerConfig.java`
- `render.yaml`
- `.dockerignore`
- `src/test/java/com/fashionshop/AuthIntegrationTest.java`
- `src/test/java/com/fashionshop/ProductIntegrationTest.java`
- `src/test/java/com/fashionshop/OrderIntegrationTest.java`

### Modify
- `Dockerfile` (finalize)
- `src/main/resources/application-prod.yml` (finalize all env vars)
- `src/main/java/com/fashionshop/controller/AuthController.java` — add `@Tag`
- `src/main/java/com/fashionshop/controller/ProductController.java` — add `@Tag`, `@Operation`
- `src/main/java/com/fashionshop/controller/OrderController.java` — add `@Tag`
- `src/main/java/com/fashionshop/controller/CategoryController.java` — add `@Tag`
- `src/main/java/com/fashionshop/controller/UserController.java` — add `@Tag`
- `src/main/java/com/fashionshop/controller/CartController.java` — add `@Tag`
- `src/main/java/com/fashionshop/controller/ReviewController.java` — add `@Tag`
- `src/main/java/com/fashionshop/controller/CouponController.java` — add `@Tag`
- `src/main/java/com/fashionshop/controller/AiController.java` — add `@Tag`
- `src/main/java/com/fashionshop/security/SecurityConfig.java` — ensure Swagger paths are public

---

## Implementation Steps

1. **Create `SwaggerConfig.java`**:
   ```java
   @Configuration
   @OpenAPIDefinition(
     info = @Info(
       title = "Fashion Shop API",
       version = "1.0.0",
       description = "Fashion E-Commerce REST API — J2EE Academic Project",
       contact = @Contact(name = "Dev Team")
     ),
     servers = {
       @Server(url = "http://localhost:8080", description = "Local"),
       @Server(url = "https://fashion-shop.onrender.com", description = "Production")
     }
   )
   @SecurityScheme(
     name = "bearerAuth",
     type = SecuritySchemeType.HTTP,
     scheme = "bearer",
     bearerFormat = "JWT"
   )
   public class SwaggerConfig {}
   ```

2. **Add Swagger config to `application.yml`**:
   ```yaml
   springdoc:
     api-docs:
       path: /api-docs
     swagger-ui:
       path: /swagger-ui.html
       tags-sorter: alpha
       operations-sorter: method
       try-it-out-enabled: true
   ```

3. **Annotate all controllers** with `@Tag(name = "Auth")`, `@Tag(name = "Products")`, etc.
   Add `@Operation(summary = "...")` to key endpoints (at minimum: login, register, create product, create order).
   Add `@SecurityRequirement(name = "bearerAuth")` to controllers requiring auth (can use class-level).

4. **Verify Swagger paths are public in `SecurityConfig`**:
   ```java
   .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/api-docs.yaml").permitAll()
   ```

5. **Write integration tests** using `@SpringBootTest` + `MockMvc` (or `TestRestTemplate`):

   `AuthIntegrationTest`:
   - `testRegister()` → POST /api/auth/register → 201, tokens returned
   - `testLogin()` → POST /api/auth/login → 200, tokens returned
   - `testLoginWrongPassword()` → 401

   `ProductIntegrationTest`:
   - `testGetProducts()` → GET /api/products → 200, page returned
   - `testCreateProductAsAdmin()` → POST /api/products with ADMIN token → 201
   - `testCreateProductAsUser()` → POST /api/products with USER token → 403

   `OrderIntegrationTest`:
   - `testCreateOrder()` → add cart items → POST /api/orders → 201, stock decremented

   Use `@ActiveProfiles("test")` with H2 in-memory DB to avoid needing real PostgreSQL in CI.

6. **Add test profile** `application-test.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:mem:testdb;MODE=PostgreSQL
       driver-class-name: org.h2.Driver
     jpa:
       hibernate:
         ddl-auto: create-drop
     flyway:
       enabled: false   # Use Hibernate DDL for tests (simpler)
     cache:
       type: simple     # Use in-memory cache for tests (no Redis needed)
   ```
   Add H2 dependency (test scope) to `pom.xml`:
   ```xml
   <dependency>
     <groupId>com.h2database</groupId>
     <artifactId>h2</artifactId>
     <scope>test</scope>
   </dependency>
   ```

7. **Finalize `Dockerfile`**:
   ```dockerfile
   # Stage 1: Build
   FROM eclipse-temurin:21-jdk-alpine AS build
   WORKDIR /app
   COPY .mvn/ .mvn
   COPY mvnw pom.xml ./
   RUN ./mvnw dependency:go-offline -q
   COPY src ./src
   RUN ./mvnw package -DskipTests -q

   # Stage 2: Runtime
   FROM eclipse-temurin:21-jre-alpine
   WORKDIR /app
   RUN addgroup -S spring && adduser -S spring -G spring
   USER spring:spring
   COPY --from=build /app/target/*.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-jar", "app.jar", \
     "--spring.profiles.active=prod"]
   ```
   Note: `dependency:go-offline` layer caches Maven deps — much faster rebuilds.

8. **Create `.dockerignore`**:
   ```
   .env
   .git
   .gitignore
   target/
   *.md
   .idea/
   *.iml
   ```

9. **Create `render.yaml`**:
   ```yaml
   services:
     - type: web
       name: fashion-shop-api
       runtime: docker
       dockerfilePath: ./Dockerfile
       plan: free
       healthCheckPath: /actuator/health
       envVars:
         - key: SPRING_PROFILES_ACTIVE
           value: prod
         - key: DATABASE_URL
           sync: false
         - key: DATABASE_USERNAME
           sync: false
         - key: DATABASE_PASSWORD
           sync: false
         - key: JWT_SECRET
           generateValue: true
         - key: GOOGLE_CLIENT_ID
           sync: false
         - key: GOOGLE_CLIENT_SECRET
           sync: false
         - key: GITHUB_CLIENT_ID
           sync: false
         - key: GITHUB_CLIENT_SECRET
           sync: false
         - key: CLOUDINARY_CLOUD_NAME
           sync: false
         - key: CLOUDINARY_API_KEY
           sync: false
         - key: CLOUDINARY_API_SECRET
           sync: false
         - key: REDIS_HOST
           sync: false
         - key: REDIS_PORT
           value: "6379"
         - key: REDIS_PASSWORD
           sync: false
         - key: REDIS_SSL
           value: "true"
         - key: GEMINI_API_KEY
           sync: false
         - key: MAIL_USERNAME
           sync: false
         - key: MAIL_PASSWORD
           sync: false
         - key: APP_FRONTEND_URL
           sync: false
   ```

10. **Deploy to Render.com**:
    - Push code to GitHub
    - Create new Web Service on Render → connect GitHub repo → Render detects `render.yaml` OR select Dockerfile manually
    - Set all `sync: false` environment variables in Render dashboard
    - First deploy will take ~5 min (Docker build + Flyway migrations)
    - Verify: `https://fashion-shop.onrender.com/actuator/health` → `{"status":"UP"}`
    - Verify: `https://fashion-shop.onrender.com/swagger-ui.html` → Swagger UI loads

11. **Register OAuth2 redirect URIs** in Google/GitHub consoles:
    - Google: `https://fashion-shop.onrender.com/login/oauth2/code/google`
    - GitHub: `https://fashion-shop.onrender.com/login/oauth2/code/github`

12. **Run integration tests locally**: `./mvnw test`

---

## Todo List

- [ ] Create `SwaggerConfig.java`
- [ ] Add Springdoc config to `application.yml`
- [ ] Add `@Tag` to all 9 controllers
- [ ] Add `@Operation` to key endpoints (min 2 per controller)
- [ ] Verify Swagger paths are public in `SecurityConfig`
- [ ] Add H2 test dependency + `application-test.yml`
- [ ] Write `AuthIntegrationTest` (3 tests)
- [ ] Write `ProductIntegrationTest` (3 tests)
- [ ] Write `OrderIntegrationTest` (1 test)
- [ ] Finalize `Dockerfile` (with dependency caching layer)
- [ ] Create `.dockerignore`
- [ ] Create `render.yaml`
- [ ] Finalize `application-prod.yml`
- [ ] Test `docker build` locally
- [ ] Test `docker run` locally with env vars
- [ ] Push to GitHub
- [ ] Create Render.com web service
- [ ] Set all env vars in Render dashboard
- [ ] Verify live deployment health check
- [ ] Verify Swagger UI on production URL
- [ ] Register prod OAuth2 redirect URIs in Google + GitHub consoles

---

## Success Criteria

- `GET /swagger-ui.html` → Swagger UI loads with all endpoints visible and grouped by tag
- Swagger "Authorize" button accepts JWT token → protected endpoints work from UI
- `./mvnw test` → all integration tests pass (using H2 in-memory)
- `docker build -t fashion-shop .` → succeeds locally
- `docker run -p 8080:8080 --env-file .env fashion-shop` → app starts, `/actuator/health` UP
- `https://{render-url}/actuator/health` → `{"status":"UP"}` within 5 min of first deploy
- `https://{render-url}/swagger-ui.html` → accessible without login
- Google/GitHub OAuth2 redirect works on production URL

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Render free tier cold start confuses health check | High | Set `healthCheckPath: /actuator/health`; Render retries — first check may fail |
| Docker build fails on Render due to Maven timeout | Medium | Use `dependency:go-offline` layer in Dockerfile to pre-cache deps |
| H2 `MODE=PostgreSQL` doesn't support all PG syntax in tests | Medium | Avoid PG-specific SQL in entity queries; use JPQL instead of native SQL in tests |
| OAuth2 redirect URI mismatch after deploy | High | Register EXACT Render URL in Google/GitHub console before testing OAuth2 |
| Render free tier only 512MB RAM; Spring Boot + JVM may OOM | Medium | Use `-XX:MaxRAMPercentage=75.0`; avoid loading large datasets in startup |
| Flyway migration fails on Neon.tech SSL in prod | Medium | Verify JDBC URL includes `?sslmode=require`; test connection before first deploy |

---

## Security Considerations

- `render.yaml` has `sync: false` for secrets — they must be manually entered in Render dashboard, never in source code
- `JWT_SECRET` uses `generateValue: true` in `render.yaml` — Render auto-generates a secure random value
- Swagger UI is public (no auth) — acceptable since it only shows API structure, not data
- Production Docker image runs as non-root user (`spring:spring`) — reduces attack surface
- CORS in prod must restrict to actual frontend domain, not `*`
- Actuator: only expose `health` endpoint in prod (disable `env`, `beans`, `metrics` actuator endpoints)

---

## Next Steps

- All phases complete — project is deployable
- Future improvements (not in scope): CI/CD pipeline (GitHub Actions), monitoring (Sentry), payment integration (VNPay), WebSocket for real-time order tracking
