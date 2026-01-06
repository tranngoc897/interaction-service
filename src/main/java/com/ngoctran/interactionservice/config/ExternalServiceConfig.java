package com.ngoctran.interactionservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for external service integrations
 * Provides HTTP clients for calling external APIs (OTP, eKYC, AML, Core Banking)
 */
@Slf4j
@Configuration
public class ExternalServiceConfig {

    @Value("${external.http.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${external.http.read-timeout:30000}")
    private int readTimeout;

    /**
     * RestTemplate for synchronous HTTP calls
     * Used for OTP service, Core Banking API
     */
    @Bean
    public RestTemplate externalRestTemplate() {
        log.info("Configuring external RestTemplate with timeouts: connect={}ms, read={}ms",
                connectTimeout, readTimeout);

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate(factory);

        // Add interceptors for logging, correlation ID, etc.
        restTemplate.getInterceptors().add((request, body, execution) -> {
            // Add correlation ID to outgoing requests
            String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
            if (correlationId != null) {
                request.getHeaders().add("X-Correlation-ID", correlationId);
            }

            log.debug("Making external HTTP call: {} {}", request.getMethod(), request.getURI());
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    /**
     * WebClient for reactive HTTP calls
     * Used for high-throughput external services
     */
    @Bean
    public WebClient externalWebClient() {
        log.info("Configuring external WebClient");

        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .filter((request, next) -> {
                    // Add correlation ID to outgoing requests
                    String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
                    if (correlationId != null) {
                        return next.exchange(request.mutate()
                                .header("X-Correlation-ID", correlationId)
                                .build());
                    }
                    return next.exchange(request);
                })
                .build();
    }
}
