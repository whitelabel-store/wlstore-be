package com.wlstore.catalog_service.model;

import com.wlstore.catalog_service.enums.ActiveStatus;
import com.wlstore.common.persistence.TenantEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Table(name = "tb_category", uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_tenant_slug", columnNames = {"tenant_id", "slug"}),
        @UniqueConstraint(name = "uk_category_tenant_parent_title", columnNames = {"tenant_id", "parent_id", "title"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Category extends TenantEntity {
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slug", nullable = false)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private int position = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ActiveStatus status = ActiveStatus.ACTIVE;

    public void refreshPath() {
        this.path = (parent == null ? "" : parent.getPath()) + "/" + slug;
        this.depth = parent == null ? 0 : parent.getDepth() + 1;
        children.forEach(Category::refreshPath);
    }
}
