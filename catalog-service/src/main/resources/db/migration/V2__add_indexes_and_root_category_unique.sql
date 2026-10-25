-- V2: índices que o Hibernate não gera.

-- 1) Categorias raiz: título único por tenant.
-- A UNIQUE (tenant_id, parent_id, title) do V1 não protege a raiz: no Postgres, NULL nunca é
-- igual a NULL, então duas raízes com o mesmo título passam. O índice parcial resolve.
CREATE UNIQUE INDEX uk_category_tenant_root_title
    ON tb_category (tenant_id, title)
    WHERE parent_id IS NULL;

-- 2) Índices nas chaves estrangeiras.
-- O Postgres indexa o lado referenciado (a PK), mas não o lado que referencia.
-- Sem eles, "variants do produto X" e o delete em cascata viram varredura da tabela inteira.
CREATE INDEX idx_category_parent_id ON tb_category (parent_id);
CREATE INDEX idx_product_category_id ON tb_product (category_id);
CREATE INDEX idx_variant_product_id ON tb_product_variant (product_id);
CREATE INDEX idx_image_product_id ON tb_product_image (product_id);
CREATE INDEX idx_image_variant_id ON tb_product_image (variant_id);

-- 3) Busca por subárvore: path LIKE '/acessorios/%' (a consulta findIdsInSubtree).
-- varchar_pattern_ops permite ao índice atender LIKE 'prefixo%' em qualquer collation.
CREATE INDEX idx_category_tenant_path
    ON tb_category (tenant_id, path varchar_pattern_ops);