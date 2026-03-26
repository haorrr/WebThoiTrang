package com.fashionshop.repository;

import com.fashionshop.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> withFilters(String search, Long categoryId,
                                                      BigDecimal minPrice, BigDecimal maxPrice,
                                                      String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Non-deleted
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (status != null && !status.isBlank()) {
                try {
                    Product.Status s = Product.Status.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), s));
                } catch (IllegalArgumentException ignored) {}
            } else {
                // Default: only ACTIVE for public
                predicates.add(cb.equal(root.get("status"), Product.Status.ACTIVE));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
