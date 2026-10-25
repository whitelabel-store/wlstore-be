package com.wlstore.catalog_service.service;

import com.wlstore.common.web.PageResponseDTO;
import com.wlstore.catalog_service.dto.ProductFilterDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    ProductResponseDTO create(ProductRequestDTO requestDTO);
    PageResponseDTO<ProductResponseDTO> findAll(ProductFilterDTO filter, Pageable pageable);
    ProductResponseDTO findById(UUID id);
    ProductResponseDTO update(UUID id, ProductRequestDTO requestDTO);
    void delete(UUID id);
}
