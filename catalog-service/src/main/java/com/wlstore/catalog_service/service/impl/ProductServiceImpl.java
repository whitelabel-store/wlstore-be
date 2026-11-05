package com.wlstore.catalog_service.service.impl;

import com.wlstore.common.web.PageResponseDTO;
import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.catalog_service.dto.ProductFilterDTO;
import com.wlstore.catalog_service.dto.ProductImageRequestDTO;
import com.wlstore.catalog_service.dto.ProductRequestDTO;
import com.wlstore.catalog_service.dto.ProductResponseDTO;
import com.wlstore.catalog_service.dto.ProductVariantRequestDTO;
import com.wlstore.catalog_service.enums.ActiveStatus;
import com.wlstore.catalog_service.mapper.ProductMapper;
import com.wlstore.catalog_service.model.Category;
import com.wlstore.catalog_service.model.Product;
import com.wlstore.catalog_service.model.ProductImage;
import com.wlstore.catalog_service.model.ProductVariant;
import com.wlstore.catalog_service.model.VariantPrice;
import com.wlstore.catalog_service.model.VariantStock;
import com.wlstore.catalog_service.outbox.ProductEventRecorder;
import com.wlstore.catalog_service.repository.CategoryRepository;
import com.wlstore.catalog_service.repository.ProductRepository;
import com.wlstore.catalog_service.repository.ProductSpecifications;
import com.wlstore.catalog_service.repository.VariantPriceRepository;
import com.wlstore.catalog_service.repository.VariantStockRepository;
import com.wlstore.catalog_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    // cheapest first; the SKU only breaks ties so the result never depends on database row order
    private static final Comparator<VariantPrice> CHEAPEST_FIRST =
            Comparator.comparingLong(VariantPrice::getAmountCents).thenComparing(VariantPrice::getSku);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VariantPriceRepository variantPriceRepository;
    private final VariantStockRepository variantStockRepository;
    private final ProductMapper productMapper;
    private final ProductEventRecorder eventRecorder;

    @Override
    @Transactional
    public ProductResponseDTO create(ProductRequestDTO requestDTO) {
        validateChildren(requestDTO);

        Product product = productMapper.toEntity(requestDTO);
        product.setCategory(findCategory(requestDTO.category().id()));

        syncVariants(product, requestDTO.variants());
        addImages(product, requestDTO.images());

        Product saveProduct = productRepository.saveAndFlush(product);

        eventRecorder.record(ProductEventRecorder.PRODUCT_CREATED, saveProduct);

        return productMapper.toResponseDTO(saveProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductResponseDTO> findAll(ProductFilterDTO filter, Pageable pageable) {
        Collection<UUID> categoryIds = resolveCategoryIds(filter);

        // priceFromCents is not an attribute of Product: when it is in the sort, the specification builds the
        // whole ORDER BY and the Pageable carries only the page number and size (Spring would override it otherwise)
        boolean sortsByPrice = pageable.getSort().getOrderFor(ProductSpecifications.PRICE_SORT) != null;

        Specification<Product> specification = sortsByPrice
                ? ProductSpecifications.withFilters(filter, categoryIds, pageable.getSort())
                : ProductSpecifications.withFilters(filter, categoryIds);

        Pageable effectivePageable = sortsByPrice
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : pageable;

        Page<Product> products = productRepository.findAll(specification, effectivePageable);

        // same order as the page: the sort was decided by the query, the DTOs only follow it
        List<ProductResponseDTO> content = toResponses(products.getContent());

        return PageResponseDTO.from(new PageImpl<>(content, products.getPageable(), products.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO findById(UUID id) {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        return toResponses(List.of(product)).getFirst();
    }

    @Override
    @Transactional
    public ProductResponseDTO update(UUID id, ProductRequestDTO requestDTO) {
        validateChildren(requestDTO);

        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        productMapper.updateProductFromRequest(requestDTO, product);
        product.setCategory(findCategory(requestDTO.category().id()));

        // As imagens antigas saem primeiro e o flush as apaga já: uma delas pode apontar para
        // uma variant que vamos remover logo abaixo, e apagar a variant antes violaria a FK.
        product.getImages().clear();
        productRepository.flush();

        syncVariants(product, requestDTO.variants());
        addImages(product, requestDTO.images());

        Product updateProduct = productRepository.save(product);

        eventRecorder.record(ProductEventRecorder.PRODUCT_UPDATED, updateProduct);

        return productMapper.toResponseDTO(updateProduct);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        // Carrega o produto (em vez de existsById) porque o evento leva o último snapshot dele.
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        eventRecorder.record(ProductEventRecorder.PRODUCT_DELETED, product);

        // O preço copiado do pricing não tem FK para o produto: sem isto a linha fica órfã e
        // segura o SKU (uk_variant_price_tenant_sku) para sempre.
        variantPriceRepository.deleteByProductId(product.getId());
        productRepository.delete(product);
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }

    /**
     * Completes the responses of a list of products with data that lives in other read models: the "from"
     * price and the availability. Two queries for the whole list (IN), whatever its size, so it serves the
     * listing (a page) and the detail (a list of one) alike. The result keeps the order of the input.
     */
    private List<ProductResponseDTO> toResponses(List<Product> products) {
        Map<UUID, VariantPrice> cheapestByProduct = cheapestPriceByProduct(
                products.stream().map(Product::getId).toList());

        Map<String, Integer> availableBySku = availableBySku(products);

        return products.stream()
                .map(product -> withPriceAndStock(
                        productMapper.toResponseDTO(product),
                        cheapestByProduct.get(product.getId()),
                        inStockOf(product, availableBySku)))
                .toList();
    }

    /**
     * One query for the whole list (IN), then the cheapest price row per product. The "from" price and
     * its original ("de") come from that same row, so the discount is never mixed across variants.
     * Products without any price are absent from the map.
     */
    private Map<UUID, VariantPrice> cheapestPriceByProduct(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return variantPriceRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        VariantPrice::getProductId,
                        Function.identity(),
                        BinaryOperator.minBy(CHEAPEST_FIRST)));
    }

    /**
     * One query for the whole list (IN): the available units of every variant, by SKU.
     * A SKU without a stock row is absent from the map, which means "no information".
     */
    private Map<String, Integer> availableBySku(Collection<Product> products) {
        Set<String> skus = products.stream()
                .flatMap(product -> product.getVariants().stream())
                .map(ProductVariant::getSku)
                .collect(Collectors.toSet());

        if (skus.isEmpty()) {
            return Map.of();
        }

        return variantStockRepository.findBySkuIn(skus).stream()
                .collect(Collectors.toMap(VariantStock::getSku, VariantStock::getAvailable));
    }

    /**
     * true: some variant has units available. false: there is stock information and every variant is
     * sold out. null: no variant has a stock row yet, so we do not know (it must not look sold out).
     */
    private Boolean inStockOf(Product product, Map<String, Integer> availableBySku) {
        boolean known = false;

        for (ProductVariant variant : product.getVariants()) {
            Integer available = availableBySku.get(variant.getSku());
            if (available == null) {
                continue;
            }
            if (available > 0) {
                return Boolean.TRUE;
            }
            known = true;
        }

        return known ? Boolean.FALSE : null;
    }

    // Prices and availability come from other sources (the read models), so the response is completed
    // here, not in the mapper.
    private ProductResponseDTO withPriceAndStock(ProductResponseDTO dto, VariantPrice cheapest, Boolean inStock) {
        Long amount = cheapest == null ? null : cheapest.getAmountCents();
        Long original = cheapest == null ? null : cheapest.getOriginalAmountCents();

        return new ProductResponseDTO(dto.id(), dto.title(), dto.brand(), dto.description(), dto.status(),
                dto.category(), dto.variants(), dto.images(), amount, original, inStock,
                dto.createdAt(), dto.updatedAt());
    }

    /**
     * Regras que o Bean Validation não expressa: dependem de olhar a lista inteira.
     */
    private void validateChildren(ProductRequestDTO requestDTO) {
        Set<String> skus = new HashSet<>();
        for (ProductVariantRequestDTO variant : requestDTO.variants()) {
            if (!skus.add(variant.sku())) {
                throw new IllegalArgumentException("Duplicate variant SKU: " + variant.sku());
            }
        }

        List<ProductImageRequestDTO> images = requestDTO.images() == null ? List.of() : requestDTO.images();

        long primaryCount = images.stream().filter(ProductImageRequestDTO::primaryImage).count();
        if (primaryCount > 1) {
            throw new IllegalArgumentException("Only one image can be primary");
        }

        for (ProductImageRequestDTO image : images) {
            if (image.variantSku() != null && !skus.contains(image.variantSku())) {
                throw new IllegalArgumentException("Image references unknown variant SKU: " + image.variantSku());
            }
        }
    }

    /**
     * Casa as variants pelo SKU: conhecido atualiza, novo cria, ausente da lista é removido
     * (orphanRemoval). No create a lista do produto está vazia, então tudo cai no "novo".
     */
    private void syncVariants(Product product, List<ProductVariantRequestDTO> requested) {
        Set<String> requestedSkus = new HashSet<>();
        for (ProductVariantRequestDTO dto : requested) {
            requestedSkus.add(dto.sku());
        }

        Set<String> removedSkus = new HashSet<>();
        product.getVariants().removeIf(variant -> {
            boolean removed = !requestedSkus.contains(variant.getSku());
            if (removed) {
                removedSkus.add(variant.getSku());
            }
            return removed;
        });

        // Variant removida leva junto o preço copiado, senão ele ainda conta no "a partir de" da listagem.
        if (!removedSkus.isEmpty()) {
            variantPriceRepository.deleteByProductIdAndSkuIn(product.getId(), removedSkus);
        }

        Map<String, ProductVariant> existing = new HashMap<>();
        for (ProductVariant variant : product.getVariants()) {
            existing.put(variant.getSku(), variant);
        }

        for (int index = 0; index < requested.size(); index++) {
            ProductVariantRequestDTO dto = requested.get(index);
            int position = dto.position() != null ? dto.position() : index;
            ProductVariant variant = existing.get(dto.sku());

            if (variant == null) {
                product.addVariant(buildVariant(dto, position));
            } else {
                variant.setName(dto.name());
                variant.setOptionValues(copyOptions(dto.optionValues()));
                variant.setWeightGrams(dto.weightGrams());
                variant.setStatus(dto.status() != null ? dto.status() : ActiveStatus.ACTIVE);
                variant.setPosition(position);
            }
        }
    }

    private ProductVariant buildVariant(ProductVariantRequestDTO dto, int position) {
        return ProductVariant.builder()
                .sku(dto.sku())
                .name(dto.name())
                .optionValues(copyOptions(dto.optionValues()))
                .weightGrams(dto.weightGrams())
                .status(dto.status() != null ? dto.status() : ActiveStatus.ACTIVE)
                .position(position)
                .build();
    }

    // O builder sobrescreve o @Builder.Default com null se passarmos null: por isso a cópia.
    private Map<String, String> copyOptions(Map<String, String> options) {
        return options == null ? new HashMap<>() : new HashMap<>(options);
    }

    private void addImages(Product product, List<ProductImageRequestDTO> requested) {
        if (requested == null) {
            return;
        }

        Map<String, ProductVariant> variantsBySku = new HashMap<>();
        for (ProductVariant variant : product.getVariants()) {
            variantsBySku.put(variant.getSku(), variant);
        }

        for (int index = 0; index < requested.size(); index++) {
            ProductImageRequestDTO dto = requested.get(index);

            ProductImage image = ProductImage.builder()
                    .url(dto.url())
                    .altText(dto.altText())
                    .position(dto.position() != null ? dto.position() : index)
                    .primaryImage(dto.primaryImage())
                    .variant(dto.variantSku() == null ? null : variantsBySku.get(dto.variantSku()))
                    .build();

            product.addImage(image);
        }
    }

    /**
     * Returns null when no category filter was sent, so the specification skips the IN clause entirely.
     */
    private Collection<UUID> resolveCategoryIds(ProductFilterDTO filter) {
        if (filter.categoryId() == null && !StringUtils.hasText(filter.categorySlug())) {
            return null;
        }

        Category category = filter.categoryId() != null
                ? findCategory(filter.categoryId())
                : categoryRepository
                .findBySlug(filter.categorySlug())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", filter.categorySlug()));

        if (!filter.shouldIncludeSubchildren()) {
            return Set.of(category.getId());
        }

        return new LinkedHashSet<>(categoryRepository.findIdsInSubtree(category.getPath()));
    }
}