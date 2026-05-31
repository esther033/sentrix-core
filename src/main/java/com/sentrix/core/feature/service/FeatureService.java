package com.sentrix.core.feature.service;

import com.sentrix.core.feature.dto.FeatureDefinition;
import com.sentrix.core.feature.dto.FeatureSchemaResponse;
import com.sentrix.core.feature.dto.ModelInputFeaturesResponse;
import com.sentrix.core.feature.dto.WindowFeaturesResponse;
import com.sentrix.core.feature.extractor.FeatureExtractor;
import com.sentrix.core.metric.buffer.SlidingWindowBuffer;
import com.sentrix.core.metric.dto.CurrentMetricsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeatureService {

    private final SlidingWindowBuffer slidingWindowBuffer;
    private final FeatureExtractor featureExtractor;

    @Value("${sentrix.diagnosis.schema-version}")
    private String schemaVersion;

    public FeatureService(
            SlidingWindowBuffer slidingWindowBuffer,
            FeatureExtractor featureExtractor
    ) {
        this.slidingWindowBuffer = slidingWindowBuffer;
        this.featureExtractor = featureExtractor;
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

        List<String> featureNames = new ArrayList<>(window.get(0).getFeatures().keySet());

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

    public FeatureSchemaResponse getFeatureSchema() {
        List<FeatureDefinition> features = new ArrayList<>();

        Map<String, String> sourceMetricDescriptions = new LinkedHashMap<>();
        sourceMetricDescriptions.put("system_cpu_usage", "시스템 전체 CPU 사용률");
        sourceMetricDescriptions.put("process_cpu_usage", "demo-server 프로세스 CPU 사용률");
        sourceMetricDescriptions.put("jvm_memory_used", "JVM 사용 메모리");
        sourceMetricDescriptions.put("jvm_memory_max_ratio", "JVM 최대 메모리 대비 사용 메모리 비율");
        sourceMetricDescriptions.put("jvm_gc_overhead", "JVM GC overhead");
        sourceMetricDescriptions.put("jvm_threads_live", "JVM live thread 수");
        sourceMetricDescriptions.put("db_connections_active", "활성 DB connection 수");
        sourceMetricDescriptions.put("request_count_total", "HTTP 요청 누적 수");
        sourceMetricDescriptions.put("latency_avg", "평균 HTTP 응답 시간");
        sourceMetricDescriptions.put("error_rate", "전체 요청 중 5xx 에러 비율");

        List<String> statistics = List.of("mean", "max", "min", "std", "slope");

        for (Map.Entry<String, String> entry : sourceMetricDescriptions.entrySet()) {
            String sourceMetric = entry.getKey();
            String sourceDescription = entry.getValue();

            for (String statistic : statistics) {
                String featureName = sourceMetric + "_" + statistic;

                features.add(new FeatureDefinition(
                        featureName,
                        sourceMetric,
                        statistic,
                        sourceDescription + "의 window " + statistic + " 값"
                ));
            }
        }

        features.add(new FeatureDefinition(
                "request_rate",
                "request_count_total",
                "rate",
                "request_count_total 증가량을 시간 차이로 나눈 초당 요청 수"
        ));

        return new FeatureSchemaResponse(
                schemaVersion,
                features.size(),
                features
        );
    }

    public ModelInputFeaturesResponse getModelInputFeatures() {
        WindowFeaturesResponse windowFeaturesResponse = calculateWindowFeatures();

        Map<String, Double> modelInputFeatures = featureExtractor.toModelInputFeatures(
                windowFeaturesResponse.getFeatures()
        );

        return new ModelInputFeaturesResponse(
                LocalDateTime.now(),
                schemaVersion,
                modelInputFeatures.size(),
                windowFeaturesResponse.isReady(),
                modelInputFeatures
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