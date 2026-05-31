package com.sentrix.core.metric.controller;

import com.sentrix.core.metric.dto.CurrentMetricsResponse;
import com.sentrix.core.metric.dto.MetricsBufferStatusResponse;
import com.sentrix.core.metric.service.MetricService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricController {

    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @GetMapping("/api/metrics/current")
    public CurrentMetricsResponse getCurrentMetrics() {
        return metricService.getCurrentMetrics();
    }

    @GetMapping("/api/metrics/buffer/status")
    public MetricsBufferStatusResponse getBufferStatus() {
        return metricService.getBufferStatus();
    }
}