package com.ngoctran.interactionservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class FlowDefinitionLoader {
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> load(Resource yamlResource) throws IOException {
        return mapper.readValue(yamlResource.getInputStream(), Map.class);
    }
}