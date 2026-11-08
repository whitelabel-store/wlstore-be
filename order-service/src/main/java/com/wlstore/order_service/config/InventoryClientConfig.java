package com.wlstore.order_service.config;

import com.wlstore.order_service.gateway.HttpInventoryGateway;
import com.wlstore.order_service.gateway.InventoryGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class InventoryClientConfig {

    @Bean
    public InventoryGateway inventoryGateway(
            RestClient.Builder builder,
            @Value("${inventory.base-url}") String baseUrl) {
        return new HttpInventoryGateway(builder.baseUrl(baseUrl).build());
    }
}
