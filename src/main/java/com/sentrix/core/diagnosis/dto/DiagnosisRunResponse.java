package com.sentrix.core.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DiagnosisRunResponse {

    private LocalDateTime timestamp;
    private boolean ready;
    private String schemaVersion;

    private String detectionStatus;
    private double anomalyScore;
    private double threshold;

    private String faultType;
    private double confidence;

    private String message;
}