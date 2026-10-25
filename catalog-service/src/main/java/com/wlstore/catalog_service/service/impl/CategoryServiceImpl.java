package com.wlstore.catalog_service.service.impl;

import com.wlstore.common.web.PageResponseDTO;
import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.catalog_service.dto.CategoryRequestDTO;
import com.wlstore.catalog_service.dto.CategoryResponseDTO;
import com.wlstore.catalog_service.mapper.CategoryMapper;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-|-$)");
    private static final String FALLBACK_SLUG = "categoria";

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;


    @Override
    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO requestDTO) {
        Category category = buildTree(requestDTO, null, new HashSet<>());

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponseDTO(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<CategoryResponseDTO> findAll(String title, Pageable pageable) {
        Page<Category> categories = StringUtils.hasText(title)
                ? categoryRepository.findByTitleContainingIgnoreCase(title, pageable)
                : categoryRepository.findByParentIsNull(pageable);

        return PageResponseDTO.from(categories.map(categoryMapper::toResponseDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(UUID id) {
        Category category = categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        return categoryMapper.toResponseDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDTO findBySlug(String slug) {
        Category category = categoryRepository
                .findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toResponseDTO(category);
    }

    @Override
    @Transactional
    public CategoryResponseDTO update(UUID id, CategoryRequestDTO requestDTO) {
        Category category = categoryRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        Set<String> takenSlugs = new HashSet<>();

        category.setTitle(requestDTO.title());
        applyRequestedSlug(category, requestDTO, takenSlugs);
        reconcileChildren(category, requestDTO.children(), takenSlugs);
        category.refreshPath();

        Category updateCategory = categoryRepository.save(category);

        return categoryMapper.toResponseDTO(updateCategory);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }

        categoryRepository.deleteById(id);
    }

    private Category buildTree(CategoryRequestDTO requestDTO, Category parent, Set<String> takenSlugs) {
        Category category = Category.builder()
                .title(requestDTO.title())
                .slug(resolveSlug(requestDTO.slug(), requestDTO.title(), null, takenSlugs))
                .parent(parent)
                .build();

        category.refreshPath();

        category.getChildren().addAll(buildChildren(requestDTO.children(), category, takenSlugs));

        return category;
    }

    private List<Category> buildChildren(List<CategoryRequestDTO> subcategories, Category parent, Set<String> takenSlugs) {
        if (subcategories == null) {
            return new ArrayList<>();
        }

        return subcategories.stream()
                .map(subcategory -> buildTree(subcategory, parent, takenSlugs))
                .toList();
    }

    /**
     * Matches each item in {@code requests} against the parent's current subcategories by id:
     * existing ids are updated in place (recursively), items without a matching id are created,
     * and any current child whose id is absent from {@code requests} is dropped from the
     * collection (deleted via orphanRemoval on save).
     */
    private void reconcileChildren(Category parent, List<CategoryRequestDTO> requests, Set<String> takenSlugs) {
        List<CategoryRequestDTO> childRequests = requests == null ? List.of() : requests;

        Map<UUID, Category> existingById = parent.getChildren().stream()
                .filter(child -> child.getId() != null)
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Category> reconciled = new ArrayList<>();
        for (CategoryRequestDTO request : childRequests) {
            Category child = request.id() != null ? existingById.get(request.id()) : null;

            if (child == null) {
                child = buildTree(request, parent, takenSlugs);
            } else {
                child.setTitle(request.title());
                applyRequestedSlug(child, request, takenSlugs);
                reconcileChildren(child, request.children(), takenSlugs);
            }

            reconciled.add(child);
        }

        parent.getChildren().clear();
        parent.getChildren().addAll(reconciled);
    }

    /**
     * A rename on its own never rewrites the slug — published URLs would break. The slug only moves
     * when the request carries an explicit, different one.
     */
    private void applyRequestedSlug(Category category, CategoryRequestDTO requestDTO, Set<String> takenSlugs) {
        if (!StringUtils.hasText(requestDTO.slug())) {
            return;
        }

        String requested = toSlug(requestDTO.slug());
        if (requested.equals(category.getSlug())) {
            return;
        }

        category.setSlug(resolveSlug(requested, category.getTitle(), category.getId(), takenSlugs));
    }

    /**
     * @param excludedId the category being updated, so its own stored slug doesn't count as a collision
     * @param takenSlugs slugs already handed out earlier in this same request, which aren't persisted yet
     */
    private String resolveSlug(String requestedSlug, String title, UUID excludedId, Set<String> takenSlugs) {
        String base = toSlug(StringUtils.hasText(requestedSlug) ? requestedSlug : title);

        if (base.isEmpty()) {
            base = FALLBACK_SLUG;
        }

        String candidate = base;
        int suffix = 2;
        while (takenSlugs.contains(candidate) || isSlugTaken(candidate, excludedId)) {
            candidate = base + "-" + suffix++;
        }

        takenSlugs.add(candidate);

        return candidate;
    }

    private boolean isSlugTaken(String slug, UUID excludedId) {
        return excludedId == null
                ? categoryRepository.existsBySlug(slug)
                : categoryRepository.existsBySlugAndIdNot(slug, excludedId);
    }

    private static String toSlug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutAccents = DIACRITICS.matcher(normalized).replaceAll("");
        String dashed = NON_SLUG_CHARS.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll("-");

        return EDGE_DASHES.matcher(dashed).replaceAll("");
    }
}
