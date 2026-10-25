package com.wlstore.catalog_service.repository;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CategoryRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Test
    void rejectsTwoRootCategoriesWithSameTitle() {
        categoryRepository.saveAndFlush(root("Acessórios", "acessorios"));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(root("Acessórios", "acessorios-2")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_category_tenant_root_title");
    }

    private Category root(String title, String slug) {
        Category category = Category.builder()
                .title(title)
                .slug(slug)
                .build();
        category.refreshPath();
        return category;
    }
}