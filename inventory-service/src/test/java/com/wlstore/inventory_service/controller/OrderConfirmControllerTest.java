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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderConfirmControllerTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Test
    void confirmingAnOrderAnswersNoContentAndConsumesTheUnits() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "CNF-HTTP", 10);
        reserve(tenant, orderId, "CNF-HTTP", 4);

        mockMvc.perform(confirm(tenant, orderId))
                .andExpect(status().isNoContent());

        Stock stock = stockOf(tenant, "CNF-HTTP");
        assertThat(stock.getQuantityOnHand()).isEqualTo(6);
        assertThat(stock.getQuantityReserved()).isZero();
    }

    @Test
    void confirmingTheSameOrderTwiceAnswersNoContentBothTimesAndConsumesOnce() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenStock(tenant, "CNF-TWICE", 20);
        reserve(tenant, first, "CNF-TWICE", 3);
        reserve(tenant, second, "CNF-TWICE", 4);

        mockMvc.perform(confirm(tenant, first)).andExpect(status().isNoContent());
        mockMvc.perform(confirm(tenant, first)).andExpect(status().isNoContent());

        Stock stock = stockOf(tenant, "CNF-TWICE");
        assertThat(stock.getQuantityOnHand()).isEqualTo(17);
        assertThat(stock.getQuantityReserved()).isEqualTo(4);
    }

    @Test
    void confirmingAnOrderThatNeverReservedAnswersNotFound() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        givenStock(tenant, "CNF-NONE", 10);
        reserve(tenant, UUID.randomUUID(), "CNF-NONE", 2);

        mockMvc.perform(confirm(tenant, unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Reservation"))
                .andExpect(jsonPath("$.fieldValue").value(unknown.toString()));

        Stock stock = stockOf(tenant, "CNF-NONE");
        assertThat(stock.getQuantityOnHand()).isEqualTo(10);
        assertThat(stock.getQuantityReserved()).isEqualTo(2);
    }

    @Test
    void confirmingAReleasedOrderAnswersConflictAndConsumesNothing() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "CNF-REL", 10);
        reserve(tenant, orderId, "CNF-REL", 3);
        TenantContext.runWithTenant(tenant, () -> stockService.release(orderId));

        mockMvc.perform(confirm(tenant, orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RELEASED"));

        Stock stock = stockOf(tenant, "CNF-REL");
        assertThat(stock.getQuantityOnHand()).isEqualTo(10);
        assertThat(stock.getQuantityReserved()).isZero();
    }

    @Test
    void anotherTenantCannotConfirmMyOrder() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(owner, "CNF-MINE", 10);
        reserve(owner, orderId, "CNF-MINE", 3);

        mockMvc.perform(confirm(intruder, orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceName").value("Reservation"));

        Stock stock = stockOf(owner, "CNF-MINE");
        assertThat(stock.getQuantityOnHand()).isEqualTo(10);
        assertThat(stock.getQuantityReserved()).isEqualTo(3);
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder confirm(UUID tenant, UUID orderId) {
        return post("/api/v1/reservations/{orderId}/confirm", orderId)
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

    private Stock stockOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku(sku).orElseThrow());
    }
}