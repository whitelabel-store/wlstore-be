package com.wlstore.inventory_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory/Stock Service API")
                        .description("Manages stock balance per SKU: on hand, reserved and available.")
                        .version("v1"));
    }

    @Bean
    public GlobalOperationCustomizer tenantHeaderCustomizer() {
        return (operation, handlerMethod) -> operation.addParametersItem(
                new Parameter()
                        .in("header")
                        .name("X-Tenant-Id")
                        .description("Tenant that scopes this request. Required; must be a valid UUID.")
                        .required(true)
                        .schema(new StringSchema().format("uuid"))
                        .example("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        );
    }
}
