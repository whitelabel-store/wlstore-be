package com.wlstore.catalog_service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceApplicationTests extends AbstractIntegrationTest {

	@Autowired
	Flyway flyway;

	@Test
	void contextLoadsAndAllMigrationsAreApplied() {
		assertThat(flyway.info().applied()).hasSize(6);
	}
}