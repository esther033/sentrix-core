package com.sentrix.core.feature.service;

import com.sentrix.core.feature.dto.WindowFeaturesResponse;
import com.sentrix.core.metric.buffer.SlidingWindowBuffer;
import com.sentrix.core.metric.dto.CurrentMetricsResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FeatureService {

    private final SlidingWindowBuffer slidingWindowBuffer;

    public FeatureService(SlidingWindowBuffer slidingWindowBuffer) {
        this.slidingWindowBuffer = slidingWindowBuffer;
    }

    public WindowFeaturesResponse calculateWindowFeatures() {
        List<CurrentMetricsResponse> window = slidingWindowBuffer.getSnapshot();

        Map<String, Double> windowFeatures = new LinkedHashMap<>();

        if (window.isEmpty()) {
            return new WindowFeaturesResponse(
                    LocalDateTime.now(),
                    0,
                    false,
                    windowFeatures
            );
        }

        Set<String> featureNames = window.get(0).getFeatures().keySet();

        for (String featureName : featureNames) {
            List<Double> values = window.stream()
                    .map(row -> row.getFeatures().getOrDefault(featureName, 0.0))
                    .toList();

            windowFeatures.put(featureName + "_mean", mean(values));
            windowFeatures.put(featureName + "_max", max(values));
            windowFeatures.put(featureName + "_min", min(values));
            windowFeatures.put(featureName + "_std", std(values));
            windowFeatures.put(featureName + "_slope", slope(values));
        }

        windowFeatures.put("request_rate", calculateRequestRate(window));

        return new WindowFeaturesResponse(
                LocalDateTime.now(),
                window.size(),
                slidingWindowBuffer.isReady(),
                windowFeatures
        );
    }

    private double mean(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double max(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    private double min(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
    }

    private double std(List<Double> values) {
        if (values.size() <= 1) {
            return 0.0;
        }

        double mean = mean(values);

        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    private double slope(List<Double> values) {
        if (values.size() <= 1) {
            return 0.0;
        }

        double first = values.get(0);
        double last = values.get(values.size() - 1);

        return (last - first) / (values.size() - 1);
    }

    private double calculateRequestRate(List<CurrentMetricsResponse> window) {
        if (window.size() <= 1) {
            return 0.0;
        }

        CurrentMetricsResponse first = window.get(0);
        CurrentMetricsResponse last = window.get(window.size() - 1);

        double firstCount = first.getFeatures().getOrDefault("request_count_total", 0.0);
        double lastCount = last.getFeatures().getOrDefault("request_count_total", 0.0);

        long seconds = Duration.between(first.getTimestamp(), last.getTimestamp()).getSeconds();

        if (seconds <= 0) {
            return 0.0;
        }

        double diff = lastCount - firstCount;

        return Math.max(diff, 0.0) / seconds;
    }
}