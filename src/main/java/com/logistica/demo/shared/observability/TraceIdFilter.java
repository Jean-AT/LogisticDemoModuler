package com.logistica.demo.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final int MAX_TRACE_ID_LENGTH = 128;
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]+");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TraceContext.HEADER_NAME));
        request.setAttribute(TraceContext.ATTRIBUTE_NAME, traceId);
        response.setHeader(TraceContext.HEADER_NAME, traceId);
        MDC.put(TraceContext.MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.MDC_KEY);
        }
    }

    private String resolveTraceId(String candidate) {
        if (candidate == null) {
            return UUID.randomUUID().toString();
        }
        String normalized = candidate.trim();
        if (normalized.isEmpty()
                || normalized.length() > MAX_TRACE_ID_LENGTH
                || !VALID_TRACE_ID.matcher(normalized).matches()) {
            return UUID.randomUUID().toString();
        }
        return normalized;
    }
}
