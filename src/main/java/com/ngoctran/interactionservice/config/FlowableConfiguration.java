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
 * Flowable REST Client Configuration
 * Connects to external Flowable server via REST API
 */
@Configuration
public class FlowableConfiguration {

    @Value("${flowable.bpm.client.base-url:http://localhost:8080/flowable-rest}")
    private String flowableBaseUrl;

    /**
     * RestTemplate for Flowable REST API calls
     */
    @Bean
    public RestTemplate flowableRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Add FormHttpMessageConverter to support multipart/form-data
        List<HttpMessageConverter<?>> converters = new ArrayList<>(restTemplate.getMessageConverters());
        converters.add(new FormHttpMessageConverter());
        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }

    /**
     * Flowable REST API base URL
     */
    @Bean
    public String flowableBaseUrl() {
        return flowableBaseUrl;
    }
}
