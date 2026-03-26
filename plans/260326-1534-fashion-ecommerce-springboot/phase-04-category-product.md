# Phase 04 — Category & Product CRUD

## Context Links

- [plan.md](plan.md)
- [Research 02 — Cloudinary Integration](research/researcher-02-report.md)
- Depends on: Phase 01 (BaseEntity, ApiResponse, Flyway), Phase 02 (SecurityConfig), Phase 03 (Specification pattern)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | HIGH |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Categories (hierarchical tree) and Products (with images, variants, search/filter/sort) CRUD. Cloudinary image upload. Soft delete and status toggle for both. |

---

## Key Insights

- Categories are self-referencing (`parentId`); the tree is built in-memory in `CategoryService` — no recursive SQL (keeps queries simple).
- Products use `JPA Specification` for multi-filter search (same pattern as Phase 3), avoiding N+1 with `@EntityGraph`.
- `slug` for products and categories must be unique — generate from name on create, do NOT allow manual override in create requests (auto-generate in service).
- Cloudinary upload is a standalone `POST /api/upload/image` endpoint used by both product images and avatar updates — not coupled to product create.
- `product_images` has `isPrimary` flag; enforce only one primary per product in service logic, not DB constraint (simpler).

---

## Requirements

### Functional
- `GET /api/categories` — full tree (parent+children), public
- `GET/POST/PUT/DELETE /api/categories/{id}` — ADMIN
- `PATCH /api/categories/{id}/status` — ADMIN
- `GET /api/products` with filters: `search`, `categoryId`, `minPrice`, `maxPrice`, `status`, `sort`, `page`, `size`
- `GET /api/products/{id}` and `GET /api/products/slug/{slug}` — public
- `POST/PUT/DELETE /api/products/{id}` — ADMIN
- `PATCH /api/products/{id}/status` — ADMIN
- `POST /api/products/{id}/images` — ADMIN, upload image to Cloudinary
- `DELETE /api/products/{id}/images/{imageId}` — ADMIN, delete from Cloudinary + DB
- `POST /api/upload/image` — authenticated, upload any image to Cloudinary

### Technical
- Slug generation: lowercase, spaces→hyphens, remove special chars, append `-{timestamp}` if slug exists
- `@Where(clause = "deleted_at IS NULL")` on Category and Product entities
- Product listing excludes INACTIVE unless ADMIN requests with `status=INACTIVE`
- Image upload max size: 10MB (configure in `application.yml`)

---

## Architecture

### Package Structure
```
com.fashionshop/
├── entity/
│   ├── Category.java
│   ├── Product.java
│   └── ProductImage.java
├── repository/
│   ├── CategoryRepository.java
│   ├── ProductRepository.java
│   ├── ProductImageRepository.java
│   └── ProductSpecification.java
├── dto/
│   ├── request/
│   │   ├── CategoryRequest.java
│   │   └── ProductRequest.java
│   └── response/
│       ├── CategoryResponse.java      (flat)
│       ├── CategoryTreeResponse.java  (with children list)
│       ├── ProductResponse.java       (with images)
│       └── ProductSummaryResponse.java (for list)
├── service/
│   ├── CategoryService.java
│   ├── ProductService.java
│   └── CloudinaryService.java
└── controller/
    ├── CategoryController.java
    ├── ProductController.java
    └── UploadController.java
```

### Category Tree Build (in-memory)
```
1. Load all non-deleted categories (flat list)
2. Map by id
3. Assign children: for each cat with parentId, add to parent's children list
4. Return only top-level cats (parentId == null) — tree is populated
```

### Product Filter Spec
```
search     → name ILIKE %q% OR description ILIKE %q%
categoryId → categoryId = :id
minPrice   → price >= :min (uses salePrice if not null, else price)
maxPrice   → price <= :max
status     → status = :status (default ACTIVE for public; ADMIN can see all)
sort       → price ASC/DESC, createdAt DESC, name ASC
```

---

## Related Code Files

### Create
- `src/main/java/com/fashionshop/entity/Category.java`
- `src/main/java/com/fashionshop/entity/Product.java`
- `src/main/java/com/fashionshop/entity/ProductImage.java`
- `src/main/java/com/fashionshop/repository/CategoryRepository.java`
- `src/main/java/com/fashionshop/repository/ProductRepository.java`
- `src/main/java/com/fashionshop/repository/ProductImageRepository.java`
- `src/main/java/com/fashionshop/repository/ProductSpecification.java`
- `src/main/java/com/fashionshop/dto/request/CategoryRequest.java`
- `src/main/java/com/fashionshop/dto/request/ProductRequest.java`
- `src/main/java/com/fashionshop/dto/response/CategoryResponse.java`
- `src/main/java/com/fashionshop/dto/response/CategoryTreeResponse.java`
- `src/main/java/com/fashionshop/dto/response/ProductResponse.java`
- `src/main/java/com/fashionshop/dto/response/ProductSummaryResponse.java`
- `src/main/java/com/fashionshop/service/CategoryService.java`
- `src/main/java/com/fashionshop/service/ProductService.java`
- `src/main/java/com/fashionshop/service/CloudinaryService.java`
- `src/main/java/com/fashionshop/controller/CategoryController.java`
- `src/main/java/com/fashionshop/controller/ProductController.java`
- `src/main/java/com/fashionshop/controller/UploadController.java`
- `src/main/java/com/fashionshop/config/CloudinaryConfig.java`

---

## Implementation Steps

1. **Create `Category` entity** extending `BaseEntity`:
   ```java
   @Entity @Table(name = "categories")
   @Where(clause = "deleted_at IS NULL")
   public class Category extends BaseEntity {
     @NotBlank private String name;
     @Column(unique = true) private String slug;
     private String description, imageUrl;
     @Enumerated(EnumType.STRING) private Status status; // ACTIVE/INACTIVE
     @ManyToOne @JoinColumn(name = "parent_id")
     private Category parent;
     @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
     private List<Category> children = new ArrayList<>();
   }
   ```

2. **Create `Product` entity** extending `BaseEntity`:
   - Fields: `name`, `slug` (unique), `description`, `price` (BigDecimal), `salePrice` (nullable BigDecimal), `stock` (int), `status`, `aiDescription`
   - `@ManyToOne` to `Category`
   - `@OneToMany(mappedBy = "product", cascade = ALL, orphanRemoval = true)` to `ProductImage`
   - `@Where(clause = "deleted_at IS NULL")`

3. **Create `ProductImage` entity** (no BaseEntity needed — no audit needed):
   ```java
   @Entity @Table(name = "product_images")
   public class ProductImage {
     @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
     @ManyToOne @JoinColumn(name = "product_id") private Product product;
     private String imageUrl;
     private boolean isPrimary;
     private int sortOrder;
   }
   ```

4. **Configure Cloudinary** in `CloudinaryConfig.java`:
   ```java
   @Bean
   public Cloudinary cloudinary() {
     return new Cloudinary(ObjectUtils.asMap(
       "cloud_name", cloudName,
       "api_key", apiKey,
       "api_secret", apiSecret
     ));
   }
   ```
   Add to `application.yml`:
   ```yaml
   cloudinary:
     cloud-name: ${CLOUDINARY_CLOUD_NAME}
     api-key: ${CLOUDINARY_API_KEY}
     api-secret: ${CLOUDINARY_API_SECRET}
   spring:
     servlet:
       multipart:
         max-file-size: 10MB
         max-request-size: 10MB
   ```

5. **Create `CloudinaryService`**:
   ```java
   public String upload(MultipartFile file, String folder) {
     Map<?, ?> result = cloudinary.uploader().upload(
       file.getBytes(),
       ObjectUtils.asMap("folder", folder, "resource_type", "image")
     );
     return result.get("secure_url").toString();
   }

   public void delete(String publicId) {
     cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
   }
   ```
   - `folder`: `"fashion-shop/products"` or `"fashion-shop/avatars"`
   - Extract `publicId` from URL for delete: parse between `upload/` and file extension

6. **Create slug utility** in `SlugUtil`:
   ```java
   public static String toSlug(String input) {
     return input.toLowerCase()
       .replaceAll("[^a-z0-9\\s-]", "")
       .replaceAll("\\s+", "-")
       .replaceAll("-+", "-")
       .trim();
   }
   ```

7. **Create `CategoryService`**:
   - `getCategoryTree()` → load all → build tree in memory → return `List<CategoryTreeResponse>`
   - `getCategoryById(Long id)` → or throw 404
   - `createCategory(CategoryRequest)` → generate slug → save → return `CategoryResponse`
   - `updateCategory(Long id, CategoryRequest)` → find → update fields → save
   - `deleteCategory(Long id)` → soft delete (set `deletedAt`)
   - `toggleStatus(Long id)` → flip ACTIVE↔INACTIVE

8. **Create `ProductSpecification`** (same pattern as `UserSpecification` from Phase 3):
   - Predicates for: search, categoryId, minPrice, maxPrice, status

9. **Create `ProductService`**:
   - `getProducts(filters, Pageable)` → `Page<ProductSummaryResponse>`
   - `getProductById(Long id)` → `ProductResponse` (with images)
   - `getProductBySlug(String slug)` → `ProductResponse`
   - `createProduct(ProductRequest)` → generate unique slug → save → return `ProductResponse`
   - `updateProduct(Long id, ProductRequest)` → update fields → save
   - `deleteProduct(Long id)` → soft delete
   - `toggleStatus(Long id)` → flip status
   - `addImage(Long productId, MultipartFile file, boolean isPrimary)` → upload to Cloudinary → save `ProductImage`
   - `deleteImage(Long productId, Long imageId)` → delete from Cloudinary → remove `ProductImage`

10. **Create `ProductController`**:
    - All GET endpoints are public (no auth required for listing/detail)
    - POST/PUT/DELETE/PATCH require `@PreAuthorize("hasRole('ADMIN')")`
    - Image upload: `@PostMapping("/{id}/images")` accepts `@RequestParam("file") MultipartFile file`

11. **Create `UploadController`**:
    ```java
    @PostMapping("/api/upload/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
        @RequestParam("file") MultipartFile file) {
      String url = cloudinaryService.upload(file, "fashion-shop/misc");
      return ResponseEntity.ok(ApiResponse.ok("Uploaded", Map.of("url", url)));
    }
    ```

12. **Create `CategoryController`** and map all 6 endpoints.

13. **Test**: create category → create product in that category → upload image → list products with filter.

---

## Todo List

- [ ] Create `Category` entity with self-reference
- [ ] Create `Product` entity
- [ ] Create `ProductImage` entity
- [ ] Create `CategoryRepository`, `ProductRepository`, `ProductImageRepository`
- [ ] Create `ProductSpecification`
- [ ] Create `CloudinaryConfig`
- [ ] Implement `CloudinaryService` (upload + delete)
- [ ] Implement `SlugUtil`
- [ ] Implement `CategoryService` (6 methods)
- [ ] Implement `ProductService` (10 methods)
- [ ] Implement `CategoryController`
- [ ] Implement `ProductController`
- [ ] Implement `UploadController`
- [ ] Configure Cloudinary + multipart in `application.yml`
- [ ] Test category tree response (parent + children)
- [ ] Test product search + filter + pagination
- [ ] Test Cloudinary image upload

---

## Success Criteria

- `GET /api/categories` → tree structure with nested children
- `POST /api/products` (ADMIN) → 201 with slug auto-generated
- `GET /api/products?search=shirt&minPrice=100000` → filtered results
- `POST /api/products/{id}/images` → image URL stored, returned in `ProductResponse`
- `DELETE /api/products/{id}` → soft deleted; `GET /api/products/{id}` → 404
- `GET /api/products/{id}` → includes `images[]` array

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Cloudinary SDK `cloudinary-http44` compile error (old HTTP client) | Medium | Use `cloudinary-http5` if Java 21 conflict; or wrap in try/catch |
| Slug collision on concurrent create | Low | Append `System.currentTimeMillis()` suffix to slug on conflict |
| N+1 query on product → images | High | Use `@EntityGraph(attributePaths = {"images"})` on `findById` |
| Category delete while products reference it | Medium | Check `productRepository.existsByCategoryId(id)` before soft delete |
| Cloudinary `publicId` extraction fragile | Low | Store `publicId` in `ProductImage` entity separately from `imageUrl` |

---

## Security Considerations

- Image upload validates MIME type: only `image/jpeg`, `image/png`, `image/webp` accepted
- Cloudinary folder structure prevents overwriting other users' files
- Product and category public GETs do NOT require auth — expected for e-commerce frontend
- ADMIN-only write operations protected by `@PreAuthorize`

---

## Next Steps

- Phase 5 (Cart/Orders) requires `Product` entity with `price`, `stock`, `id`
- Phase 6 (AI) uses `Product.description` for Gemini prompts and writes to `Product.aiDescription`
- Phase 7 (Redis) caches `getProducts()` and `getCategoryTree()` — no code changes in this phase needed
