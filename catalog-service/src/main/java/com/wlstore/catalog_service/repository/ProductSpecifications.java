package com.wlstore.catalog_service.repository;

import com.wlstore.catalog_service.dto.ProductFilterDTO;
import com.wlstore.catalog_service.model.Product;
import com.wlstore.catalog_service.model.VariantPrice;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class ProductSpecifications {
    public static final String PRICE_SORT = "priceFromCents";

    private ProductSpecifications(){}

    public static Specification<Product> withFilters(ProductFilterDTO filter, Collection<UUID> categoryIds) {
        return (root, query, builder) ->
                builder.and(predicates(filter, categoryIds, root, query, builder));
    }

    /**
     * Same filters, but the specification also builds the whole ORDER BY. Needed when sorting by price:
     * priceFromCents is not an attribute of Product, so Spring Data cannot translate it by itself.
     */
    public static Specification<Product> withFilters(ProductFilterDTO filter, Collection<UUID> categoryIds, Sort sort) {
        return (root, query, builder) -> {
            query.orderBy(orders(sort, root, query, builder));
            return builder.and(predicates(filter, categoryIds, root, query, builder));
        };
    }

    private static Predicate[] predicates(ProductFilterDTO filter, Collection<UUID> categoryIds,
                                          Root<Product> root, AbstractQuery<?> query, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.hasText(filter.title())) {
            predicates.add(builder.like(builder.lower(root.get("title")), contains(filter.title())));
        }

        if (StringUtils.hasText(filter.brand())) {
            predicates.add(builder.like(builder.lower(root.get("brand")), contains(filter.brand())));
        }

        if (filter.status() != null) {
            predicates.add(builder.equal(root.get("status"), filter.status()));
        }

        if (categoryIds != null) {
            predicates.add(root.get("category").get("id").in(categoryIds));
        }

        if (filter.minPriceCents() != null || filter.maxPriceCents() != null) {
            Subquery<Long> lowestPrice = lowestPrice(root, query, builder);

            if (filter.minPriceCents() != null) {
                predicates.add(builder.greaterThanOrEqualTo(lowestPrice, filter.minPriceCents()));
            }

            if (filter.maxPriceCents() != null) {
                predicates.add(builder.lessThanOrEqualTo(lowestPrice, filter.maxPriceCents()));
            }
        }

        return predicates.toArray(Predicate[]::new);
    }

    /** The number the listing shows: the lowest price among the product's variants (null when it has none). */
    private static Subquery<Long> lowestPrice(Root<Product> root, AbstractQuery<?> query, CriteriaBuilder builder) {
        Subquery<Long> lowestPrice = query.subquery(Long.class);
        Root<VariantPrice> price = lowestPrice.from(VariantPrice.class);
        lowestPrice.select(builder.min(price.<Long>get("amountCents")))
                .where(builder.equal(price.get("productId"), root.get("id")));
        return lowestPrice;
    }

    private static List<Order> orders(Sort sort, Root<Product> root, AbstractQuery<?> query, CriteriaBuilder builder) {
        List<Order> orders = new ArrayList<>();

        for (Sort.Order order : sort) {
            if (PRICE_SORT.equals(order.getProperty())) {
                // products without price always go last, in both directions
                Expression<Long> price = builder.coalesce(
                        lowestPrice(root, query, builder),
                        order.isAscending() ? Long.MAX_VALUE : -1L);
                orders.add(order.isAscending() ? builder.asc(price) : builder.desc(price));
            } else {
                Path<?> path = root;
                for (String part : order.getProperty().split("\\.")) {
                    path = path.get(part);
                }
                orders.add(order.isAscending() ? builder.asc(path) : builder.desc(path));
            }
        }

        // tie-breaker: keeps pagination stable when several products share the same value
        orders.add(builder.asc(root.get("id")));
        return orders;
    }

    private static String contains(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}