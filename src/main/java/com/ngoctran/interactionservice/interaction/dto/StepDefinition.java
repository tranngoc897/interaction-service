package com.ngoctran.interactionservice.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor; // Keeping imports if needed for other reasons, but arguably we are replacing lombok behavior.
import lombok.Data; 

import java.util.List;
import java.util.Map;

/**
 * Represents a single step definition from flw_int_def.steps (BLUEPRINT)
 * This is the template/configuration for a step in the interaction journey
 */
@NoArgsConstructor
@AllArgsConstructor
public class StepDefinition {
    
    private String name;
    private String type;
    private String title;
    private String description;
    private String next;
    private List<FieldDefinition> fields;
    private Map<String, Object> validation;
    private List<Map<String, Object>> onSubmit;
    private Map<String, Object> uiSchema;
    private Boolean resumable;
    private String estimatedTime;
    private Map<String, Object> metadata;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }

    public List<FieldDefinition> getFields() { return fields; }
    public void setFields(List<FieldDefinition> fields) { this.fields = fields; }

    public Map<String, Object> getValidation() { return validation; }
    public void setValidation(Map<String, Object> validation) { this.validation = validation; }

    public List<Map<String, Object>> getOnSubmit() { return onSubmit; }
    public void setOnSubmit(List<Map<String, Object>> onSubmit) { this.onSubmit = onSubmit; }

    public Map<String, Object> getUiSchema() { return uiSchema; }
    public void setUiSchema(Map<String, Object> uiSchema) { this.uiSchema = uiSchema; }

    public Boolean getResumable() { return resumable; }
    public void setResumable(Boolean resumable) { this.resumable = resumable; }

    public String getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(String estimatedTime) { this.estimatedTime = estimatedTime; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
