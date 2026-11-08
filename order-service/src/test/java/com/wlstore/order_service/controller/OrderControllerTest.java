package com.wlstore.order_service.controller;

import com.wlstore.order_service.AbstractIntegrationTest;
import com.wlstore.order_service.exception.InventoryUnavailableException;
import com.wlstore.order_service.exception.ReservationRejectedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderControllerTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Test
    void createsAnOrderAndAnswersCreatedWithItsStatus() throws Exception {
        String body = """
                {"lines": [{"sku": "SKU-1", "quantity": 2}]}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .header(TENANT_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void answersConflictWhenTheInventoryRejectsTheReservation() throws Exception {
        doThrow(new ReservationRejectedException("Insufficient stock for SKU-1"))
                .when(inventoryGateway).reserve(any(), any(), any());

        String body = """
                {"lines": [{"sku": "SKU-1", "quantity": 2}]}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .header(TENANT_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void answersServiceUnavailableWhenTheInventoryCannotBeReached() throws Exception {
        doThrow(new InventoryUnavailableException("Inventory timed out"))
                .when(inventoryGateway).reserve(any(), any(), any());

        String body = """
                {"lines": [{"sku": "SKU-1", "quantity": 2}]}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .header(TENANT_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void answersBadRequestWhenTheOrderHasNoLines() throws Exception {
        String body = """
                {"lines": []}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .header(TENANT_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryGateway);
    }
}