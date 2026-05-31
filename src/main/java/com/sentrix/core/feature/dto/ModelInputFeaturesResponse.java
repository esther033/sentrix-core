package com.sentrix.core.feature.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ModelInputFeaturesResponse {

    private LocalDateTime timestamp;
    private String schemaVersion;
    private int featureCount;
    private boolean ready;
    private Map<String, Double> features;
}