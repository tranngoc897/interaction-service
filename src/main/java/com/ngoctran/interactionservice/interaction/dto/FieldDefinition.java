package com.ngoctran.interactionservice.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a form field definition within a step
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {
    
    /**
     * Field name (e.g., "fullName", "dob", "idNumber")
     */
    private String name;
    
    /**
     * Field type: "text", "date", "select", "number", "email", etc.
     */
    private String type;
    
    /**
     * Display label for the field
     */
    private String label;
    
    /**
     * Whether this field is required
     */
    private Boolean required;
    
    /**
     * Placeholder text
     */
    private String placeholder;
    
    /**
     * Options for select/radio fields
     */
    private List<Map<String, Object>> options;
    
    /**
     * Validation rules specific to this field
     */
    private Map<String, Object> validation;
    
    /**
     * Default value
     */
    private Object defaultValue;
}
