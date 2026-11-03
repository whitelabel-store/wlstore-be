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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StockAdjustmentControllerTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StockRepository stockRepository;

    @Test
    void gettingAKnownSkuShowsOnHandReservedAndAvailable() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ADJ-GET", 10, 4);

        mockMvc.perform(get("/api/v1/stock/{sku}", "ADJ-GET")
                        .header(TENANT_HEADER, tenant.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("ADJ-GET"))
                .andExpect(jsonPath("$.quantityOnHand").value(10))
                .andExpect(jsonPath("$.quantityReserved").value(4))
                .andExpect(jsonPath("$.available").value(6));
    }

    @Test
    void gettingAnUnknownSkuAnswersNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/stock/{sku}", "ADJ-GHOST")
                        .header(TENANT_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Stock"));
    }

    @Test
    void settingOnHandReplacesTheValueAndKeepsTheReservations() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ADJ-SET", 5, 3);

        mockMvc.perform(setOnHand(tenant, "ADJ-SET", 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(20))
                .andExpect(jsonPath("$.quantityReserved").value(3))
                .andExpect(jsonPath("$.available").value(17));

        assertThat(stockOf(tenant, "ADJ-SET").getQuantityOnHand()).isEqualTo(20);
        assertThat(stockOf(tenant, "ADJ-SET").getQuantityReserved()).isEqualTo(3);
    }

    @Test
    void settingOnHandOfAnUnknownSkuAnswersNotFoundAndCreatesNothing() throws Exception {
        UUID tenant = UUID.randomUUID();

        mockMvc.perform(setOnHand(tenant, "ADJ-NEW", 10))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Stock"));

        assertThat(TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku("ADJ-NEW"))).isEmpty();
    }

    @Test
    void settingANegativeOnHandAnswersBadRequest() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ADJ-NEG", 5, 0);

        mockMvc.perform(setOnHand(tenant, "ADJ-NEG", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Quantity on hand cannot be negative"));

        assertThat(stockOf(tenant, "ADJ-NEG").getQuantityOnHand()).isEqualTo(5);
    }

    @Test
    void settingOnHandBelowWhatIsReservedAnswersConflictAndChangesNothing() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "ADJ-LOW", 10, 3);

        mockMvc.perform(setOnHand(tenant, "ADJ-LOW", 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.sku").value("ADJ-LOW"))
                .andExpect(jsonPath("$.requestedOnHand").value(2))
                .andExpect(jsonPath("$.reserved").value(3));

        assertThat(stockOf(tenant, "ADJ-LOW").getQuantityOnHand()).isEqualTo(10);
    }

    private MockHttpServletRequestBuilder setOnHand(UUID tenant, String sku, int quantityOnHand) {
        return put("/api/v1/stock/{sku}", sku)
                .header(TENANT_HEADER, tenant.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityOnHand\":%d}".formatted(quantityOnHand));
    }

    private void givenStock(UUID tenant, String sku, int onHand, int reserved) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(reserved).build()));
    }

    private Stock stockOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku(sku).orElseThrow());
    }
}