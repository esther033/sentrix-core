package com.sentrix.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModelStatusResponse {

    private boolean available;
    private String modelServerUrl;
    private String status;
    private String message;
}