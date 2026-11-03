package com.wlstore.pricing_service.migration;

import com.wlstore.pricing_service.AbstractIntegrationTest;
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
 * Banco que aprendeu os SKUs de demonstração por evento (produtos do seed-demo.sh criados
 * pela API) antes da V5 existir. O callback beforeMigrate.sql precisa abrir caminho para a V5.
 * Cada teste usa um schema próprio, fora do public que o contexto do Spring já migrou.
 */
class LegacyDemoSkuMigrationTest extends AbstractIntegrationTest {

    private static final String TENANT = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String SEED_REDMI_ID = "534b565f-c3e1-4114-a0cb-52c47bca89b7";

    @Autowired
    DataSource dataSource;

    @Test
    void v5RunsOverDemoSkusLearnedFromEvents() {
        String schema = "legacy_" + UUID.randomUUID().toString().substring(0, 8);
        flyway(schema, "4").migrate();
        JdbcTemplate jdbc = jdbc(schema);

        jdbc.update("""
                INSERT INTO tb_known_sku (id, created_at, updated_at, version, tenant_id, product_id, sku)
                VALUES (gen_random_uuid(), now(), now(), 0, ?::uuid, gen_random_uuid(), 'REDMI18-128-BLK')""", TENANT);
        jdbc.update("""
                INSERT INTO tb_price (id, created_at, updated_at, version, tenant_id, sku, amount_cents, valid_from)
                VALUES (gen_random_uuid(), now(), now(), 0, ?::uuid, 'REDMI18-128-BLK', 9000, now())""", TENANT);
        long publicBefore = new JdbcTemplate(dataSource).queryForObject("SELECT count(*) FROM public.tb_known_sku", Long.class);

        flyway(schema, null).migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_known_sku", Long.class)).isEqualTo(100L);
        assertThat(jdbc.queryForObject("SELECT product_id::text FROM tb_known_sku WHERE sku = 'REDMI18-128-BLK'", String.class))
                .isEqualTo(SEED_REDMI_ID);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_price WHERE sku = 'REDMI18-128-BLK' AND valid_to IS NULL", Long.class))
                .isEqualTo(1L);
        // o callback só mexe no schema que está sendo migrado
        assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT count(*) FROM public.tb_known_sku", Long.class))
                .isEqualTo(publicBefore);
    }

    @Test
    void afterV5TheCallbackNeverTouchesSkusAgain() {
        String schema = "legacy_" + UUID.randomUUID().toString().substring(0, 8);
        flyway(schema, null).migrate();
        JdbcTemplate jdbc = jdbc(schema);

        // preço novo depois da V5 (dado de verdade): fecha o vigente e abre outro
        jdbc.update("UPDATE tb_price SET valid_to = now() WHERE sku = 'REDMI18-128-BLK' AND valid_to IS NULL");
        jdbc.update("""
                INSERT INTO tb_price (id, created_at, updated_at, version, tenant_id, sku, amount_cents, valid_from)
                VALUES (gen_random_uuid(), now(), now(), 0, ?::uuid, 'REDMI18-128-BLK', 8500, now() + interval '1 second')""", TENANT);

        flyway(schema, null).migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_price WHERE sku = 'REDMI18-128-BLK'", Long.class))
                .isEqualTo(2L);
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
