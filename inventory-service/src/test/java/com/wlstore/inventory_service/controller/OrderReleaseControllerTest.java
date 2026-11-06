package com.wlstore.inventory_service.controller;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import com.wlstore.inventory_service.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderReleaseControllerTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void releasingAnOrderAnswersNoContentAndGivesTheUnitsBack() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "REL-HTTP", 10);
        reserve(tenant, orderId, "REL-HTTP", 4);
        assertThat(reservedOf(tenant, "REL-HTTP")).isEqualTo(4);

        mockMvc.perform(release(tenant, orderId))
                .andExpect(status().isNoContent());

        assertThat(reservedOf(tenant, "REL-HTTP")).isZero();
    }

    @Test
    void releasingTheSameOrderTwiceAnswersNoContentBothTimesAndGivesBackOnce() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenStock(tenant, "REL-TWICE", 20);
        reserve(tenant, first, "REL-TWICE", 3);
        reserve(tenant, second, "REL-TWICE", 4);

        mockMvc.perform(release(tenant, first)).andExpect(status().isNoContent());
        mockMvc.perform(release(tenant, first)).andExpect(status().isNoContent());

        // only the 3 units of the first order went back; the 4 of the second are still held
        assertThat(reservedOf(tenant, "REL-TWICE")).isEqualTo(4);
    }

    @Test
    void releasingAnOrderThatNeverReservedAnswersNoContent() throws Exception {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "REL-NONE", 10);
        reserve(tenant, UUID.randomUUID(), "REL-NONE", 2);

        mockMvc.perform(release(tenant, UUID.randomUUID()))
                .andExpect(status().isNoContent());

        assertThat(reservedOf(tenant, "REL-NONE")).isEqualTo(2);
    }

    @Test
    void anotherTenantCannotReleaseMyOrder() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(owner, "REL-MINE", 10);
        reserve(owner, orderId, "REL-MINE", 3);

        // for the intruder this order does not exist, so there is nothing to release
        mockMvc.perform(release(intruder, orderId))
                .andExpect(status().isNoContent());

        assertThat(reservedOf(owner, "REL-MINE")).isEqualTo(3);
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder release(UUID tenant, UUID orderId) {
        return post("/api/v1/reservations/{orderId}/release", orderId)
                .header(TENANT_HEADER, tenant.toString());
    }

    private void reserve(UUID tenant, UUID orderId, String sku, int quantity) {
        TenantContext.runWithTenant(tenant, () ->
                stockService.reserveAll(orderId, List.of(new ReservationLineDTO(sku, quantity))));
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