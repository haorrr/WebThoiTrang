package com.fashionshop.service;

import com.fashionshop.dto.request.ProductRequest;
import com.fashionshop.dto.response.ProductResponse;
import com.fashionshop.dto.response.ProductSummaryResponse;
import com.fashionshop.entity.Category;
import com.fashionshop.entity.Product;
import com.fashionshop.entity.ProductImage;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.CategoryRepository;
import com.fashionshop.repository.ProductImageRepository;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.ProductSpecification;
import com.fashionshop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final FlashSaleService flashSaleService;

    private static final Set<String> ALLOWED_SORT = Set.of("price", "createdAt", "name", "stock");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getProducts(String search, Long categoryId,
                                                     BigDecimal minPrice, BigDecimal maxPrice,
                                                     String status, String sort, String dir,
                                                     int page, int size) {
        size = Math.min(size, 50);
        String sortField = ALLOWED_SORT.contains(sort) ? sort : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Map<Long, FlashSaleService.FlashInfo> flashMap = flashSaleService.getActiveFlashInfoMap();

        return productRepository
                .findAll(ProductSpecification.withFilters(search, categoryId, minPrice, maxPrice, status), pageable)
                .map(p -> {
                    ProductSummaryResponse dto = ProductSummaryResponse.from(p);
                    FlashSaleService.FlashInfo fi = flashMap.get(p.getId());
                    if (fi != null) {
                        dto.setFlashPrice(fi.flashPrice());
                        dto.setFlashEndsAt(fi.endsAt().format(ISO_FMT));
                    }
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return enrichWithFlash(ProductResponse.from(product), product.getId());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return enrichWithFlash(ProductResponse.from(product), product.getId());
    }

    private ProductResponse enrichWithFlash(ProductResponse dto, Long productId) {
        Map<Long, FlashSaleService.FlashInfo> flashMap = flashSaleService.getActiveFlashInfoMap();
        FlashSaleService.FlashInfo fi = flashMap.get(productId);
        if (fi != null) {
            dto.setFlashPrice(fi.flashPrice());
            dto.setFlashEndsAt(fi.endsAt().format(ISO_FMT));
        }
        return dto;
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        String slug = SlugUtil.toUniqueSlug(req.getName(), productRepository::existsBySlug);

        Product product = Product.builder()
                .name(req.getName())
                .slug(slug)
                .description(req.getDescription())
                .price(req.getPrice())
                .salePrice(req.getSalePrice())
                .stock(req.getStock())
                .status(Product.Status.ACTIVE)
                .build();

        if (req.getCategoryId() != null) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
            product.setCategory(cat);
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product product = findProduct(id);

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setSalePrice(req.getSalePrice());
        product.setStock(req.getStock());

        if (req.getCategoryId() != null) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
            product.setCategory(cat);
        } else {
            product.setCategory(null);
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        product.softDelete();
        productRepository.save(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse toggleStatus(Long id) {
        Product product = findProduct(id);
        product.setStatus(product.getStatus() == Product.Status.ACTIVE
                ? Product.Status.INACTIVE
                : Product.Status.ACTIVE);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse addImage(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = findProduct(productId);
        String url = cloudinaryService.upload(file, "fashion-shop/products");

        if (isPrimary) {
            productImageRepository.clearPrimaryForProduct(productId);
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(url)
                .isPrimary(isPrimary || product.getImages().isEmpty())
                .sortOrder(product.getImages().size())
                .build();
        productImageRepository.save(image);

        // Reload with images
        return getProductById(productId);
    }

    @Transactional
    public ProductResponse addImageByUrl(Long productId, String imageUrl, boolean isPrimary) {
        Product product = findProduct(productId);

        if (isPrimary) {
            productImageRepository.clearPrimaryForProduct(productId);
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .isPrimary(isPrimary || product.getImages().isEmpty())
                .sortOrder(product.getImages().size())
                .build();
        productImageRepository.save(image);

        return getProductById(productId);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));

        cloudinaryService.delete(image.getImageUrl());
        productImageRepository.delete(image);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse updateAiDescription(Long id, String aiDescription) {
        Product product = findProduct(id);
        product.setAiDescription(aiDescription);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
