package com.wlstore.order_service.gateway;

import com.wlstore.order_service.dto.OrderLineDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.wlstore.order_service.exception.ReservationRejectedException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class HttpInventoryGatewayTest {
    @Test
    void sendsTheReservationToTheInventoryAndAcceptsA204() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryGateway gateway = new HttpInventoryGateway(builder.build());
        UUID orderId = UUID.randomUUID();

        server.expect(requestTo("http://inventory/api/v1/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.lines[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andRespond(withNoContent());

        gateway.reserve(orderId, List.of(new OrderLineDTO("SKU-1", 2)));

        server.verify();
    }

    @Test
    void turnsAnInventory409IntoAReservationRejection() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryGateway gateway = new HttpInventoryGateway(builder.build());

        server.expect(requestTo("http://inventory/api/v1/reservations"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> gateway.reserve(UUID.randomUUID(), List.of(new OrderLineDTO("SKU-1", 2))))
                .isInstanceOf(ReservationRejectedException.class);
    }

    @Test
    void turnsAnInventory404IntoAReservationRejection() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryGateway gateway = new HttpInventoryGateway(builder.build());

        server.expect(requestTo("http://inventory/api/v1/reservations"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> gateway.reserve(UUID.randomUUID(), List.of(new OrderLineDTO("SKU-1", 2))))
                .isInstanceOf(ReservationRejectedException.class);
    }
}