package com.logistica.demo.shared.observability;

public final class TraceContext {

    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String ATTRIBUTE_NAME = TraceContext.class.getName() + ".traceId";
    public static final String MDC_KEY = "traceId";

    private TraceContext() {
    }
}
