package com.wlstore.catalog_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "wlstore.outbox.relay.enabled=false"
})
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {
}