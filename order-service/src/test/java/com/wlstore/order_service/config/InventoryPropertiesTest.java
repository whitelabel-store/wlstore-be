package com.wlstore.order_service.config;

import com.wlstore.order_service.AbstractIntegrationTest;
import org.springframework.core.env.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryPropertiesTest extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void definesTheInventoryBaseUrl() {
        assertThat(environment.getProperty("inventory.base-url")).isNotBlank();
    }
}