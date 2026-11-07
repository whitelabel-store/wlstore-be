package com.wlstore.order_service.exception;

/**
 * Não sabemos o que o inventory fez: a chamada caiu, demorou demais ou voltou com erro do servidor.
 * Ele pode ou não ter reservado, então o pedido NÃO é rejeitado: fica PENDING para ser recuperado.
 */
public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(String message) {
        super(message);
    }

    public InventoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}