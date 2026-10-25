package com.wlstore.catalog_service.service;

import com.wlstore.common.web.PageResponseDTO;
import com.wlstore.catalog_service.dto.CategoryRequestDTO;
import com.wlstore.catalog_service.dto.CategoryResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {
    CategoryResponseDTO create(CategoryRequestDTO requestDTO);
    PageResponseDTO<CategoryResponseDTO> findAll(String title, Pageable pageable);
    CategoryResponseDTO findById(UUID id);
    CategoryResponseDTO findBySlug(String slug);
    CategoryResponseDTO update(UUID id, CategoryRequestDTO requestDTO);
    void delete(UUID id);
}
