package com.fashionshop.service;

import com.fashionshop.dto.request.CategoryRequest;
import com.fashionshop.dto.response.CategoryResponse;
import com.fashionshop.dto.response.CategoryTreeResponse;
import com.fashionshop.entity.Category;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.CategoryRepository;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "categories", key = "'tree'")
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> all = categoryRepository.findAllActive();

        // Build id → response map
        Map<Long, CategoryTreeResponse> map = all.stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        c -> CategoryTreeResponse.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .slug(c.getSlug())
                                .description(c.getDescription())
                                .imageUrl(c.getImageUrl())
                                .status(c.getStatus().name())
                                .children(new java.util.ArrayList<>())
                                .build()
                ));

        // Assign children to parents
        List<CategoryTreeResponse> roots = new java.util.ArrayList<>();
        for (Category c : all) {
            CategoryTreeResponse node = map.get(c.getId());
            if (c.getParent() == null) {
                roots.add(node);
            } else {
                CategoryTreeResponse parent = map.get(c.getParent().getId());
                if (parent != null) parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return CategoryResponse.from(findCategory(id));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        String slug = SlugUtil.toUniqueSlug(req.getName(), categoryRepository::existsBySlug);

        Category category = Category.builder()
                .name(req.getName())
                .slug(slug)
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .status(Category.Status.ACTIVE)
                .build();

        if (req.getParentId() != null) {
            Category parent = findCategory(req.getParentId());
            category.setParent(parent);
        }

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest req) {
        Category category = findCategory(id);

        category.setName(req.getName());
        category.setDescription(req.getDescription());
        if (req.getImageUrl() != null) category.setImageUrl(req.getImageUrl());

        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category parent = findCategory(req.getParentId());
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category with existing products. Move products first.");
        }
        category.softDelete();
        categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse toggleStatus(Long id) {
        Category category = findCategory(id);
        Category.Status newStatus = category.getStatus() == Category.Status.ACTIVE
                ? Category.Status.INACTIVE
                : Category.Status.ACTIVE;
        category.setStatus(newStatus);
        categoryRepository.save(category);
        // Cascade status change to all products in this category
        productRepository.updateStatusByCategoryId(id,
                newStatus == Category.Status.ACTIVE
                        ? com.fashionshop.entity.Product.Status.ACTIVE
                        : com.fashionshop.entity.Product.Status.INACTIVE);
        return CategoryResponse.from(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
