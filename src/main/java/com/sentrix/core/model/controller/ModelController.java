package com.sentrix.core.model.controller;

import com.sentrix.core.model.client.ModelServerClient;
import com.sentrix.core.model.dto.ModelStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelController {

    private final ModelServerClient modelServerClient;

    public ModelController(ModelServerClient modelServerClient) {
        this.modelServerClient = modelServerClient;
    }

    @GetMapping("/api/model/status")
    public ModelStatusResponse getModelStatus() {
        return modelServerClient.getModelStatus();
    }
}