package com.sentrix.core.feature.extractor;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FeatureExtractor {

    private static final List<String> MODEL_RAW_FEATURES = List.of(
            "request_rate",
            "latency_p95",
            "latency_p99",
            "error_rate",
            "process_cpu_usage",
            "system_cpu_usage",
            "jvm_memory_used",
            "jvm_memory_max_ratio",
            "hikaricp_active",
            "hikaricp_pending",
            "executor_active_threads"
    );

    private static final List<String> STATISTICS = List.of(
            "mean",
            "std",
            "max",
            "min",
            "slope"
    );

    public Map<String, Double> toModelInputFeatures(Map<String, Double> windowFeatures) {
        Map<String, Double> modelInputFeatures = new LinkedHashMap<>();

        for (String rawFeature : MODEL_RAW_FEATURES) {
            for (String statistic : STATISTICS) {
                String modelFeatureName = rawFeature + "_" + statistic;
                double value = resolveFeatureValue(rawFeature, statistic, windowFeatures);

                modelInputFeatures.put(modelFeatureName, value);
            }
        }

        return modelInputFeatures;
    }

    private double resolveFeatureValue(
            String rawFeature,
            String statistic,
            Map<String, Double> windowFeatures
    ) {
        if (rawFeature.equals("request_rate")) {
            return resolveRequestRate(statistic, windowFeatures);
        }

        if (rawFeature.equals("latency_p95") || rawFeature.equals("latency_p99")) {
            return getWindowFeature(windowFeatures, "latency_avg", statistic);
        }

        if (rawFeature.equals("hikaricp_active")) {
            return getWindowFeature(windowFeatures, "db_connections_active", statistic);
        }

        if (rawFeature.equals("hikaricp_pending")) {
            return 0.0;
        }

        if (rawFeature.equals("executor_active_threads")) {
            return getWindowFeature(windowFeatures, "jvm_threads_live", statistic);
        }

        return getWindowFeature(windowFeatures, rawFeature, statistic);
    }

    private double resolveRequestRate(String statistic, Map<String, Double> windowFeatures) {
        double requestRate = windowFeatures.getOrDefault("request_rate", 0.0);

        return switch (statistic) {
            case "mean", "max", "min" -> requestRate;
            case "std", "slope" -> 0.0;
            default -> 0.0;
        };
    }

    private double getWindowFeature(
            Map<String, Double> windowFeatures,
            String sourceFeature,
            String statistic
    ) {
        return windowFeatures.getOrDefault(sourceFeature + "_" + statistic, 0.0);
    }
}