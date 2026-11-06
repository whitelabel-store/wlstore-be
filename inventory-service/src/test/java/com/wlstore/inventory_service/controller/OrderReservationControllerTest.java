package com.wlstore.inventory_service.controller;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderReservationControllerTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StockRepository stockRepository;

    @Test
    void reservingAnOrderAnswersNoContentAndHoldsEveryLine() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ORD-A", 10);
        givenStock(tenant, "ORD-B", 5);

        mockMvc.perform(reserve(tenant, UUID.randomUUID(), "{\"sku\":\"ORD-A\",\"quantity\":3}",
                        "{\"sku\":\"ORD-B\",\"quantity\":2}"))
                .andExpect(status().isNoContent());

        assertThat(reservedOf(tenant, "ORD-A")).isEqualTo(3);
        assertThat(reservedOf(tenant, "ORD-B")).isEqualTo(2);
    }

    @Test
    void sendingTheSameOrderTwiceAnswersNoContentBothTimesAndReservesOnce() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "ORD-RETRY", 10);

        mockMvc.perform(reserve(tenant, orderId, "{\"sku\":\"ORD-RETRY\",\"quantity\":4}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(reserve(tenant, orderId, "{\"sku\":\"ORD-RETRY\",\"quantity\":4}"))
                .andExpect(status().isNoContent());

        assertThat(reservedOf(tenant, "ORD-RETRY")).isEqualTo(4);
    }

    @Test
    void aLineWithoutStockAnswersConflictAndNothingStaysReserved() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ORD-OK", 10);
        givenStock(tenant, "ORD-LOW", 1);

        mockMvc.perform(reserve(tenant, UUID.randomUUID(), "{\"sku\":\"ORD-OK\",\"quantity\":3}",
                        "{\"sku\":\"ORD-LOW\",\"quantity\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.sku").value("ORD-LOW"))
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.available").value(1));

        // the first line had stock and was written before the second failed: it must not stay reserved
        assertThat(reservedOf(tenant, "ORD-OK")).isZero();
        assertThat(reservedOf(tenant, "ORD-LOW")).isZero();
    }

    @Test
    void anUnknownSkuAnswersNotFoundAndNothingStaysReserved() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ORD-REAL", 10);

        mockMvc.perform(reserve(tenant, UUID.randomUUID(), "{\"sku\":\"ORD-REAL\",\"quantity\":3}",
                        "{\"sku\":\"ORD-GHOST\",\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Stock"))
                .andExpect(jsonPath("$.fieldValue").value("ORD-GHOST"));

        assertThat(reservedOf(tenant, "ORD-REAL")).isZero();
    }

    @Test
    void aNonPositiveQuantityAnswersBadRequest() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ORD-ZERO", 10);

        mockMvc.perform(reserve(tenant, UUID.randomUUID(), "{\"sku\":\"ORD-ZERO\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        assertThat(reservedOf(tenant, "ORD-ZERO")).isZero();
    }

    @Test
    void anOrderWithoutLinesAnswersBadRequest() throws Exception {
        mockMvc.perform(reserve(UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void stockOfAnotherTenantIsInvisibleToTheOrder() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        givenStock(owner, "ORD-MINE", 10);

        mockMvc.perform(reserve(intruder, UUID.randomUUID(), "{\"sku\":\"ORD-MINE\",\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Stock"));

        assertThat(reservedOf(owner, "ORD-MINE")).isZero();
    }

    @Test
    void anOrderWithoutAnOrderIdAnswersBadRequest() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ORD-NOID", 10);

        mockMvc.perform(post("/api/v1/reservations")
                        .header(TENANT_HEADER, tenant.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"sku\":\"ORD-NOID\",\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        assertThat(reservedOf(tenant, "ORD-NOID")).isZero();
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder reserve(UUID tenant, UUID orderId, String... lines) {
        String body = "{\"orderId\":\"%s\",\"lines\":[%s]}".formatted(orderId, String.join(",", lines));

        return post("/api/v1/reservations")
                .header(TENANT_HEADER, tenant.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private void givenStock(UUID tenant, String sku, int onHand) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(0).build()));
    }

    private int reservedOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant,
                () -> stockRepository.findBySku(sku).orElseThrow().getQuantityReserved());
    }
}