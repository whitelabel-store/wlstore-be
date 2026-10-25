package com.wlstore.pricing_service.controller;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorHandlingTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    MockMvc mockMvc;

    @Test
    void anUnknownRouteAnswersNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist")
                        .header(TENANT_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void aMissingRequiredQueryParameterAnswersBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .header(TENANT_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void anUnsupportedHttpMethodAnswersMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/v1/prices/ANY-SKU")
                        .header(TENANT_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Method Not Allowed"));
    }
}