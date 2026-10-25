package com.wlstore.catalog_service.repository;

import com.wlstore.catalog_service.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findByParentIsNull(Pageable pageable);
    Page<Category> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);

    /**
     * Ids da categoria dona do path e de toda a sua subárvore.
     * O "/" antes do % evita que /acessorios também case com /acessorios-promo.
     */
    @Query("select c.id from Category c where c.path = :path or c.path like concat(:path, '/%')")
    List<UUID> findIdsInSubtree(@Param("path") String path);
}
