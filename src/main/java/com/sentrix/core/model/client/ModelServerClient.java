package com.sentrix.core.model.client;

import com.sentrix.core.model.dto.ModelStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class ModelServerClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${sentrix.model-server.base-url}")
    private String modelServerBaseUrl;

    public ModelStatusResponse getModelStatus() {
        try {
            webClientBuilder.build()
                    .get()
                    .uri(modelServerBaseUrl + "/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return new ModelStatusResponse(
                    true,
                    modelServerBaseUrl,
                    "UP",
                    "Model server is available"
            );
        } catch (Exception e) {
            return new ModelStatusResponse(
                    false,
                    modelServerBaseUrl,
                    "DOWN",
                    "Model server is not available: " + e.getMessage()
            );
        }
    }
}