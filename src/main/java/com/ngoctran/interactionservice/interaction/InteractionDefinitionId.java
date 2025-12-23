package com.ngoctran.interactionservice.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionDefinitionId implements Serializable {
    private String interactionDefinitionKey;
    private Long interactionDefinitionVersion;
}
