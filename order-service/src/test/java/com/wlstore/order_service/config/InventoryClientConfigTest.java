package com.wlstore.order_service.config;

import com.wlstore.order_service.dto.OrderLineDTO;
import com.wlstore.order_service.gateway.HttpInventoryGateway;
import com.wlstore.order_service.gateway.InventoryGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

import java.util.List;
import java.util.UUID;

public class InventoryClientConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(InventoryClientConfig.class)
            .withBean(RestClient.Builder.class, () -> RestClient.builder());

    @Test
    void registersTheHttpGatewayAsTheInventoryGatewayBean() {
        runner.withPropertyValues("inventory.base-url=http://inventory")
                .run(context -> assertThat(context.getBean(InventoryGateway.class))
                        .isInstanceOf(HttpInventoryGateway.class));
    }

    @Test
    void buildsAnInventoryGatewayThatCallsTheConfiguredBaseUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        InventoryGateway gateway = new InventoryClientConfig()
                .inventoryGateway(builder, "http://inventory");

        server.expect(requestTo("http://inventory/api/v1/reservations"))
                .andRespond(withNoContent());

        gateway.reserve(UUID.randomUUID(), UUID.randomUUID(), List.of(new OrderLineDTO("SKU-1", 2)));

        server.verify();
    }
}
