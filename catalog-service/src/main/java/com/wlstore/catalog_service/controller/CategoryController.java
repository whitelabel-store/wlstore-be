package com.wlstore.catalog_service.controller;

import com.wlstore.common.web.PageResponseDTO;
import com.wlstore.catalog_service.dto.CategoryRequestDTO;
import com.wlstore.catalog_service.dto.CategoryResponseDTO;
import com.wlstore.catalog_service.service.impl.CategoryServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(
        name = "Categories",
        description = "Categories form a tree: each category can hold nested subcategories to any depth. "
                + "A create sends the whole tree at once; an update reconciles a subtree against what's already stored."
)
class CategoryController {
    private final CategoryServiceImpl categoryService;

    private static final String TREE_EXAMPLE = """
            {
              "title": "Acessorios",
              "children": [
                {
                  "title": "Acessorios para Celular",
                  "children": [
                    {
                      "title": "Capas e Cases",
                      "children": [
                        { "title": "Capas de Silicone", "children": [] },
                        { "title": "Capas de Couro", "children": [] }
                      ]
                    },
                    { "title": "Peliculas de Protecao", "children": [] }
                  ]
                }
              ]
            }
            """;

    private static final String UPDATE_EXAMPLE = """
            {
              "title": "Acessorios para Celular",
              "children": [
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "title": "Capas e Cases",
                  "children": [
                    { "id": "9c858901-8a57-4791-81fe-4c455b099bc9", "title": "Capas de Silicone", "children": [] }
                  ]
                },
                { "title": "Suportes Veiculares", "children": [] }
              ]
            }
            """;

    private static final String SLUG_EXAMPLE = """
            {
              "title": "Acessorios",
              "slug": "acessorios-promo",
              "children": []
            }
            """;

    @Operation(
            summary = "Create a category tree",
            description = "Creates a category and, recursively, every subcategory nested under it in the request body. "
                    + "Any `id` sent in the body is ignored — every node is always persisted as new."
    )
    @ApiResponse(responseCode = "201", description = "Tree created")
    @ApiResponse(responseCode = "400", description = "Validation error (e.g. missing title)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDTO create(
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Root category with nested subcategories", required = true,
                    content = @Content(schema = @Schema(implementation = CategoryRequestDTO.class),
                            examples = @ExampleObject(name = "Nested tree", value = TREE_EXAMPLE)))
            @org.springframework.web.bind.annotation.RequestBody CategoryRequestDTO categoryRequestDTO
    ) {
        return categoryService.create(categoryRequestDTO);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponseDTO<CategoryResponseDTO> findAll(
            @Parameter(description = "Partial, case-insensitive search across every level of the tree", example = "capas")
            @RequestParam(required = false) String title,
            @ParameterObject Pageable pageable) {
        return categoryService.findAll(title, pageable);
    }


    @Operation(
            summary = "Get a category by id",
            description = "Returns the category and its direct/nested subcategories. Works for a root category or any subcategory."
    )
    @ApiResponse(responseCode = "404", description = "Category not found")
    @GetMapping("/{id}")
    public CategoryResponseDTO findById(@PathVariable UUID id) {
        return categoryService.findById(id);
    }

    @Operation(
            summary = "Get a category by slug",
            description = "Storefront-friendly lookup for URLs like /c/capas-de-silicone, with no UUID involved. "
                    + "Returns the category and the subtree below it."
    )
    @ApiResponse(responseCode = "404", description = "No category with that slug")
    @GetMapping("/slug/{slug}")
    public CategoryResponseDTO findBySlug(
            @Parameter(description = "URL-friendly identifier", example = "capas-de-silicone")
            @PathVariable String slug
    ) {
        return categoryService.findBySlug(slug);
    }

    @Operation(
            summary = "Update a category and reconcile its subtree",
            description = """
                    Renames the category identified by the path `id` and replaces its subcategories using \
                    the `children` array in the body:

                    - A subcategory sent with an existing `id` is matched to the current record and updated in place \
                    (its own `id`, and everything below it, is reconciled the same way, recursively) — safe to use even \
                    when a `Product` already points at that subcategory's `categoryId`.
                    - A subcategory sent without an `id` (or with an unknown one) is created as new.
                    - Any subcategory currently stored under this category but missing from the request body is deleted. \
                    Deleting a subcategory that's still referenced by a `Product.categoryId` returns 409 Conflict — \
                    move or delete those products first.

                    The example below renames "Acessórios" to "Acessórios para Celular", keeps the existing \
                    "Capas e Cases" > "Capas de Silicone" branch (by id), drops "Películas de Proteção" (omitted), \
                    and adds a brand new "Suportes Veiculares" subcategory.

                    Renaming does **not** rewrite the slug — already-published URLs would break. Send an explicit \
                    `slug` (second example) when you actually want the URL to move.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Tree updated")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "409", description = "A removed subcategory is still referenced by a product")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryResponseDTO update(
            @PathVariable UUID id,
            @Valid @RequestBody(description = "Category title plus the reconciled subcategories tree", required = true,
                    content = @Content(schema = @Schema(implementation = CategoryRequestDTO.class),
                            examples = {
                                    @ExampleObject(name = "Rename + keep/add/remove subcategories", value = UPDATE_EXAMPLE),
                                    @ExampleObject(name = "Change the slug explicitly", value = SLUG_EXAMPLE)
                            }))
            @org.springframework.web.bind.annotation.RequestBody CategoryRequestDTO categoryRequestDTO
    ) {
        return categoryService.update(id, categoryRequestDTO);
    }

    @Operation(
            summary = "Delete a category",
            description = "Deletes the category and cascades to every subcategory beneath it. "
                    + "Fails with 409 Conflict if the category or any of its subcategories is still referenced by a Product."
    )
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "409", description = "Category (or a subcategory) is still referenced by a product")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }

}
