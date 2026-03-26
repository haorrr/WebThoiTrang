package com.fashionshop.repository;

import com.fashionshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.deletedAt IS NULL")
    List<Category> findAllRoots();

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL ORDER BY c.name ASC")
    List<Category> findAllActive();
}
