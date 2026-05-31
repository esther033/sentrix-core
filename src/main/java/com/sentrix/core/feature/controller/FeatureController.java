package com.sentrix.core.feature.controller;

import com.sentrix.core.feature.dto.WindowFeaturesResponse;
import com.sentrix.core.feature.service.FeatureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureController {

    private final FeatureService featureService;

    public FeatureController(FeatureService featureService) {
        this.featureService = featureService;
    }

    @GetMapping("/api/features/window")
    public WindowFeaturesResponse getWindowFeatures() {
        return featureService.calculateWindowFeatures();
    }
}