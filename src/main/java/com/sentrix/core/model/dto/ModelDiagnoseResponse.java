package com.sentrix.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ModelDiagnoseResponse {

    private LocalDateTime timestamp;
    private String featureSchemaVersion;
    private Map<String, Object> rawResponse;
}