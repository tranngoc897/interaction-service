package com.ngoctran.interactionservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Camunda REST Client Configuration
 * Connects to external Camunda server via REST API
 */
@Configuration
public class CamundaConfiguration {

    /**
     * RestTemplate for Camunda REST API calls
     */
    @Bean
    public RestTemplate camundaRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Camunda REST API base URL
     */
    @Bean
    public String camundaBaseUrl() {
        return "http://localhost:8080/engine-rest";
    }
}
