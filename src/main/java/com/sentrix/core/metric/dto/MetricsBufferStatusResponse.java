package com.sentrix.core.metric.dto;

import java.time.LocalDateTime;

public record MetricsBufferStatusResponse(
        int windowSizeSeconds,
        int stepSizeSeconds,
        int currentSize,
        boolean ready,
        LocalDateTime oldestTimestamp,
        LocalDateTime latestTimestamp
) {
}