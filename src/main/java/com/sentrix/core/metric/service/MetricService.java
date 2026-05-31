package com.sentrix.core.metric.service;

import com.sentrix.core.metric.buffer.SlidingWindowBuffer;
import com.sentrix.core.metric.collector.PrometheusMetricCollector;
import com.sentrix.core.metric.dto.CurrentMetricsResponse;
import com.sentrix.core.metric.dto.MetricsBufferStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class MetricService {

    private final PrometheusMetricCollector prometheusMetricCollector;
    private final SlidingWindowBuffer slidingWindowBuffer;

    private final int windowSizeSeconds;
    private final int stepSizeSeconds;

    public MetricService(
            PrometheusMetricCollector prometheusMetricCollector,
            SlidingWindowBuffer slidingWindowBuffer,
            @Value("${sentrix.diagnosis.window-size-seconds}") int windowSizeSeconds,
            @Value("${sentrix.diagnosis.step-size-seconds}") int stepSizeSeconds
    ) {
        this.prometheusMetricCollector = prometheusMetricCollector;
        this.slidingWindowBuffer = slidingWindowBuffer;
        this.windowSizeSeconds = windowSizeSeconds;
        this.stepSizeSeconds = stepSizeSeconds;
    }

    public CurrentMetricsResponse getCurrentMetrics() {
        Map<String, Double> features = prometheusMetricCollector.collectCurrentMetrics();

        CurrentMetricsResponse currentMetrics = new CurrentMetricsResponse(
                LocalDateTime.now(),
                features
        );

        slidingWindowBuffer.add(currentMetrics);

        return currentMetrics;
    }

    public MetricsBufferStatusResponse getBufferStatus() {
        return new MetricsBufferStatusResponse(
                windowSizeSeconds,
                stepSizeSeconds,
                slidingWindowBuffer.size(),
                slidingWindowBuffer.isReady(),
                slidingWindowBuffer.getOldestTimestamp(),
                slidingWindowBuffer.getLatestTimestamp()
        );
    }
}