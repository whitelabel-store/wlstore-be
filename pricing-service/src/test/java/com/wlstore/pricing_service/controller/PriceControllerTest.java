package com.wlstore.pricing_service.controller;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.pricing_service.AbstractIntegrationTest;
import com.wlstore.pricing_service.service.SkuSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PriceControllerTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SkuSyncService skuSyncService;

    @Test
    void definesThePriceOfAKnownSku() throws Exception {
        UUID tenant = UUID.randomUUID();
        registerSku(tenant, "API-001");

        mockMvc.perform(put("/api/v1/prices/API-001")
                        .header(TENANT_HEADER, tenant.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":12990,\"originalAmountCents\":15990}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("API-001"))
                .andExpect(jsonPath("$.amountCents").value(12990))
                .andExpect(jsonPath("$.originalAmountCents").value(15990))
                .andExpect(jsonPath("$.validFrom").exists());
    }

    @Test
    void answersNotFoundForASkuTheTenantDoesNotKnow() throws Exception {
        mockMvc.perform(put("/api/v1/prices/API-UNKNOWN")
                        .header(TENANT_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":12990}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void rejectsOriginalPriceThatIsNotGreaterThanTheCurrentPrice() throws Exception {
        UUID tenant = UUID.randomUUID();
        registerSku(tenant, "API-003");

        mockMvc.perform(put("/api/v1/prices/API-003")
                        .header(TENANT_HEADER, tenant.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":12990,\"originalAmountCents\":12990}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void rejectsAnAmountThatIsNotPositive() throws Exception {
        UUID tenant = UUID.randomUUID();
        registerSku(tenant, "API-004");

        mockMvc.perform(put("/api/v1/prices/API-004")
                        .header(TENANT_HEADER, tenant.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void returnsTheCurrentPricesOfTheRequestedSkusAndSkipsTheOnesWithoutPrice() throws Exception {
        UUID tenant = UUID.randomUUID();
        registerSku(tenant, "LST-001");
        registerSku(tenant, "LST-002");
        registerSku(tenant, "LST-003");
        definePrice(tenant, "LST-001", 10000);
        definePrice(tenant, "LST-002", 20000);

        mockMvc.perform(get("/api/v1/prices")
                        .param("skus", "LST-001,LST-002,LST-003")
                        .header(TENANT_HEADER, tenant.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].sku", containsInAnyOrder("LST-001", "LST-002")));
    }

    @Test
    void returnsOnlyTheCurrentPriceWhenThePriceChanged() throws Exception {
        UUID tenant = UUID.randomUUID();
        registerSku(tenant, "LST-010");
        definePrice(tenant, "LST-010", 10000);
        definePrice(tenant, "LST-010", 9000);

        mockMvc.perform(get("/api/v1/prices")
                        .param("skus", "LST-010")
                        .header(TENANT_HEADER, tenant.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amountCents").value(9000));
    }

    @Test
    void doesNotReturnThePricesOfAnotherTenant() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        registerSku(tenantA, "LST-020");
        registerSku(tenantB, "LST-020");
        definePrice(tenantA, "LST-020", 10000);

        mockMvc.perform(get("/api/v1/prices")
                        .param("skus", "LST-020")
                        .header(TENANT_HEADER, tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void returnsTheWholePriceHistoryOldestFirst() throws Exception {
        UUID tenant = UUID.randomUUID();
        registerSku(tenant, "HST-001");
        definePrice(tenant, "HST-001", 10000);
        definePrice(tenant, "HST-001", 9000);
        definePrice(tenant, "HST-001", 8000);

        mockMvc.perform(get("/api/v1/prices/HST-001/history")
                        .header(TENANT_HEADER, tenant.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].amountCents", contains(10000, 9000, 8000)))
                .andExpect(jsonPath("$[0].validTo").isNotEmpty())
                .andExpect(jsonPath("$[1].validTo").isNotEmpty())
                .andExpect(jsonPath("$[2].validTo").value(nullValue()));
    }

    @Test
    void returnsAnEmptyHistoryForAKnownSkuThatNeverHadAPrice() throws Exception {
        UUID tenant = UUID.randomUUID();
        registerSku(tenant, "HST-002");

        mockMvc.perform(get("/api/v1/prices/HST-002/history")
                        .header(TENANT_HEADER, tenant.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void answersNotFoundForTheHistoryOfASkuTheTenantDoesNotKnow() throws Exception {
        mockMvc.perform(get("/api/v1/prices/HST-UNKNOWN/history")
                        .header(TENANT_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void doesNotShowTheHistoryOfAnotherTenant() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        registerSku(tenantA, "HST-003");
        registerSku(tenantB, "HST-003");
        definePrice(tenantA, "HST-003", 10000);
        definePrice(tenantA, "HST-003", 9000);

        mockMvc.perform(get("/api/v1/prices/HST-003/history")
                        .header(TENANT_HEADER, tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private void registerSku(UUID tenant, String sku) {
        TenantContext.runWithTenant(tenant,
                () -> skuSyncService.sync(UUID.randomUUID(), Set.of(sku)));
    }

    private void definePrice(UUID tenant, String sku, long amountCents) throws Exception {
        mockMvc.perform(put("/api/v1/prices/" + sku)
                        .header(TENANT_HEADER, tenant.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":" + amountCents + "}"))
                .andExpect(status().isOk());
    }
}