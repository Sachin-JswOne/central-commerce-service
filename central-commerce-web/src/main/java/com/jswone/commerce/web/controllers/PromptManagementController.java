package com.jswone.commerce.web.controllers;

import com.jswone.commerce.core.enums.ai.PromptType;
import com.jswone.commerce.core.model.ApiResponse;
import com.jswone.commerce.core.model.request.ai.CreatePromptRequest;
import com.jswone.commerce.core.model.response.ai.PromptVersionResponse;
import com.jswone.commerce.core.service.PromptManagementService;
import com.jswone.commerce.core.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai/prompts")
public class PromptManagementController {

    private final PromptManagementService promptManagementService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PromptVersionResponse>> createPrompt(@Valid @RequestBody CreatePromptRequest request) {
        log.debug("Received prompt create request for type {}", request.getPromptType());
        PromptVersionResponse response = promptManagementService.createPrompt(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseUtil.createSuccessResponse(response, HttpStatus.CREATED));
    }

    @GetMapping(value = "/{promptType}/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PromptVersionResponse>> getActivePrompt(@PathVariable String promptType) {
        PromptVersionResponse response = promptManagementService.getActivePrompt(PromptType.fromValue(promptType));
        return ResponseEntity.ok(ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK));
    }

    @GetMapping(value = "/{promptType}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<PromptVersionResponse>>> getPromptVersions(@PathVariable String promptType) {
        List<PromptVersionResponse> response = promptManagementService.getPromptVersions(PromptType.fromValue(promptType));
        return ResponseEntity.ok(ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK));
    }

    @GetMapping(value = "/{promptType}/versions/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PromptVersionResponse>> getPromptVersion(
            @PathVariable String promptType,
            @PathVariable int version
    ) {
        PromptVersionResponse response = promptManagementService.getPromptVersion(PromptType.fromValue(promptType), version);
        return ResponseEntity.ok(ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK));
    }

    @PutMapping(value = "/{promptType}/versions/{version}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PromptVersionResponse>> activatePromptVersion(
            @PathVariable String promptType,
            @PathVariable int version
    ) {
        PromptVersionResponse response = promptManagementService.activatePromptVersion(
                PromptType.fromValue(promptType),
                version
        );
        return ResponseEntity.ok(ApiResponseUtil.createSuccessResponse(response, HttpStatus.OK));
    }
}
