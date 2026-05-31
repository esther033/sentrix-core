package com.sentrix.core.model.controller;

import com.sentrix.core.feature.dto.ModelInputFeaturesResponse;
import com.sentrix.core.feature.service.FeatureService;
import com.sentrix.core.model.client.ModelServerClient;
import com.sentrix.core.model.dto.ModelDiagnoseRequest;
import com.sentrix.core.model.dto.ModelDiagnoseResponse;
import com.sentrix.core.model.dto.ModelStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class ModelController {

    private final ModelServerClient modelServerClient;
    private final FeatureService featureService;

    public ModelController(
            ModelServerClient modelServerClient,
            FeatureService featureService
    ) {
        this.modelServerClient = modelServerClient;
        this.featureService = featureService;
    }

    @GetMapping("/api/model/status")
    public ModelStatusResponse getModelStatus() {
        return modelServerClient.getModelStatus();
    }

    @PostMapping("/api/model/diagnose")
    public ModelDiagnoseResponse diagnose() {
        ModelInputFeaturesResponse modelInputFeatures = featureService.getModelInputFeatures();

        ModelDiagnoseRequest request = new ModelDiagnoseRequest(
                LocalDateTime.now(),
                modelInputFeatures.getSchemaVersion(),
                modelInputFeatures.getFeatures()
        );

        return modelServerClient.diagnose(request);
    }
}