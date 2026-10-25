package com.wlstore.catalog_service.controller;

import com.wlstore.common.web.PageResponseDTO;
import com.wlstore.catalog_service.dto.ProductFilterDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductResponseDTO;
import com.wlstore.catalog_service.service.impl.ProductServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog. Every product belongs to exactly one category or subcategory via categoryId.")
class ProductController {
    private final ProductServiceImpl productService;

    private static final String PRODUCT_EXAMPLE = """
            {
              "title": "Redmi 18 Pro",
              "brand": "Xiaomi",
              "description": "Lorem Ipsum Dolor Sit",
              "status": "ACTIVE",
              "category": {
                "id": "46f1230c-7ea0-45eb-9a02-502d920cf8d9",
                "title": "Acessorios",
                "slug": "acessorios",
                "path": [
                  "Acessorios"
                ]
              },
              "variants": [
                {
                  "sku": "REDMI18-128-BLK",
                  "name": "Redmi 18 128GB Preto",
                  "optionValues": {
                    "color": "Preto",
                    "storage": "128GB"
                  },
                  "weightGrams": 200,
                  "status": "ACTIVE",
                  "position": 0
                },
                {
                  "sku": "REDMI18-256-WHT",
                  "name": "Redmi 18 256GB Branco",
                  "optionValues": {
                    "color": "Branco",
                    "storage": "256GB"
                  },
                  "weightGrams": 205,
                  "status": "ACTIVE",
                  "position": 1
                }
              ],
              "images": [
                {
                  "url": "https://example.com/redmi-18-front.png",
                  "altText": "Frente",
                  "position": 0,
                  "primaryImage": true,
                  "variantSku": null
                },
                {
                  "url": "https://example.com/redmi-18-white.png",
                  "altText": "Versão branca",
                  "position": 1,
                  "primaryImage": false,
                  "variantSku": "REDMI18-256-WHT"
                }
              ],
              "createdAt": "2026-10-05T20:56:43.887962Z",
              "updatedAt": "2026-10-05T20:56:43.887962Z"
            }
            """;

    @Operation(
            summary = "Create a product",
            description = "categoryId must point to an existing category or subcategory (returns 404 otherwise)."
    )
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "categoryId does not match any category")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO create(
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Product payload", required = true,
                    content = @Content(schema = @Schema(implementation = ProductRequestDTO.class),
                            examples = @ExampleObject(name = "New product", value = PRODUCT_EXAMPLE)))
            @RequestBody ProductRequestDTO productRequestDTO
    ) {
        return productService.create(productRequestDTO);
    }

    @Operation(
            summary = "List products (paginated + filters)",
            description = """
                    All filters are optional and combine with AND. `title` and `brand` are partial and \
                    case-insensitive; `status` is exact.

                    `categoryId` matches the category **and everything below it in the tree** — filtering by \
                    "Acessorios" also returns products filed under "Capas de Silicone". Pass \
                    `includeSubcategories=false` to match that one category exactly.

                    Pages are 10 items by default: `?page=0&size=10&sort=createdAt,desc`.
                    """
    )
    @ApiResponse(responseCode = "404", description = "categoryId does not match any category")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponseDTO<ProductResponseDTO> findAll(
            @ParameterObject ProductFilterDTO filter,
            @ParameterObject Pageable pageable) {
        return  productService.findAll(filter, pageable);
    }

    @Operation(
            summary = "Get a product by id",
            description = "Returns the product along with a summary of its category, including the breadcrumb path from the root."
    )
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{id}")
    public ProductResponseDTO findById(@PathVariable UUID id) {
        return productService.findById(id);
    }

    @Operation(
            summary = "Update a product",
            description = "Replaces every field, including categoryId — send the current categoryId back if it shouldn't change."
    )
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Product or categoryId not found")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDTO update(
            @PathVariable UUID id,
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Full product payload — every field is replaced", required = true,
                    content = @Content(schema = @Schema(implementation = ProductRequestDTO.class),
                            examples = @ExampleObject(name = "Updated product", value = PRODUCT_EXAMPLE)))
            @RequestBody ProductRequestDTO productRequestDTO
    ) {
        return productService.update(id, productRequestDTO);
    }

    @Operation(
            summary = "Delete a product",
            description = "Permanently removes the product. Does not affect its category."
    )
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
