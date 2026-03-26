package com.fashionshop.repository;

import com.fashionshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"images", "category"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"images", "category"})
    Optional<Product> findBySlug(String slug);

    boolean existsByCategoryId(Long categoryId);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndStock(int stock);

    @Query("SELECT p.category.name, COUNT(p) FROM Product p WHERE p.deletedAt IS NULL GROUP BY p.category.name ORDER BY COUNT(p) DESC")
    List<Object[]> countByCategory();

    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.stock <= :threshold ORDER BY p.stock ASC")
    List<Product> findLowStock(@org.springframework.data.repository.query.Param("threshold") int threshold, Pageable pageable);
}
