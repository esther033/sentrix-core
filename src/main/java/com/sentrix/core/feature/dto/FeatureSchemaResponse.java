package com.sentrix.core.feature.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FeatureSchemaResponse {

    private String schemaVersion;
    private int featureCount;
    private List<FeatureDefinition> features;
}