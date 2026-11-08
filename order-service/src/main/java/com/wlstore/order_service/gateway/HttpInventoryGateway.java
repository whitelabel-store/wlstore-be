package com.wlstore.order_service.gateway;

import com.wlstore.order_service.dto.OrderLineDTO;
import com.wlstore.order_service.exception.InventoryUnavailableException;
import com.wlstore.order_service.exception.ReservationRejectedException;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

public class HttpInventoryGateway implements InventoryGateway{
    private final RestClient restClient;

    public HttpInventoryGateway(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void reserve(UUID orderId, List<OrderLineDTO> lines) {
        try {
            restClient.post()
                    .uri("/api/v1/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReservationRequest(orderId, lines))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.NotFound refusal) {
            throw new ReservationRejectedException(
                    "The inventory could not reserve the stock for order " + orderId);
        } catch (HttpServerErrorException serverError) {
            throw new InventoryUnavailableException(
                    "The inventory failed to answer the reservation for order " + orderId, serverError);
        } catch (ResourceAccessException networkFailure) {
            throw new InventoryUnavailableException(
                    "The inventory could not be reached for order " + orderId, networkFailure);
        }
    }

    private record ReservationRequest(UUID orderId, List<OrderLineDTO> lines) {

    }
}
