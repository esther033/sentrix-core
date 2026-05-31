package com.sentrix.core.metric.collector;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PrometheusMetricCollector {

    private final WebClient.Builder webClientBuilder;

    @Value("${sentrix.demo-server.actuator-url:http://localhost:8080/actuator/prometheus}")
    private String actuatorUrl;

    public Map<String, Double> collectCurrentMetrics() {
        String prometheusText = webClientBuilder.build()
                .get()
                .uri(actuatorUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Map<String, Double> features = new HashMap<>();

        features.put("process_cpu_usage", extractSimpleMetric(prometheusText, "process_cpu_usage"));
        features.put("system_cpu_usage", extractSimpleMetric(prometheusText, "system_cpu_usage"));
        features.put("jvm_gc_overhead", extractSimpleMetric(prometheusText, "jvm_gc_overhead_percent"));
        features.put("jvm_threads_live", extractSimpleMetric(prometheusText, "jvm_threads_live_threads"));
        features.put("db_connections_active", extractSimpleMetric(prometheusText, "jdbc_connections_active"));

        double jvmMemoryUsed = sumMetricByPrefix(prometheusText, "jvm_memory_used_bytes");
        double jvmMemoryMax = sumMetricByPrefix(prometheusText, "jvm_memory_max_bytes");

        features.put("jvm_memory_used", jvmMemoryUsed);
        features.put("jvm_memory_max_ratio", jvmMemoryMax > 0 ? jvmMemoryUsed / jvmMemoryMax : 0.0);

        /*
         * HTTP request metric은 전체 uri를 합산하지 않고 /api/posts만 기준으로 계산한다.
         *
         * 이유:
         * - 전체 합산 시 /actuator/prometheus, /swagger-ui, /v3/api-docs, /chaos/status 등이 섞인다.
         * - sentrix-core가 주기적으로 /actuator/prometheus를 호출하므로 latency가 희석된다.
         * - 현재 DB delay 실험 대상은 /api/posts이므로 해당 uri만 기준으로 latency를 계산한다.
         */
        double requestCount = sumHttpServerRequestsByUri(
                prometheusText,
                "http_server_requests_seconds_count",
                "/api/posts"
        );

        double requestSum = sumHttpServerRequestsByUri(
                prometheusText,
                "http_server_requests_seconds_sum",
                "/api/posts"
        );

        features.put("request_count_total", requestCount);
        features.put("request_duration_sum_total", requestSum);
        features.put("latency_avg", requestCount > 0 ? requestSum / requestCount : 0.0);

        double errorCount = sumHttpServerRequestsByUriAndStatusPrefix(
                prometheusText,
                "http_server_requests_seconds_count",
                "/api/posts",
                "5"
        );

        features.put("error_rate", requestCount > 0 ? errorCount / requestCount : 0.0);

        return features;
    }

    private double extractSimpleMetric(String text, String metricName) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        String[] lines = text.split("\\R");

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            if (line.startsWith(metricName + " ")) {
                return parseLastNumber(line);
            }

            if (line.startsWith(metricName + "{")) {
                return parseLastNumber(line);
            }
        }

        return 0.0;
    }

    private double sumMetricByPrefix(String text, String metricName) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        double sum = 0.0;
        String[] lines = text.split("\\R");

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            if (line.startsWith(metricName + " ") || line.startsWith(metricName + "{")) {
                sum += parseLastNumber(line);
            }
        }

        return sum;
    }

    private double sumHttpServerRequestsByUri(
            String text,
            String metricName,
            String uri
    ) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        double sum = 0.0;
        String[] lines = text.split("\\R");

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            if (!line.startsWith(metricName + "{")) {
                continue;
            }

            if (!line.contains("uri=\"" + uri + "\"")) {
                continue;
            }

            sum += parseLastNumber(line);
        }

        return sum;
    }

    private double sumHttpServerRequestsByUriAndStatusPrefix(
            String text,
            String metricName,
            String uri,
            String statusPrefix
    ) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        double sum = 0.0;
        String[] lines = text.split("\\R");

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            if (!line.startsWith(metricName + "{")) {
                continue;
            }

            if (!line.contains("uri=\"" + uri + "\"")) {
                continue;
            }

            if (!line.contains("status=\"" + statusPrefix)) {
                continue;
            }

            sum += parseLastNumber(line);
        }

        return sum;
    }

    private double parseLastNumber(String line) {
        try {
            String[] tokens = line.trim().split("\\s+");
            return Double.parseDouble(tokens[tokens.length - 1]);
        } catch (Exception e) {
            return 0.0;
        }
    }
}