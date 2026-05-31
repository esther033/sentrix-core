package com.sentrix.core.diagnosis.service;

import com.sentrix.core.diagnosis.dto.DiagnosisRunResponse;
import com.sentrix.core.feature.dto.ModelInputFeaturesResponse;
import com.sentrix.core.feature.service.FeatureService;
import com.sentrix.core.model.client.ModelServerClient;
import com.sentrix.core.model.dto.ModelDiagnoseRequest;
import com.sentrix.core.model.dto.ModelDiagnoseResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DiagnosisService {

    private final FeatureService featureService;
    private final ModelServerClient modelServerClient;

    public DiagnosisService(
            FeatureService featureService,
            ModelServerClient modelServerClient
    ) {
        this.featureService = featureService;
        this.modelServerClient = modelServerClient;
    }

    public DiagnosisRunResponse runDiagnosis() {
        ModelInputFeaturesResponse modelInputFeatures = featureService.getModelInputFeatures();

        ModelDiagnoseRequest request = new ModelDiagnoseRequest(
                LocalDateTime.now(),
                modelInputFeatures.getSchemaVersion(),
                modelInputFeatures.getFeatures()
        );

        ModelDiagnoseResponse modelResponse = modelServerClient.diagnose(request);

        Map<String, Object> rawResponse = modelResponse.getRawResponse();

        Map<String, Object> detection = getObjectMap(rawResponse, "detection");
        Map<String, Object> classification = getObjectMap(rawResponse, "classification");

        String detectionStatus = getString(detection, "status", "UNKNOWN");
        double anomalyScore = getDouble(detection, "anomalyScore", 0.0);
        double threshold = getDouble(detection, "threshold", 0.0);

        String faultType = getString(classification, "faultType", "UNKNOWN");
        double confidence = getDouble(classification, "confidence", 0.0);

        return new DiagnosisRunResponse(
                LocalDateTime.now(),
                modelInputFeatures.isReady(),
                modelInputFeatures.getSchemaVersion(),
                detectionStatus,
                anomalyScore,
                threshold,
                faultType,
                confidence,
                "Diagnosis completed"
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getObjectMap(
            Map<String, Object> source,
            String key
    ) {
        Object value = source.get(key);

        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }

        return Map.of();
    }

    private String getString(
            Map<String, Object> source,
            String key,
            String defaultValue
    ) {
        Object value = source.get(key);

        if (value == null) {
            return defaultValue;
        }

        return value.toString();
    }

    private double getDouble(
            Map<String, Object> source,
            String key,
            double defaultValue
    ) {
        Object value = source.get(key);

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return defaultValue;
    }
}