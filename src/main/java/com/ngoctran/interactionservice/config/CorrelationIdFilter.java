package com.ngoctran.interactionservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter - Adds correlation ID to all requests for tracing
 * Ensures every request has a unique correlation ID for distributed tracing
 */
@Slf4j
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract or generate correlation ID
            String correlationId = extractOrGenerateCorrelationId(request);

            // Add to MDC for logging
            MDC.put(CORRELATION_ID_KEY, correlationId);

            // Add to response headers
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with request
            filterChain.doFilter(request, response);

        } finally {
            // Clean up MDC
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    /**
     * Extract correlation ID from request headers or generate new one
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        // Try to get from header
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId != null && !correlationId.trim().isEmpty()) {
            // Validate format (should be UUID)
            try {
                UUID.fromString(correlationId);
                return correlationId;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid correlation ID format: {}, generating new one", correlationId);
            }
        }

        // Generate new correlation ID
        String newCorrelationId = UUID.randomUUID().toString();
        log.debug("Generated new correlation ID: {}", newCorrelationId);
        return newCorrelationId;
    }

    /**
     * Get current correlation ID from MDC
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Set correlation ID in MDC (for async operations)
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    /**
     * Clear correlation ID from MDC
     */
    public static void clearCorrelationId() {
        MDC.remove(CORRELATION_ID_KEY);
    }
}
