package com.wlstore.order_service;

import com.wlstore.order_service.gateway.InventoryGateway;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "eureka.client.enabled=false")
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {

    /**
     * O inventory é outro serviço: nos testes de integração ele é sempre um mock.
     * Quem testa a conversa HTTP de verdade é o teste do próprio cliente.
     */
    @MockitoBean
    protected InventoryGateway inventoryGateway;
}