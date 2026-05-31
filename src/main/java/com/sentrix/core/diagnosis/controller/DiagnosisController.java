package com.sentrix.core.diagnosis.controller;

import com.sentrix.core.diagnosis.dto.DiagnosisRunResponse;
import com.sentrix.core.diagnosis.service.DiagnosisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @PostMapping("/api/diagnosis/run")
    public DiagnosisRunResponse runDiagnosis() {
        return diagnosisService.runDiagnosis();
    }
}