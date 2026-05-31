package com.sentrix.core.feature.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class WindowFeaturesResponse {

    private LocalDateTime timestamp;
    private int windowSize;
    private boolean ready;
    private Map<String, Double> features;
}