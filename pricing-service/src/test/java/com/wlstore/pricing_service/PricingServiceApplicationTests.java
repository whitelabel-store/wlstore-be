package com.wlstore.pricing_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PricingServiceApplicationTests extends AbstractIntegrationTest {

	@Autowired
	Flyway flyway;

	@Test
	void contextLoadsAndAllMigrationsAreApplied() {
		assertThat(flyway.info().applied()).hasSize(5);
	}
}