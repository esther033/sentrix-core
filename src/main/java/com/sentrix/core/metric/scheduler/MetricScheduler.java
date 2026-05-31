package com.sentrix.core.metric.scheduler;

import com.sentrix.core.metric.dto.CurrentMetricsResponse;
import com.sentrix.core.metric.service.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricScheduler {

    private final MetricService metricService;

    @Scheduled(fixedDelayString = "${sentrix.diagnosis.step-size-seconds}000")
    public void collectMetricsPeriodically() {
        try {
            CurrentMetricsResponse response = metricService.collectMetrics();

            log.info(
                    "Scheduled metric collected. timestamp={}, featureCount={}",
                    response.getTimestamp(),
                    response.getFeatures().size()
            );
        } catch (Exception e) {
            log.warn("Failed to collect scheduled metrics: {}", e.getMessage());
        }
    }
}