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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StockControllerTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StockRepository stockRepository;

    @Test
    void reservingAvailableStockAnswersNoContentAndHoldsTheUnits() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "HTTP-OK", 10, 0);

        mockMvc.perform(reserve(tenant, "HTTP-OK", 3))
                .andExpect(status().isNoContent());

        assertThat(reservedOf(tenant, "HTTP-OK")).isEqualTo(3);
    }

    @Test
    void reservingAnUnknownSkuAnswersNotFound() throws Exception {
        mockMvc.perform(reserve(UUID.randomUUID(), "HTTP-GHOST", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Stock"))
                .andExpect(jsonPath("$.fieldValue").value("HTTP-GHOST"));
    }

    @Test
    void reservingMoreThanAvailableAnswersConflictAndChangesNothing() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "HTTP-LOW", 5, 3);

        mockMvc.perform(reserve(tenant, "HTTP-LOW", 5))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.sku").value("HTTP-LOW"))
                .andExpect(jsonPath("$.requested").value(5))
                .andExpect(jsonPath("$.available").value(2));

        assertThat(reservedOf(tenant, "HTTP-LOW")).isEqualTo(3);
    }

    @Test
    void aNonPositiveQuantityAnswersBadRequest() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "HTTP-ZERO", 10, 0);

        mockMvc.perform(reserve(tenant, "HTTP-ZERO", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        assertThat(reservedOf(tenant, "HTTP-ZERO")).isZero();
    }

    @Test
    void stockOfAnotherTenantIsInvisibleOverHttp() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        givenStock(owner, "HTTP-MINE", 10, 0);

        mockMvc.perform(reserve(intruder, "HTTP-MINE", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Stock"));

        assertThat(reservedOf(owner, "HTTP-MINE")).isZero();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder reserve(
            UUID tenant, String sku, int quantity) {
        return post("/api/v1/stock/{sku}/reservations", sku)
                .header(TENANT_HEADER, tenant.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":%d}".formatted(quantity));
    }

    private void givenStock(UUID tenant, String sku, int onHand, int reserved) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(reserved).build()));
    }

    private int reservedOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant,
                () -> stockRepository.findBySku(sku).orElseThrow().getQuantityReserved());
    }
}