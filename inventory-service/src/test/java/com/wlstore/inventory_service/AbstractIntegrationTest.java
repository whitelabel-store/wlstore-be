package com.wlstore.inventory_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "eureka.client.enabled=false")
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {
}