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

@Slf4j
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = getOrGenerateCorrelationId(request);

        // Add to MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        // Add to response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            // Log request start
            log.info("Request started - Method: {} Path: {} CorrelationId: {}",
                    request.getMethod(), request.getRequestURI(), correlationId);

            // Continue with request processing
            filterChain.doFilter(request, response);

            // Log request completion
            log.info("Request completed - Status: {} CorrelationId: {}",
                    response.getStatus(), correlationId);

        } finally {
            // Clean up MDC
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }

        return correlationId;
    }
}
