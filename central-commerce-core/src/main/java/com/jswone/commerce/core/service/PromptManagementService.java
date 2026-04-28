package com.jswone.commerce.core.service;

import com.jswone.commerce.core.enums.ai.PromptType;
import com.jswone.commerce.core.model.request.ai.CreatePromptRequest;
import com.jswone.commerce.core.model.response.ai.PromptVersionResponse;

import java.util.List;
import java.util.Optional;

public interface PromptManagementService {

    PromptVersionResponse createPrompt(CreatePromptRequest request);

    PromptVersionResponse getActivePrompt(PromptType promptType);

    Optional<String> findActiveDecodedPrompt(PromptType promptType);

    PromptVersionResponse getPromptVersion(PromptType promptType, int version);

    List<PromptVersionResponse> getPromptVersions(PromptType promptType);

    PromptVersionResponse activatePromptVersion(PromptType promptType, int version);
}
