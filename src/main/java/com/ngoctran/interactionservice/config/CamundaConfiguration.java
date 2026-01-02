package com.ngoctran.interactionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Camunda REST Client Configuration
 * Connects to external Camunda server via REST API
 */
@Configuration
public class CamundaConfiguration {

    @Value("${camunda.bpm.client.base-url:http://localhost:8080/engine-rest}")
    private String camundaBaseUrl;

    /**
     * RestTemplate for Camunda REST API calls
     */
    @Bean
    public RestTemplate camundaRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Add FormHttpMessageConverter to support multipart/form-data
        List<HttpMessageConverter<?>> converters = new ArrayList<>(restTemplate.getMessageConverters());
        converters.add(new FormHttpMessageConverter());
        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }

    /**
     * Camunda REST API base URL
     */
    @Bean
    public String camundaBaseUrl() {
        return camundaBaseUrl;
    }
}
