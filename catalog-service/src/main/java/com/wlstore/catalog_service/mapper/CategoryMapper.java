package com.wlstore.catalog_service.mapper;

import com.wlstore.catalog_service.dto.CategoryResponseDTO;
import com.wlstore.catalog_service.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponseDTO toResponseDTO(Category category);
}
