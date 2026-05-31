package com.sentrix.core.feature.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FeatureDefinition {

    private String name;
    private String sourceMetric;
    private String statistic;
    private String description;
}