package com.wlstore.catalog_service.migration;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Banco que recebeu os produtos de demonstração pela API (seed-demo.sh) antes da V7 existir:
 * mesmos títulos/SKUs, ids aleatórios. O callback beforeMigrate.sql precisa abrir caminho
 * para a V7 — foi assim que prod ficou preso na 1.2.0 e na 1.2.3.
 * Cada teste usa um schema próprio, fora do public que o contexto do Spring já migrou.
 */
class LegacyDemoSeedMigrationTest extends AbstractIntegrationTest {

    private static final String TENANT = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String SEED_REDMI_ID = "534b565f-c3e1-4114-a0cb-52c47bca89b7";

    @Autowired
    DataSource dataSource;

    @Test
    void v7RunsOverDemoProductsCreatedThroughTheApi() {
        String schema = "legacy_" + UUID.randomUUID().toString().substring(0, 8);
        flyway(schema, "6").migrate();
        JdbcTemplate jdbc = jdbc(schema);

        UUID legacyId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tb_product (id, created_at, updated_at, version, tenant_id, brand, description, status, title, category_id)
                SELECT ?, now(), now(), 0, ?::uuid, 'Xiaomi', 'via API', 'ACTIVE', 'Redmi 18 Pro', id
                FROM tb_category WHERE tenant_id = ?::uuid AND slug = 'smartphones'""", legacyId, TENANT, TENANT);
        jdbc.update("""
                INSERT INTO tb_product_variant (id, created_at, updated_at, version, tenant_id, name, "position", sku, status, product_id)
                VALUES (gen_random_uuid(), now(), now(), 0, ?::uuid, 'Redmi', 0, 'REDMI18-128-BLK', 'ACTIVE', ?)""", TENANT, legacyId);
        jdbc.update("""
                INSERT INTO tb_variant_price (id, created_at, updated_at, version, tenant_id, product_id, sku, amount_cents, valid_from)
                VALUES (gen_random_uuid(), now(), now(), 0, ?::uuid, ?, 'REDMI18-128-BLK', 9000, now())""", TENANT, legacyId);

        long publicBefore = new JdbcTemplate(dataSource).queryForObject("SELECT count(*) FROM public.tb_product", Long.class);

        flyway(schema, null).migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_product", Long.class)).isEqualTo(100L);
        // o callback só mexe no schema que está sendo migrado
        assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT count(*) FROM public.tb_product", Long.class))
                .isEqualTo(publicBefore);
        assertThat(jdbc.queryForObject("SELECT id::text FROM tb_product WHERE title = 'Redmi 18 Pro'", String.class))
                .isEqualTo(SEED_REDMI_ID);
        assertThat(jdbc.queryForObject("SELECT product_id::text FROM tb_variant_price WHERE sku = 'REDMI18-128-BLK'", String.class))
                .isEqualTo(SEED_REDMI_ID);
    }

    @Test
    void afterV7TheCallbackNeverTouchesProductsAgain() {
        String schema = "legacy_" + UUID.randomUUID().toString().substring(0, 8);
        flyway(schema, null).migrate();
        JdbcTemplate jdbc = jdbc(schema);

        // produto do seed apagado e recadastrado (novo id) depois da V7: é dado de verdade
        jdbc.update("DELETE FROM tb_product_image WHERE product_id = ?::uuid", SEED_REDMI_ID);
        jdbc.update("DELETE FROM tb_product_variant WHERE product_id = ?::uuid", SEED_REDMI_ID);
        jdbc.update("DELETE FROM tb_product WHERE id = ?::uuid", SEED_REDMI_ID);
        UUID recreated = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tb_product (id, created_at, updated_at, version, tenant_id, brand, description, status, title, category_id)
                SELECT ?, now(), now(), 0, ?::uuid, 'Xiaomi', 'recadastrado', 'ACTIVE', 'Redmi 18 Pro', id
                FROM tb_category WHERE tenant_id = ?::uuid AND slug = 'smartphones'""", recreated, TENANT, TENANT);

        flyway(schema, null).migrate();

        assertThat(jdbc.queryForObject("SELECT id FROM tb_product WHERE title = 'Redmi 18 Pro'", UUID.class))
                .isEqualTo(recreated);
    }

    private Flyway flyway(String schema, String target) {
        var config = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    // conexão dedicada: com o pool, o search_path podia ficar numa conexão e a query ir em outra
    private JdbcTemplate jdbc(String schema) {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(dataSource.getConnection(), true));
            jdbc.execute("SET search_path TO " + schema);
            return jdbc;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
