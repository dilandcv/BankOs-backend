package com.bankos.common.web;

import java.util.UUID;

public final class CorrelationIdContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIdContext() {
    }

    public static void set(String correlationId) {
        CURRENT.set(correlationId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static String getOrCreate() {
        String current = CURRENT.get();
        if (current == null || current.isBlank()) {
            current = UUID.randomUUID().toString();
            CURRENT.set(current);
        }
        return current;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
