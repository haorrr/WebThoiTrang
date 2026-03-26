# Phase 03 — User Management (Admin)

## Context Links

- [plan.md](plan.md)
- [Research 01 — Architecture](research/researcher-01-report.md)
- Depends on: Phase 01 (BaseEntity, ApiResponse), Phase 02 (User entity, SecurityConfig, JWT)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | HIGH |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Admin CRUD for users (paginated list, search, status toggle, soft delete) + self-profile endpoints for authenticated users. |

---

## Key Insights

- Soft delete: set `deletedAt = now()` — never DELETE from DB. All queries must filter `WHERE deleted_at IS NULL` via `@Where(clause = "deleted_at IS NULL")` on the entity.
- Pagination via `Pageable` + Spring Data; `Page<UserResponse>` wrapped in `ApiResponse`.
- Search: `ILIKE %term%` on name + email via JPA Specification — avoids writing custom SQL.
- Admin sees all users; USER role can only access `/api/users/me` — enforced via `@PreAuthorize`.
- Avatar update goes through Cloudinary (Phase 4 upload endpoint); this phase only updates profile text fields.

---

## Requirements

### Functional
- `GET /api/users?page=&size=&search=&status=&sort=` (ADMIN) — paginated, filterable
- `GET /api/users/{id}` (ADMIN)
- `PUT /api/users/{id}` (ADMIN) — update name, role, status
- `DELETE /api/users/{id}` (ADMIN) — soft delete
- `PATCH /api/users/{id}/status` (ADMIN) — toggle ACTIVE/INACTIVE
- `GET /api/users/me` — own profile
- `PUT /api/users/me` — update own name, avatar

### Technical
- Default page size: 10, max: 50
- Sort by: `createdAt` (default desc), `name`, `email`
- `UserSpecification` for dynamic query building
- Response must never include password or refreshToken fields

---

## Architecture

### Package Structure
```
com.fashionshop/
├── dto/
│   ├── request/
│   │   ├── UpdateUserRequest.java      (ADMIN update)
│   │   └── UpdateProfileRequest.java   (self update)
│   └── response/
│       ├── UserResponse.java            (full, for ADMIN)
│       └── UserProfileResponse.java     (public-safe, for /me)
├── service/
│   └── UserService.java
├── controller/
│   └── UserController.java
└── repository/
    └── UserSpecification.java
```

### UserResponse Fields
```
id, email, name, avatar, role, status, provider, createdAt, updatedAt
(NO password, NO refreshToken)
```

### Query Filter Spec
```
search → WHERE (name ILIKE %q% OR email ILIKE %q%)
status → WHERE status = 'ACTIVE'|'INACTIVE'
sort   → ORDER BY createdAt DESC (default)
```

---

## Related Code Files

### Create
- `src/main/java/com/fashionshop/dto/request/UpdateUserRequest.java`
- `src/main/java/com/fashionshop/dto/request/UpdateProfileRequest.java`
- `src/main/java/com/fashionshop/dto/response/UserResponse.java`
- `src/main/java/com/fashionshop/dto/response/UserProfileResponse.java`
- `src/main/java/com/fashionshop/service/UserService.java`
- `src/main/java/com/fashionshop/controller/UserController.java`
- `src/main/java/com/fashionshop/repository/UserSpecification.java`

---

## Implementation Steps

1. **Add `@Where` annotation to `User` entity** to auto-filter soft-deleted records:
   ```java
   @Where(clause = "deleted_at IS NULL")
   @Entity @Table(name = "users")
   public class User extends BaseEntity { ... }
   ```

2. **Create `UserResponse` DTO** — map from `User` entity excluding sensitive fields:
   ```java
   @Data @Builder
   public class UserResponse {
     private Long id;
     private String email, name, avatar;
     private String role, status, provider;
     private LocalDateTime createdAt, updatedAt;
   }
   ```

3. **Create `UserProfileResponse`** — same as `UserResponse` but suitable for `/me` (can be same class, or omit `role` for USER).

4. **Create `UpdateUserRequest`** (ADMIN):
   ```java
   public class UpdateUserRequest {
     private String name;
     @Pattern(regexp = "ADMIN|USER")
     private String role;
     private String avatar;
   }
   ```

5. **Create `UpdateProfileRequest`** (self):
   ```java
   public class UpdateProfileRequest {
     @Size(min = 2, max = 100)
     private String name;
     private String avatar; // URL from Cloudinary upload
   }
   ```

6. **Create `UserSpecification`** implements `Specification<User>`:
   ```java
   public static Specification<User> withFilters(String search, String status) {
     return (root, query, cb) -> {
       List<Predicate> predicates = new ArrayList<>();
       if (search != null && !search.isBlank()) {
         String like = "%" + search.toLowerCase() + "%";
         predicates.add(cb.or(
           cb.like(cb.lower(root.get("name")), like),
           cb.like(cb.lower(root.get("email")), like)
         ));
       }
       if (status != null) predicates.add(cb.equal(root.get("status"), status));
       return cb.and(predicates.toArray(new Predicate[0]));
     };
   }
   ```

7. **Add to `UserRepository`**:
   ```java
   public interface UserRepository extends JpaRepository<User, Long>,
       JpaSpecificationExecutor<User> {
     // existing methods from Phase 2 +
     // findAll(Specification, Pageable) inherited from JpaSpecificationExecutor
   }
   ```

8. **Create `UserService`**:
   - `getUsers(String search, String status, String sort, int page, int size)` → `Page<UserResponse>`
     - Build `Specification` + `PageRequest` with sort
     - Map `Page<User>` → `Page<UserResponse>`
   - `getUserById(Long id)` → `UserResponse` or throw `ResourceNotFoundException`
   - `updateUser(Long id, UpdateUserRequest req)` → update allowed fields → save → return `UserResponse`
   - `deleteUser(Long id)` → find user → set `deletedAt = LocalDateTime.now()` → save (soft delete)
   - `toggleStatus(Long id)` → flip ACTIVE↔INACTIVE → save → return `UserResponse`
   - `getMyProfile(String email)` → find user by email → map to `UserProfileResponse`
   - `updateMyProfile(String email, UpdateProfileRequest req)` → update name/avatar → save

9. **Create `UserController`**:
   ```java
   @RestController
   @RequestMapping("/api/users")
   @RequiredArgsConstructor
   public class UserController {

     @GetMapping
     @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
         @RequestParam(required = false) String search,
         @RequestParam(required = false) String status,
         @RequestParam(defaultValue = "createdAt") String sort,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "10") int size) { ... }

     @GetMapping("/me")
     public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
         Authentication auth) { ... }

     @PutMapping("/me")
     public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
         Authentication auth, @Valid @RequestBody UpdateProfileRequest req) { ... }

     @GetMapping("/{id}")
     @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) { ... }

     @PutMapping("/{id}")
     @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<ApiResponse<UserResponse>> updateUser(
         @PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) { ... }

     @DeleteMapping("/{id}")
     @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) { ... }

     @PatchMapping("/{id}/status")
     @PreAuthorize("hasRole('ADMIN')")
     public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(@PathVariable Long id) { ... }
   }
   ```

10. **Extract current user email from `Authentication`**:
    ```java
    String email = ((UserDetails) auth.getPrincipal()).getUsername();
    ```

11. **Test** all endpoints with Postman using both ADMIN and USER JWT tokens.

---

## Todo List

- [ ] Add `@Where(clause = "deleted_at IS NULL")` to `User` entity
- [ ] Create `UserResponse` DTO
- [ ] Create `UserProfileResponse` DTO
- [ ] Create `UpdateUserRequest` DTO
- [ ] Create `UpdateProfileRequest` DTO
- [ ] Create `UserSpecification` for dynamic filtering
- [ ] Extend `UserRepository` with `JpaSpecificationExecutor`
- [ ] Implement `UserService` (7 methods)
- [ ] Implement `UserController` (7 endpoints)
- [ ] Test ADMIN endpoints with ADMIN JWT
- [ ] Test USER can only access `/me` endpoints
- [ ] Test soft delete (user hidden from list but still in DB)

---

## Success Criteria

- `GET /api/users` with ADMIN token → paginated list, no passwords in response
- `GET /api/users` with USER token → 403 Forbidden
- `GET /api/users?search=john` → filters by name/email
- `DELETE /api/users/{id}` → 200; subsequent `GET /api/users/{id}` → 404
- `PATCH /api/users/{id}/status` → toggles ACTIVE↔INACTIVE
- `GET /api/users/me` → current user's own data

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Admin soft-deleting themselves | Low | Check `id != currentUserId` in `deleteUser()` |
| `Page<UserResponse>` serialization issue with Jackson | Low | Jackson handles Spring Data `Page` — or return `PageImpl` wrapper DTO |
| Sort field injection (SQL injection via sort param) | Medium | Whitelist allowed sort fields: `createdAt`, `name`, `email` |

---

## Security Considerations

- `@PreAuthorize("hasRole('ADMIN')")` on all admin endpoints
- Response DTOs never expose `password` or `refreshToken`
- Admin cannot change their own role via `/api/users/{id}` (guard in service)
- Soft-deleted users cannot log in (checked in `CustomUserDetailsService`)

---

## Next Steps

- Phase 4 (Category & Product) uses same Specification pattern; reuse `UserSpecification` as template
- Phase 2's `UserRepository` extended here with `JpaSpecificationExecutor` — no conflict
