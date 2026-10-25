package com.wlstore.pricing_service.exception;

import com.wlstore.common.exception.ResourceNotFoundException;

public class UnknownSkuException extends ResourceNotFoundException {

    public UnknownSkuException(String sku) {
        super("SKU", "sku", sku);
    }
}