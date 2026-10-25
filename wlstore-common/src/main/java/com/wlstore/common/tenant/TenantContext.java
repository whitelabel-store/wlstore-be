package com.wlstore.common.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static <T> T callWithTenant(UUID tenantId, Supplier<T> action) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");

        UUID previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void runWithTenant(UUID tenantId, Runnable action) {
        callWithTenant(tenantId, () -> {
            action.run();
            return null;
        });
    }
}